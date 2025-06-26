/* src/pages/Lobby.tsx  */
import React, { useEffect, useRef, useState, useCallback } from "react"; // Added useCallback
import { useParams, useNavigate } from "react-router-dom";
import FallingBricks from "../components/FallingBricks";
import "../style/Lobby.css";

const API = import.meta.env.VITE_API_BASE ?? "http://localhost:8080";

export interface Player {
  playerId: string;
  name: string;
}

export interface LobbyInitialPayload {
  playerID?: string;
  name?: string;
  roomId?: string;
  roomCode?: string;
  players?: Player[];
  host?: string;
}

const buildSSEURL = (isHost: boolean, room: string, name: string): string => {
  if (isHost) {
    return `${API}/createRoom/${encodeURIComponent(name)}`;
  } else {
    return `${API}/joinRoom/${room}/${encodeURIComponent(name)}`;
  }
};

const Lobby: React.FC = () => {
  const { roomCode = "" } = useParams();
  const navigate = useNavigate();

  const myName = localStorage.getItem("name") || "";
  const [myPID, setMyPID] = useState<string>(
    sessionStorage.getItem("blockopolyPID") || ""
  );
  const isHostSession = sessionStorage.getItem("blockopolyIsHost") === "true";

  const [players, setPlayers] = useState<Player[]>([]);
  const [hostID, setHostID] = useState<string | null>(null);
  const esRef = useRef<EventSource | null>(null);
  const isInitialMount = useRef(true);

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
      return [...list.slice(0, i), p, ...list.slice(i + 1)];
    }
  }, []);

  useEffect(() => {
    if (!myName || !roomCode) {
      console.warn(
        "Lobby: Missing myName or roomCode. Cannot establish SSE connection. Redirecting..."
      );
      navigate("/");
      return;
    }

    // Defensive check: If an EventSource instance already exists in the ref,
    // ensure it's closed before proceeding. This is critical for preventing duplicates.
    if (esRef.current) {
      console.log(
        "Lobby: Closing existing EventSource connection found in ref."
      );
      esRef.current.close();
      esRef.current = null;
    }

    const url = buildSSEURL(isHostSession, roomCode, myName);
    console.log(`Lobby: Attempting to establish new SSE connection to: ${url}`);

    const es = new EventSource(url);
    esRef.current = es; // Store the new EventSource instance

    const handleInitial = (ev: MessageEvent) => {
      if (typeof ev.data !== "string" || ev.data.trim() === "") {
        console.warn(
          "[SSE INITIAL] Received empty or non-string data. Skipping parse."
        );
        return;
      }
      try {
        const payload: Partial<LobbyInitialPayload> = JSON.parse(ev.data);

        if (payload.playerID) {
          setMyPID(payload.playerID);
          sessionStorage.setItem("blockopolyPID", payload.playerID);
        } else {
          console.warn(
            "[SSE INITIAL] 'playerID' is missing in payload. Full payload:",
            payload
          );
        }

        if (isHostSession) {
          console.log(
            "[SSE INITIAL - Host] Received initial payload:",
            payload
          );
          if (payload.playerID && payload.name) {
            const hostPlayer: Player = {
              playerId: payload.playerID,
              name: payload.name,
            };
            // When host receives INITIAL, they are the only one initially.
            // Set the players list to contain only themselves.
            // Subsequent JOIN events will add others.
            setPlayers([hostPlayer]);
            setHostID(payload.playerID);
          } else {
            console.warn(
              "[SSE INITIAL - Host] Missing playerID or name in host's initial payload. Setting empty players."
            );
            setPlayers([]);
            setHostID(null);
          }
        } else {
          console.log(
            "[SSE INITIAL - Joiner] Received initial payload:",
            payload
          );
          const list = payload.players;
          const host = payload.host;

          if (Array.isArray(list)) {
            // For a joiner, the INITIAL event *should* contain the full list of players already in the room.
            // Replace the current players list entirely.
            setPlayers(list);
          } else {
            console.warn(
              "[SSE INITIAL - Joiner] 'players' in payload is not an array or is undefined. Received:",
              list,
              "Full payload:",
              payload
            );
            setPlayers([]);
          }

          if (host) {
            setHostID(host);
          } else {
            console.warn(
              "[SSE INITIAL - Joiner] 'host' ID is missing in payload. Full payload:",
              payload
            );
            setHostID(null);
          }
        }
      } catch (e) {
        console.error(
          "[SSE INITIAL parse error] Error parsing event data or unexpected payload structure:",
          e,
          "Raw data:",
          ev.data
        );
        setPlayers([]);
        setHostID(null);
      }
    };

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

    es.addEventListener("INITIAL", handleInitial);
    es.addEventListener("JOIN", handleJoin);
    es.addEventListener("LEAVE", handleLeave);
    es.addEventListener("HOST", handleHost);

    es.onerror = (error) => {
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
  }, [isHostSession, roomCode, myName, navigate, upsert]);

  useEffect(() => {
    // Only run this effect's cleanup on actual component unmount, not initial render cycle
    if (isInitialMount.current) {
      isInitialMount.current = false;
      return;
    }

    return () => {
      const currentPid = sessionStorage.getItem("blockopolyPID");
      if (currentPid) {
        console.log(
          `Lobby: Sending leaveRoom request for PID: ${currentPid} via beacon.`
        );
        navigator.sendBeacon(`${API}/leaveRoom/${currentPid}`, new Blob());
      } else {
        console.warn(
          "Lobby: No PID found in sessionStorage to send leaveRoom request on unmount."
        );
      }
    };
  }, []);

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
