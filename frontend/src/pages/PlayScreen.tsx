/* src/pages/PlayScreen.tsx -------------------------------------------- */
import React, { lazy, useEffect, useMemo, useRef, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  PLAYERS_KEY,
  PLAYER_ID_KEY,
  ROOM_ID_KEY,
  NAME_KEY,
} from "../constants/constants";
import "../style/PlayScreen.css";

const playmat2 = lazy(() => import("../components/mats/Playmat2"));
const playmat3 = lazy(() => import("../components/mats/Playmat3"));
const playmat4 = lazy(() => import("../components/mats/Playmat4"));
const playmat5 = lazy(() => import("../components/mats/Playmat5"));

// ---- Types that match your Kotlin models (simplified) -----------------
type Card = {
  id: string;
  svgName: string; // e.g., "property-dark-blue.svg"
  type: "property" | "action" | "money";
};

type PlayerState = {
  hand: Card[];
  // other fields exist, but not required here
};

type VisibleGameState = {
  playerAtTurn: string | null;
  winningPlayer: string | null;
  drawPileSize: number;
  discardPile: Card[];
  cardsLeftToPlay: number;
  playerOrder: string[];
  pendingInteractions: unknown;
  playerState: Record<string, PlayerState>;
};

// ---- Config -----------------------------------------------------------
const GAME_API =
  (import.meta as ImportMeta).env?.VITE_GAME_SERVICE ??
  (sessionStorage.getItem("GAME_API") || "") ??
  "http://localhost:8081";

// Convert http(s) -> ws(s)
const toWs = (base: string) =>
  base
    .replace(/^http(s?):\/\//, (_, s) => (s ? "wss://" : "ws://"))
    .replace(/\/+$/, "");

// ---- Helpers ----------------------------------------------------------
const boardFor = (count: number) => {
  switch (count) {
    case 2:
      return playmat2;
    case 3:
      return playmat3;
    case 4:
      return playmat4;
    default:
      return playmat5; // 5-player fallback
  }
};

const PlayScreen: React.FC = () => {
  // You navigate here as /play/:roomCode from Lobby
  const { roomCode = "" } = useParams();
  const navigate = useNavigate();

  const myName = sessionStorage.getItem(NAME_KEY) || "";
  const myPID = sessionStorage.getItem(PLAYER_ID_KEY) || "";
  const roomIdStored = sessionStorage.getItem(ROOM_ID_KEY) || "";
  const roomId = roomCode || roomIdStored;

  // Existing players list to decide which mat to show
  const raw = sessionStorage.getItem(PLAYERS_KEY) ?? "[]";
  const list = JSON.parse(raw) as unknown[];
  const count = Array.isArray(list) ? list.length : 2;
  const Mat = boardFor(count);

  // ---- New: game state + hand UI -------------------------------------
  const [state, setState] = useState<VisibleGameState | null>(null);
  const [hand, setHand] = useState<Card[]>([]);
  const [isAnimating, setIsAnimating] = useState(false);
  const wsRef = useRef<WebSocket | null>(null);

  // Build ws://localhost:8081/ws/play/{roomId}/{playerId}
  const wsUrl = useMemo(() => {
    const base = toWs(GAME_API);
    return `${base}/ws/play/${encodeURIComponent(
      roomIdStored
    )}/${encodeURIComponent(myPID)}`;
  }, [roomId, myPID]);

  // Animate only cards that are newly added (staggered)
  const animateToNewHand = (newHand: Card[]) => {
    const oldIds = new Set(hand.map((c) => c.id));
    const added = newHand.filter((c) => !oldIds.has(c.id));
    if (added.length === 0) {
      setHand(newHand);
      return;
    }
    setIsAnimating(true);
    let curr = [...hand];

    added.forEach((card, i) => {
      setTimeout(() => {
        curr = [...curr, card];
        setHand(curr);
        if (i === added.length - 1) {
          // align to authoritative order from server (in case it differs)
          setTimeout(() => {
            setHand(newHand);
            setIsAnimating(false);
          }, 120);
        }
      }, i * 200);
    });
  };

  useEffect(() => {
    if (!roomId || !myPID || !myName) {
      console.warn(
        "PlayScreen: missing roomId/myPID/myName → redirecting home."
      );
      navigate("/");
      return;
    }

    // Open WebSocket to game-service
    const ws = new WebSocket(wsUrl);
    wsRef.current = ws;

    ws.onopen = () => {
      console.log("[Play] WS open:", wsUrl);
      // If your protocol requires a greeting, send it here:
      // ws.send(JSON.stringify({ type: "JOIN", playerId: myPID, name: myName }));
    };

    ws.onmessage = (evt) => {
      try {
        const msg = JSON.parse(evt.data);

        // If the server sends full VisibleGameState snapshots:
        if (
          msg &&
          "playerAtTurn" in msg &&
          "playerState" in msg &&
          "drawPileSize" in msg
        ) {
          const snapshot = msg as VisibleGameState;
          setState(snapshot);

          const my = snapshot.playerState[myPID];
          const newHand = my?.hand ?? [];
          animateToNewHand(newHand);
          return;
        }

        // If the server sometimes sends typed events:
        if (msg?.type === "DEAL" && Array.isArray(msg.cards)) {
          const cards = msg.cards as Card[];
          cards.forEach((c, i) =>
            setTimeout(() => setHand((prev) => [...prev, c]), i * 200)
          );
          return;
        }
        if (msg?.type === "DRAW" && Array.isArray(msg.cards)) {
          const cards = msg.cards as Card[];
          cards.forEach((c, i) =>
            setTimeout(() => setHand((prev) => [...prev, c]), i * 200)
          );
          return;
        }

        console.log("[Play] Unhandled WS message:", msg);
      } catch {
        // Non-JSON frame (keep quiet but visible)
        console.warn("[Play] Non-JSON WS frame:", evt.data);
      }
    };

    ws.onerror = (e) => {
      console.error("[Play] WS error:", e);
      // (Optional) show a small toast to user
    };

    ws.onclose = (e) => {
      console.log("[Play] WS closed:", e.code, e.reason);
    };

    return () => {
      wsRef.current?.close();
      wsRef.current = null;
    };
  }, [wsUrl, myName, myPID, navigate, roomId, animateToNewHand]);

  return (
    <div className="board-shell">
      {/* Your existing mat */}
      <Mat />

      {/* --- Minimal overlay UI: hand & small top bar ------------------ */}
      <div className="play-topbar">
        <div>
          Room: <b>{roomId}</b>
        </div>
        <div>
          You: <b>{myName}</b>
        </div>
        <div>
          Turn:{" "}
          <b>
            {state?.playerAtTurn === myPID
              ? "Yours"
              : state?.playerAtTurn ?? "-"}
          </b>
        </div>
      </div>

      <div className={`hand-overlay ${isAnimating ? "animating" : ""}`}>
        {hand.map((card) => (
          <div key={card.id} className="hand-card" title={card.svgName}>
            {/* Swap this for your svg map if you have one */}
            <img
              src={`/assets/cards/${card.svgName}`}
              alt={card.svgName}
              draggable={false}
            />
          </div>
        ))}
      </div>
    </div>
  );
};

export default PlayScreen;
