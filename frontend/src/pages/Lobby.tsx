/* src/pages/Lobby.tsx - It finnally works for create room */
import React, { useEffect, useRef, useState, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import FallingBricks from "../components/FallingBricks";
import "../style/Lobby.css";

const API = import.meta.env.VITE_API_BASE ?? "http://localhost:8080";

// --- Interfaces for Backend  ---

export interface Player {
  playerId: string;
  name: string;
}

// Interface for the /createRoom response (for the 'INITIAL' event for hosts)
export interface CreateRoomPayload {
  playerID: string; // The ID of the host (creator)
  name: string; // The name of the host (creator)
  roomID: string; // The internal ID of the room (may not be used directly by client)
  roomCode: string; // The human-readable code for joining
}

// Interface for the /joinRoom response (for the 'INITIAL' event for joiners)
export interface JoinRoomPayload {
  playerID: string; // The ID of the player who just joined (me)
  players: Player[]; // The COMPLETE list of players currently in the room
}

const Lobby: React.FC = () => {
  const { roomCode: roomCodeFromURL = "" } = useParams();
  const navigate = useNavigate();

  const myName = localStorage.getItem("name") ?? "";

  const [myPID, setMyPID] = useState<string>(
    () => sessionStorage.getItem("blockopolyPID") ?? ""
  );
  const [players, setPlayers] = useState<Player[]>([]);
  const [hostID, setHostID] = useState<string | null>(null);
  const [roomCode, setRoomCode] = useState(roomCodeFromURL);

  // Ref to store the current EventSource instance for explicit closing
  const esRef = useRef<EventSource | null>(null);
  // Ref to track if a connection attempt has been made (to prevent re-running effect on stable deps)
  const connectionAttempted = useRef(false);

  // Utility function to add or update a player in the list, preventing duplicates
  // This is crucial for handling re-connects or out-of-order events robustly.
  const upsert = useCallback((list: Player[], p: Player): Player[] => {
    const i = list.findIndex((x) => x.playerId === p.playerId);
    if (i === -1) {
      // Add new player
      return [...list, p];
    } else {
      // Update existing player (e.g., if their name changed, though not common here)
      // This explicitly prevents adding a duplicate entry if playerId is the same
      return [...list.slice(0, i), p, ...list.slice(i + 1)];
    }
  }, []);

  // Main connection effect - This runs ONCE on component mount
  // or if `myName` or `roomCodeFromURL` change unexpectedly.
  useEffect(() => {
    // --- Pre-connection Checks ---
    if (!myName) {
      console.warn(
        "[Lobby] No 'myName' found in localStorage. Redirecting to home."
      );
      navigate("/");
      return; // Stop execution if no name
    }

    // Prevent re-connecting if an attempt has already been made and connection is active.
    // This is the primary guard against client-side double connections on re-renders.
    if (
      connectionAttempted.current &&
      esRef.current &&
      esRef.current.readyState === EventSource.OPEN
    ) {
      console.log(
        "[Lobby] SSE connection already attempted and is open. Skipping new connection."
      );
      return;
    }

    // Determine connection type (create room or join room)
    const isInitialHost = sessionStorage.getItem("blockopolyIsHost") === "true";

    // For a non-host, a roomCode from the URL is mandatory.
    if (!isInitialHost && !roomCodeFromURL) {
      console.error(
        "[Lobby] Joiner has no room code in URL. Redirecting to home."
      );
      navigate("/");
      return;
    }

    // --- Constructing the SSE URL ---
    const url = isInitialHost
      ? `${API}/createRoom/${encodeURIComponent(myName)}`
      : `${API}/joinRoom/${roomCodeFromURL}/${encodeURIComponent(myName)}`;

    console.log(
      `[Lobby] Attempting new SSE connection for ${
        isInitialHost ? "host" : "joiner"
      }: ${url}`
    );
    connectionAttempted.current = true; // Mark that a connection attempt is being made

    // --- Close any existing connection before opening a new one ---
    // This is a crucial step for preventing leaks if the effect somehow re-runs
    // and a previous EventSource instance is still active.
    if (esRef.current && esRef.current.readyState !== EventSource.CLOSED) {
      console.warn(
        "[Lobby] Closing existing SSE connection before opening a new one."
      );
      esRef.current.close();
      esRef.current = null;
    }

    // --- Establish New EventSource Connection ---
    const es = new EventSource(url);
    esRef.current = es; // Store the new EventSource instance

    // --- Event Handlers ---

    // 'INITIAL' event: Provides the initial state of the room.
    // This is the ONLY place where the client should initially populate the players array
    // when creating a room (as a host) or joining one.
    es.addEventListener("INITIAL", (ev) => {
      try {
        const data = JSON.parse(ev.data);
        console.log("[Lobby] Received INITIAL event data:", data);

        if (isInitialHost) {
          // Logic for the client who just created the room (the host)
          const payload = data as CreateRoomPayload;
          const hostPlayer: Player = {
            playerId: payload.playerID,
            name: payload.name,
          };

          // SET PLAYERS TO ONLY THE HOST. This prevents duplicates if server sends initial host info again.
          setPlayers([hostPlayer]);
          setHostID(payload.playerID);
          setMyPID(payload.playerID);
          sessionStorage.setItem("blockopolyPID", payload.playerID);
          setRoomCode(payload.roomCode);
          window.history.replaceState(null, "", `/lobby/${payload.roomCode}`);
          sessionStorage.removeItem("blockopolyIsHost"); // Clean up host flag
        } else {
          // Logic for a client who just joined an existing room (or refreshed)
          const payload = data as JoinRoomPayload;
          // SET PLAYERS TO THE COMPLETE LIST RECEIVED from server.
          setPlayers(payload.players);
          if (payload.playerID) {
            setMyPID(payload.playerID);
            sessionStorage.setItem("blockopolyPID", payload.playerID);
          }
          // Expect 'HOST' event separately if not included in JoinRoomPayload
        }
      } catch (e) {
        console.error(
          "[Lobby] INITIAL event parse error:",
          e,
          "Data:",
          ev.data
        );
        // Consider error handling here (e.g., redirect, show message)
      }
    });

    // 'JOIN' event: Fired when a NEW player (not the current client during initial connect) joins the room.
    // The server should send this to ALL clients already in the room *except* the one that just joined.
    es.addEventListener("JOIN", (ev) => {
      const [pid, ...nameParts] = ev.data.split(":");
      if (!pid || nameParts.length === 0) {
        console.warn("[Lobby] Malformed JOIN event data:", ev.data);
        return;
      }
      const playerName = nameParts.join(":");
      console.log(`[Lobby] Player joined: ${playerName} (ID: ${pid})`);
      setPlayers((cur) => upsert(cur, { playerId: pid, name: playerName }));
    });

    // 'LEAVE' event: Fired when a player leaves the room.
    es.addEventListener("LEAVE", (ev) => {
      const [pid, ...nameParts] = ev.data.split(":");
      if (!pid) {
        console.warn("[Lobby] Malformed LEAVE event data:", ev.data);
        return;
      }
      const playerName = nameParts.join(":");
      console.log(`[Lobby] Player left: ${playerName} (ID: ${pid})`);
      setPlayers((cur) => cur.filter((p) => p.playerId !== pid));
    });

    // 'HOST' event: Fired when the host changes or for joiners to know the current host.
    es.addEventListener("HOST", (ev) => {
      const newHostId = ev.data.trim();
      if (newHostId) {
        console.log("[Lobby] Host updated to ID:", newHostId);
        setHostID(newHostId);
      } else {
        console.warn("[Lobby] Received empty HOST event data.");
      }
    });

    // --- Error Handling for EventSource ---
    es.onerror = (error) => {
      console.error("[Lobby] SSE connection error:", error);
      // Attempt to close connection on error to prevent zombie states
      if (esRef.current) {
        esRef.current.close();
        esRef.current = null;
      }
      connectionAttempted.current = false; // Allow retrying connection
      // You might want to show an error message or redirect the user here
      // navigate("/");
    };

    // --- Cleanup function for useEffect ---
    // This runs when the component unmounts or before the effect re-runs.
    // Crucial for closing the SSE connection and preventing leaks.
    return () => {
      console.log(
        "[Lobby] SSE connection cleanup initiated for unmount/re-run."
      );
      if (esRef.current) {
        esRef.current.close();
        esRef.current = null;
      }
      connectionAttempted.current = false; // Reset flag for future mounts
    };
  }, [myName, roomCodeFromURL, navigate, upsert]); // Dependencies list

  // Effect to send a beacon when the user leaves the page entirely (e.g., closes tab)
  useEffect(() => {
    const handleBeforeUnload = () => {
      const pid = sessionStorage.getItem("blockopolyPID");
      if (pid && roomCode) {
        console.log(
          `[Lobby] Sending leave beacon for PID: ${pid} from room: ${roomCode} during unload.`
        );
        // navigator.sendBeacon is non-blocking and ideal for sending data on page unload
        navigator.sendBeacon(`${API}/leaveRoom/${roomCode}/${pid}`, new Blob());
      }
    };

    window.addEventListener("beforeunload", handleBeforeUnload);

    return () => {
      window.removeEventListener("beforeunload", handleBeforeUnload);
    };
  }, [API, roomCode]); // `roomCode` is a dependency to ensure beacon has the correct room

  const leaveRoom = () => {
    // Manually close the SSE connection when the user clicks 'Leave'
    if (esRef.current) {
      console.log("[Lobby] Explicitly closing SSE connection on 'Leave Room'.");
      esRef.current.close();
      esRef.current = null;
    }
    // The `beforeunload` handler will take care of sending the beacon if playerID and roomCode are set.
    navigate("/");
  };

  const startGame = () => {
    if (iAmHost && players.length >= 2) {
      console.log(`[Lobby] Host initiating game start for room: ${roomCode}`);
      // Ideally, send a message to the server to officially start the game
      // and transition all players to the game screen via a server-sent event.
      fetch(`${API}/startGame/${roomCode}`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ hostId: myPID }),
      })
        .then((response) => {
          if (!response.ok) {
            console.error(
              "[Lobby] Failed to send start game request to server."
            );
            alert("Failed to start game. Please try again.");
          }
          // Server should broadcast an event (e.g., "GAME_START") that all clients listen to
          // and then navigate to the game page.
        })
        .catch((error) => {
          console.error("[Lobby] Error starting game:", error);
          alert("An error occurred while trying to start the game.");
        });

      // Navigate locally immediately, but ideally wait for server confirmation/broadcast
      // navigate(`/game/${roomCode}`);
    } else if (!iAmHost) {
      console.warn("[Lobby] Only the host can start the game.");
    } else if (players.length < 2) {
      console.warn("[Lobby] Need at least 2 players to start the game.");
    }
  };

  // Derived state for display purposes
  const hostPlayer = players.find((p) => p.playerId === hostID);
  const hostName = hostPlayer?.name ?? "The Host";
  const iAmHost = myPID && myPID === hostID;

  return (
    <div className="lobby-wrapper">
      <div className="falling-bricks-wrapper">
        <FallingBricks />
      </div>

      <div className="lobby">
        <h2>{hostName}&apos;s Room</h2>
        <p>
          Join&nbsp;Code:&nbsp;
          <b className="room-code-display">{roomCode || "..."}</b>
        </p>

        <h3>{players.length}/5 players</h3>
        <ol className="player-list">
          {players.map((p) => (
            <li key={p.playerId} className="player-slot">
              {" "}
              {/* player.playerId is crucial for stable keys */}
              {p.playerId === hostID && "👑 "}
              {p.name}
              {p.playerId === myPID && " (You)"}
            </li>
          ))}
          {/* Render empty slots for visual completeness */}
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
