import asyncio
import json
import aiohttp
import httpx
from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
import time

room_info = {}
host_player_id = ""
players = []
browser_opened = False
last_reload_time = 0
driver = None

def build_url(room_id):
    ts = int(time.time())
    return f"http://localhost:8081/game/{room_id}?ts={ts}"

def open_browser_tab(room_id):
    global driver
    url = build_url(room_id)
    options = Options()
    options.add_experimental_option("detach", True)  # Keep browser open
    driver = webdriver.Chrome(service=Service(), options=options)
    driver.get(url)

def reload_browser_tab(room_id):
    global last_reload_time, driver
    now = time.time()
    if now - last_reload_time >= 1:
        last_reload_time = now
        url = build_url(room_id)
        if driver:
            driver.get(url)  # This reloads the current tab
        else:
            open_browser_tab(room_id)


async def create_room(player_name):
    url = f"http://localhost:8080/createRoom/{player_name}"
    async with aiohttp.ClientSession() as session:
        async with session.get(url) as resp:
            async for line in resp.content:
                if line.startswith(b"data:"):
                    raw = line.decode().lstrip("data:").strip()

                    try:
                        event = json.loads(raw)
                    except json.JSONDecodeError:
                        print(f"[WARN] Skipping non-JSON line: {raw}")
                        continue

                    if "roomId" in event and "players" in event:
                        room_info['roomId'] = event['roomId']
                        room_info['roomCode'] = event['roomCode']
                        players.clear()
                        for p in event['players']:
                            if isinstance(p, dict) and 'playerId' in p:
                                players.append(p['playerId'])
                        print(f"[INFO] Room created: {room_info}")
                        global host_player_id
                        host_player_id = players[0]
                        return

async def listen_for_joins(player_name, player_id):
    url = f"http://localhost:8080/createRoom/{player_name}?playerId={player_id}"
    async with aiohttp.ClientSession() as session:
        async with session.get(url) as resp:
            async for line in resp.content:
                if line.startswith(b"data:"):
                    raw = line.decode().lstrip("data:").strip()
                    if ":" in raw:
                        pid, name = raw.split(":", 1)
                        print(f"[INFO] New player joined: {name} ({pid})")
                        if pid not in players:
                            players.append(pid)



async def join_room(player_name):
    url = f"http://localhost:8080/joinRoom/{room_info['roomCode']}/{player_name}"
    async with aiohttp.ClientSession() as session:
        await session.get(url)

async def start_room():
    url = f"http://localhost:8080/start/{room_info['roomId']}"
    async with httpx.AsyncClient() as client:
        await client.post(url)

async def connect_websocket(player_id, is_refresh_listener=False):
    url = f"ws://localhost:8081/ws/play/{room_info['roomId']}/{player_id}"
    print(f"Attempting WebSocket connection to: {url}")
    async with aiohttp.ClientSession() as session:
        async with session.ws_connect(url) as ws:
            print(f"[{player_id}] WebSocket connected ✅")
            global browser_opened
            if not browser_opened:
                open_browser_tab(room_info['roomId'])
                browser_opened = True

            async for msg in ws:
                print(f"[{player_id}] Received: {msg.data}")
                try:
                    data = json.loads(msg.data)
                    if is_refresh_listener and data.get("type") == "STATE":
                        reload_browser_tab(room_info['roomId'])
                except json.JSONDecodeError:
                    print(f"[{player_id}] Non-JSON message: {msg.data}")



async def main():
    await create_room("JDot")
    join_listener = asyncio.create_task(listen_for_joins("JDot", host_player_id))
    await join_room("SMoney")
    await asyncio.sleep(2)  # let JOIN messages come in
    await start_room()
    join_listener.cancel()
    await asyncio.gather(*[
        connect_websocket(pid, is_refresh_listener=(i == 0))
        for i, pid in enumerate(players)
    ])


asyncio.run(main())
