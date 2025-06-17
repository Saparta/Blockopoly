/*  src/pages/Lobby.tsx  */
import React, { useEffect, useRef, useState } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
import FallingBricks from "../components/FallingBricks";
import "../style/Lobby.css";

const API = import.meta.env.VITE_API_BASE ?? "http://localhost:8080";

/* single-player record from backend */
interface Player {
  playerId: string;
  name: string;
}

const Lobby: React.FC = () => {
  /* ───────── routing / identity ───────── */
  const { roomCode = "" } = useParams(); // /lobby/:roomCode
  const navigate = useNavigate();
  const { state } = useLocation() as { state?: { name: string } };

  const myName = state?.name ?? localStorage.getItem("name") ?? "";
  const myPID = sessionStorage.getItem("blockopolyPID") ?? "";

  /* ───────── local state ───────── */
  const [players, setPlayers] = useState<Player[]>([]);
  const [hostID, setHostID] = useState<string | null>(null);

  const esRef = useRef<EventSource | null>(null);

  /* helper: add/update player */
  const upsert = (list: Player[], p: Player) => {
    const i = list.findIndex((x) => x.playerId === p.playerId);
    return i === -1
      ? [...list, p]
      : [...list.slice(0, i), p, ...list.slice(i + 1)];
  };

  /* ───────── open SSE on mount ───────── */
  useEffect(() => {
    if (!roomCode) return; // safety

    /* backend streams lobby on this exact URL */
    const url = `${API}/joinRoom/${roomCode}/${encodeURIComponent(myName)}`;
    const es = new EventSource(url);
    esRef.current = es;

    es.addEventListener("open", () => console.log("[Lobby SSE] connected"));

    /* INITIAL: full snapshot */
    es.addEventListener("INITIAL", (ev) => {
      try {
        const { players, host } = JSON.parse(ev.data) as {
          players: Player[];
          host: string;
        };
        setPlayers(players);
        setHostID(host);
      } catch {
        console.warn("[INITIAL] not JSON:", ev.data);
      }
    });

    /* JOIN: "playerID:name" */
    es.addEventListener("JOIN", (ev) => {
      const [pid, ...nameParts] = ev.data.split(":");
      if (!pid) return;
      const p = { playerId: pid, name: nameParts.join(":") || "Player" };
      setPlayers((cur) => upsert(cur, p));
    });

    /* LEAVE: "playerID:name" (use the ID part) */
    es.addEventListener("LEAVE", (ev) => {
      const [pid] = ev.data.split(":");
      if (!pid) return;
      setPlayers((cur) => cur.filter((p) => p.playerId !== pid));
    });

    /* HOST: plain playerID string */
    es.addEventListener("HOST", (ev) => setHostID(ev.data.trim()));

    es.onerror = () => console.error("[Lobby SSE] stream error");

    return () => es.close(); // cleanup on unmount
  }, [roomCode, myName]);

  /* ───────── UI helpers ───────── */
  const leaveRoom = () => navigate("/main");
  const startGame = () => navigate("/game");
  const isHost = myPID === hostID;

  /* ───────── render ───────── */
  return (
    <div className="lobby-wrapper">
      <div className="falling-bricks-wrapper">
        <FallingBricks />
      </div>

      <div className="lobby">
        <h2>Join Code: {roomCode}</h2>
        <p>{players.length}/5 players</p>

        <h3>Players:</h3>
        <ol className="player-list">
          {players.map((p) => (
            <li key={p.playerId} className="player-slot">
              {p.playerId === hostID && "👑 "}
              {p.name}
              {p.playerId === hostID && (
                <span className="host-label"> (Host)</span>
              )}
            </li>
          ))}

          {/* empty slots for consistent layout */}
          {Array.from({ length: 5 - players.length }).map((_, i) => (
            <li key={`empty-${i}`} className="player-slot empty" />
          ))}
        </ol>

        <div className="button-row">
          <button className="leave-button" onClick={leaveRoom}>
            Leave Room
          </button>
          {isHost && (
            <button className="start-button" onClick={startGame}>
              Start Game
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

export default Lobby;
