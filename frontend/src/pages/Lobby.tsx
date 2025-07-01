/* src/pages/Lobby.tsx  */
import React, {useCallback, useEffect, useRef, useState} from "react";
import {useNavigate, useParams} from "react-router-dom";
import FallingBricks from "../components/FallingBricks";
import "../style/Lobby.css";
import type {Player} from "./Mainmenu.tsx";
import {NAME_KEY, PLAYER_ID_KEY, PLAYERS_KEY} from "../constants.ts";

const API = import.meta.env.VITE_API_BASE ?? "http://localhost:8080";

const buildSSEURL = (room: string, name: string, playerId: string | null = null): string => {
  return `${API}/joinRoom/${room}/${encodeURIComponent(name)}${playerId !== null ? `?playerId=${playerId}` : "" }`;
};

const Lobby: React.FC = () => {
  const { roomCode = "" } = useParams();
  const navigate = useNavigate();

  const myName = sessionStorage.getItem(NAME_KEY) || "";
  const [myPID] = useState<string>(
    sessionStorage.getItem(PLAYER_ID_KEY) || ""
  );

  console.log(`Players List: ${JSON.stringify(sessionStorage.getItem(PLAYERS_KEY))}`)
  const [players, setPlayers] = useState<Player[]>(JSON.parse(sessionStorage.getItem(PLAYERS_KEY)!!));
  const [hostID, setHostID] = useState<string | null>(null);
  const esRef = useRef<EventSource | null>(null);

  // Use useCallback to memoize upsert, though not strictly necessary here,
  // it's a good pattern for functions passed to child components or effects.
  const upsert = useCallback((list: Player[], p: Player): Player[] => {
    const i = list.findIndex((x) => x.playerId === p.playerId);
    if (i === -1) {
      // Player not found, add them
      return [...list, p];
    } else {
      // Player found, update their info (e.g., name might change)
      // This is crucial for preventing true duplicates and updating existing entries.
      return [...list.slice(0, i), p, ...list.slice(i + 1)]
    }
  }, []);

  useEffect(() => {
    sessionStorage.setItem(PLAYERS_KEY, JSON.stringify(players))
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

    esRef.current = new EventSource(url); // Store the new EventSource instance

    const handleJoin = (ev: MessageEvent) => {
      try {
        const [pid, ...nameParts] = ev.data.split(":");
        if (!pid) {
          console.warn("[SSE JOIN] Received event with no player ID.");
          return;
        }
        const name = nameParts.join(":") || "Player";
        console.log(`[SSE JOIN] Player joined: ${name} (${pid})`);
        // Use upsert to prevent true duplicates if event fires multiple times
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
      // Filter is naturally idempotent (removing a non-existent player does nothing)
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

    esRef.current.addEventListener("JOIN", handleJoin);
    esRef.current.addEventListener("LEAVE", handleLeave);
    esRef.current.addEventListener("HOST", handleHost);

    esRef.current.onerror = (error) => {
      console.error("[Lobby] SSE error:", error);
      // Immediately close and clear the ref on error to prevent re-use of a broken connection
      if (esRef.current) {
        esRef.current.close();
        esRef.current = null;
      }
      // Consider a retry mechanism or error display here
    };

    // Cleanup function for useEffect
    return () => {
      console.log(
        "Lobby: Cleaning up SSE connection on unmount or effect re-run."
      );
      // This is the primary place to ensure the EventSource is closed.
      if (esRef.current) {
        esRef.current.close();
        esRef.current = null;
      }
    };
    // Add `upsert` to dependencies of main useEffect as it's a callback function
  }, [ roomCode, myName, navigate, upsert]);

  const leaveRoom = () => {
    navigate("/");
  };

  const startGame = () => navigate(`/game/${roomCode}`);

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
