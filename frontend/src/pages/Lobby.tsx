/*  src/pages/Lobby.tsx  */
import React, { useEffect, useRef, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import FallingBricks from "../components/FallingBricks";
import "../style/Lobby.css";

const API = import.meta.env.VITE_API_BASE ?? "http://localhost:8080";

export interface Player {
  playerId: string;
  name: string;
}

export interface LobbyInitialPayload {
  playerID: string;
  name: string;
  roomId: string;
  roomCode: string;
  players: Player[];
  host: string;
}

const buildSSEURL = (isHost: boolean, room: string, name: string): string =>
  isHost
    ? `${API}/createRoom/${encodeURIComponent(name)}`
    : `${API}/joinRoom/${room}/${encodeURIComponent(name)}`;

const Lobby: React.FC = () => {
  const { roomCode = "" } = useParams();
  const navigate = useNavigate();

  const myName = localStorage.getItem("name") ?? "";
  const myPID = sessionStorage.getItem("blockopolyPID") ?? "";
  const isHostSession = sessionStorage.getItem("blockopolyIsHost") === "true";

  const [players, setPlayers] = useState<Player[]>([]);
  const [hostID, setHostID] = useState<string | null>(null);
  const esRef = useRef<EventSource | null>(null);

  const upsert = (list: Player[], p: Player): Player[] => {
    const i = list.findIndex((x) => x.playerId === p.playerId);
    return i === -1
      ? [...list, p]
      : [...list.slice(0, i), p, ...list.slice(i + 1)];
  };

  useEffect(() => {
    if (!myName || !roomCode) return;

    const url = buildSSEURL(isHostSession, roomCode, myName);
    const es = new EventSource(url, { withCredentials: true });
    esRef.current = es;

    es.addEventListener("INITIAL", (ev) => {
      try {
        const {
          players: list,
          host,
          playerID,
        }: LobbyInitialPayload = JSON.parse(ev.data);
        setPlayers(list);
        setHostID(host);
        if (playerID) sessionStorage.setItem("blockopolyPID", playerID);
      } catch (e) {
        console.error("[INITIAL parse error]", e);
      }
    });

    es.addEventListener("JOIN", (ev) => {
      try {
        const [pid, ...nameParts] = ev.data.split(":");
        if (!pid) return;
        const name = nameParts.join(":") || "Player";
        setPlayers((cur) => upsert(cur, { playerId: pid, name }));
      } catch (e) {
        console.error("[JOIN parse error]", e);
      }
    });

    es.addEventListener("LEAVE", (ev) => {
      const [pid] = ev.data.split(":");
      if (!pid) return;
      setPlayers((cur) => cur.filter((p) => p.playerId !== pid));
    });

    es.addEventListener("HOST", (ev) => setHostID(ev.data.trim()));
    es.onerror = () => console.error("[Lobby] SSE error");

    return () => es.close();
  }, [isHostSession, roomCode, myName]);

  useEffect(() => {
    return () => {
      const pid = sessionStorage.getItem("blockopolyPID");
      if (!pid) return;
      fetch(`${API}/leaveRoom/${pid}`, { method: "POST", keepalive: true });
    };
  }, []);

  const leaveRoom = () => navigate("/");
  const startGame = () => navigate(`/game/${roomCode}`);
  const hostName =
    players.find((p) => p.playerId === hostID)?.name ?? "The Host";
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
