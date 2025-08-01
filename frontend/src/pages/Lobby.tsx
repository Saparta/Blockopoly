/* src/pages/Lobby.tsx */
import React, { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import FallingBricks from "../components/FallingBricks";
import "../style/Lobby.css";
import type { Player } from "./Mainmenu.tsx";
import {
  NAME_KEY,
  PLAYER_ID_KEY,
  PLAYERS_KEY,
  ROOM_ID_KEY,
} from "../constants/constants.ts";

const API = import.meta.env.room_service ?? "http://localhost:8080";

const buildSSEURL = (
  room: string,
  name: string,
  playerId: string | null = null
): string => {
  return `${API}/joinRoom/${room}/${encodeURIComponent(name)}${
    playerId !== null ? `?playerId=${playerId}` : ""
  }`;
};

const Lobby: React.FC = () => {
  const { roomCode = "" } = useParams();
  const navigate = useNavigate();

  const myName = sessionStorage.getItem(NAME_KEY) || "";
  const [myPID] = useState<string>(sessionStorage.getItem(PLAYER_ID_KEY) || "");

  const roomId = sessionStorage.getItem(ROOM_ID_KEY) ?? "";

  console.log(
    `Players List: ${JSON.stringify(sessionStorage.getItem(PLAYERS_KEY))}`
  );
  const [players, setPlayers] = useState<Player[]>(
    JSON.parse(sessionStorage.getItem(PLAYERS_KEY)!)
  );
  const [hostID, setHostID] = useState<string | null>(null);
  const esRef = useRef<EventSource | null>(null);

  const upsert = useCallback((list: Player[], p: Player): Player[] => {
    const i = list.findIndex((x) => x.playerId === p.playerId);
    if (i === -1) {
      return [...list, p];
    } else {
      return [...list.slice(0, i), p, ...list.slice(i + 1)];
    }
  }, []);

  useEffect(() => {
    sessionStorage.setItem(PLAYERS_KEY, JSON.stringify(players));
  }, [players]);

  useEffect(() => {
    if (!myName || !roomCode) {
      console.warn(
        "Lobby: Missing myName or roomCode. Cannot establish SSE connection. Redirecting..."
      );
      navigate("/");
      return;
    }

    const url = buildSSEURL(roomCode, myName, myPID);
    console.log(`Lobby: Attempting to establish new SSE connection to: ${url}`);

    esRef.current = new EventSource(url);

    const handleJoin = (ev: MessageEvent) => {
      try {
        const [pid, ...nameParts] = ev.data.split(":");
        if (!pid) {
          console.warn("[SSE JOIN] Received event with no player ID.");
          return;
        }
        const name = nameParts.join(":") || "Player";
        console.log(`[SSE JOIN] Player joined: ${name} (${pid})`);
        setPlayers((cur) => upsert(cur, { playerId: pid, name }));
      } catch (e) {
        console.error("[SSE JOIN parse error]", e);
      }
    };

    const handleLeave = (ev: MessageEvent) => {
      const [pid] = ev.data.split(":");
      if (!pid) {
        console.warn("[SSE LEAVE] Received event with no player ID.");
        return;
      }
      console.log(`[SSE LEAVE] Player left: (${pid})`);
      setPlayers((cur) => cur.filter((p) => p.playerId !== pid));
    };

    const handleHost = (ev: MessageEvent) => {
      const [newHostID] = ev.data.split(":");
      const trimmedHostID = newHostID ? newHostID.trim() : "";

      if (trimmedHostID) {
        console.log(`[SSE HOST] New host ID: ${trimmedHostID}`);
        setHostID(trimmedHostID);
      } else {
        console.warn(
          "[SSE HOST] Received empty or malformed host ID from SSE event. Data:",
          ev.data
        );
      }
    };

    const handleGameStart = () => {
      console.log("[SSE START] Game is starting! Navigating...");
      if (!roomCode) {
        console.error("[SSE START] No room code available for navigation.");
        return;
      }
      navigate(`/play/${roomCode}`);
    };
    // ------------------------------------

    esRef.current.addEventListener("JOIN", handleJoin);
    esRef.current.addEventListener("LEAVE", handleLeave);
    esRef.current.addEventListener("HOST", handleHost);
    esRef.current.addEventListener("START", handleGameStart);
    // --------------------------------------

    esRef.current.onerror = (error) => {
      console.error("[Lobby] SSE error:", error);
      if (esRef.current) {
        esRef.current.close();
        esRef.current = null;
      }
    };

    return () => {
      console.log(
        "Lobby: Cleaning up SSE connection on unmount or effect re-run."
      );
      if (esRef.current) {
        esRef.current.close();
        esRef.current = null;
      }
    };
  }, [roomCode, myName, navigate, upsert, myPID]);

  const leaveRoom = () => {
    fetch(`${API}/leaveRoom/${myPID}`, { method: "POST" }).then((r) => {
      if (r.ok) {
        navigate("/");
      }
    });
  };

  const startGame = async () => {
    try {
      const res = await fetch(`${API}/start/${roomId}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ hostId: myPID }),
      });

      if (!res.ok) {
        throw new Error(await res.text());
      }

      navigate(`/play/${roomCode}`);
    } catch (err) {
      console.error("[startGame] Couldn’t start:", err);
      alert("Could not start the game – check the server log.");
    }
  };

  const hostName =
    Array.isArray(players) && typeof hostID === "string"
      ? players.find((p) => p.playerId === hostID)?.name ?? "The Host"
      : "The Host";

  const iAmHost = myPID && myPID === hostID;

  return (
    <div className="lobby-wrapper">
      <div className="falling-bricks-wrapper">
        <FallingBricks />
      </div>

      <div className="lobby">
        <h2>{hostName}&apos;s Room</h2>
        <p>
          Join&nbsp;Code:&nbsp;<b className="room-code-display">{roomCode}</b>
        </p>

        <h3>{players.length}/5 players</h3>
        <ol className="player-list">
          {players.map((p) => (
            <li key={p.playerId} className="player-slot">
              {p.playerId === hostID && "👑 "}
              {p.name}
              {p.playerId === myPID && " (You)"}
            </li>
          ))}
          {Array.from({ length: Math.max(0, 5 - players.length) }).map(
            (_, i) => (
              <li key={`empty-${i}`} className="player-slot empty" />
            )
          )}
        </ol>

        <div className="button-row">
          <button className="leave-button" onClick={leaveRoom}>
            Leave
          </button>
          {iAmHost && (
            <button
              className="start-button"
              onClick={startGame}
              disabled={players.length < 2}
            >
              Start&nbsp;Game
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

export default Lobby;
