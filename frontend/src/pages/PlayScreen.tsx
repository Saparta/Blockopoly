/* src/pages/PlayScreen.tsx -------------------------------------------- */
import React, {
  lazy,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  PLAYERS_KEY,
  PLAYER_ID_KEY,
  ROOM_ID_KEY,
  NAME_KEY,
} from "../constants/constants";
import "../style/PlayScreen.css";

import { cardAssetMap } from "../utils/cardmapping";

// URL-based import so no svg.d.ts is required
const cardBack = new URL("../assets/cards/card-back.svg", import.meta.url).href;

const playmat2 = lazy(() => import("../components/mats/Playmat2"));
const playmat3 = lazy(() => import("../components/mats/Playmat3"));
const playmat4 = lazy(() => import("../components/mats/Playmat4"));
const playmat5 = lazy(() => import("../components/mats/Playmat5"));

/** --- Server-aligned types (only what's needed here) ----------------- */
type ServerCardType = "GENERAL_ACTION" | "RENT_ACTION" | "PROPERTY" | "MONEY";
type ServerCard = {
  id: number; // 999 when hidden
  type: ServerCardType;
  value?: number;
  actionType?: string;
  colors?: string[];
};

type ServerPlayerState = {
  hand: ServerCard[];
  propertyCollection: Record<string, ServerCard[]>;
  bank: ServerCard[];
};

type ServerGameState = {
  playerAtTurn: string | null;
  winningPlayer: string | null;
  cardsLeftToPlay: number;
  playerOrder: string[];
  drawPileSize: number;
  pendingInteractions: Record<string, unknown>;
  playerState: Record<string, ServerPlayerState>;
  discardPile: ServerCard[];
};

type StateEnvelope =
  | { type: "STATE"; gameState: ServerGameState }
  | { type: "START_TURN"; playerId: string }
  | { type: "DRAW"; playerId: string; cards: ServerCard[] }
  | Record<string, unknown>;

/** --- Client→Server actions (match Kotlin @SerialName) --------------- */
type Color = string;
type Action =
  | { type: "EndTurn" }
  | { type: "PlayMoney"; id: number }
  | { type: "PlayProperty"; id: number; color: Color };

/** --- Config --------------------------------------------------------- */
const GAME_API = import.meta.env.game_service ?? "http://localhost:8081";

// Convert http(s) -> ws(s)
const toWs = (base: string) =>
  base
    .replace(/^http(s?):\/\//, (_, s) => (s ? "wss://" : "ws://"))
    .replace(/\/+$/, "");

/** --- Helpers -------------------------------------------------------- */
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

const assetForCard = (c: ServerCard) =>
  !c || c.id === 999 ? cardBack : cardAssetMap[c.id] ?? cardBack;

/** --- Type guards for safe WS handling (no `any`) ------------------- */
function isObject(v: unknown): v is Record<string, unknown> {
  return typeof v === "object" && v !== null;
}
function isStateEnvelope(
  m: unknown
): m is { type: "STATE"; gameState: ServerGameState } {
  if (!isObject(m)) return false;
  return m.type === "STATE" && "gameState" in m;
}
function isStartTurn(
  m: unknown
): m is { type: "START_TURN"; playerId: string } {
  if (!isObject(m)) return false;
  return m.type === "START_TURN" && typeof m.playerId === "string";
}
function isDraw(
  m: unknown
): m is { type: "DRAW"; playerId: string; cards: ServerCard[] } {
  if (!isObject(m)) return false;
  return (
    m.type === "DRAW" &&
    typeof m.playerId === "string" &&
    Array.isArray(m.cards)
  );
}
function isRawState(m: unknown): m is ServerGameState {
  if (!isObject(m)) return false;
  return "playerAtTurn" in m && "playerState" in m && "drawPileSize" in m;
}

/** --- Types for lobby players (from session storage) ----------------- */
type LobbyPlayer = { id: string; name?: string | null };

const PlayScreen: React.FC = () => {
  const { roomCode = "" } = useParams(); // pretty code (display only)
  const navigate = useNavigate();

  const myName = sessionStorage.getItem(NAME_KEY) || "";
  const myPID = sessionStorage.getItem(PLAYER_ID_KEY) || "";
  const storedRoomId = sessionStorage.getItem(ROOM_ID_KEY) || ""; // REAL backend Room ID
  const roomId = storedRoomId;

  // Build id → name map from PLAYERS_KEY (fallbacks if missing)
  const playersRaw = sessionStorage.getItem(PLAYERS_KEY) ?? "[]";
  const players = useMemo(
    () => (JSON.parse(playersRaw) as LobbyPlayer[]).filter(Boolean),
    [playersRaw]
  );
  const nameById = useMemo(() => {
    const m: Record<string, string> = {};
    for (const p of players) {
      if (p?.id) m[p.id] = p.name || p.id;
    }
    // Ensure my name is mapped
    if (myPID && myName) m[myPID] = myName;
    return m;
  }, [players, myPID, myName]);

  const displayName = useCallback(
    (pid?: string | null) => {
      if (!pid) return "-";
      if (pid === myPID) return "Yours";
      const name = nameById[pid];
      return name || `${pid.slice(0, 6)}…`;
    },
    [nameById, myPID]
  );

  const count = Array.isArray(players) ? players.length : 2;
  const Mat = boardFor(count);

  /** --- Local UI state --------------------------------------------- */
  const [game, setGame] = useState<ServerGameState | null>(null);
  const [myHand, setMyHand] = useState<ServerCard[]>([]);
  const [isAnimating, setIsAnimating] = useState(false);
  const [wsReady, setWsReady] = useState(false);

  // lightweight in-card action menu state
  const [menuCard, setMenuCard] = useState<ServerCard | null>(null);
  const [colorChoices, setColorChoices] = useState<string[] | null>(null);

  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const backoffRef = useRef(500); // ms

  // ws://localhost:8081/ws/play/{roomId}/{playerId}
  const wsUrl = useMemo(() => {
    const base = toWs(GAME_API);
    return `${base}/ws/play/${encodeURIComponent(roomId)}/${encodeURIComponent(
      myPID
    )}`;
  }, [roomId, myPID]);

  /** --- Send helper: wraps { playerId, command } -------------------- */
  const wsSend = useCallback(
    (action: Action) => {
      const ws = wsRef.current;
      if (!ws || ws.readyState !== WebSocket.OPEN) return;
      const payload = { playerId: myPID, command: action };
      ws.send(JSON.stringify(payload));
    },
    [myPID]
  );

  /** Animate only newly added cards (stagger in) */
  const animateToNewHand = useCallback((newHand: ServerCard[]) => {
    setIsAnimating(true);
    setMyHand((prev) => {
      const prevIds = new Set(prev.map((c) => c.id));
      const added = newHand.filter((c) => !prevIds.has(c.id));
      if (added.length === 0) {
        setIsAnimating(false);
        return newHand;
      }

      let curr = [...prev];
      added.forEach((card, i) => {
        setTimeout(() => {
          curr = [...curr, card];
          setMyHand(curr);
          if (i === added.length - 1) {
            setTimeout(() => {
              setMyHand(newHand);
              setIsAnimating(false);
            }, 120);
          }
        }, i * 200);
      });
      return prev; // final order is snapped after the animations
    });
  }, []);

  /** END TURN — send proper Command wrapper */
  const sendEndTurn = useCallback(() => {
    wsSend({ type: "EndTurn" });
  }, [wsSend]);

  /** Handle snapshots / events */
  const handleStateSnapshot = useCallback(
    (snapshot: ServerGameState) => {
      setGame(snapshot);
      const mine = snapshot.playerState?.[myPID];
      animateToNewHand(mine?.hand ?? []);
    },
    [myPID, animateToNewHand]
  );

  const handleDraw = useCallback(
    (playerId: string, cards: ServerCard[]) => {
      if (playerId !== myPID) return; // hidden for opponents, backend sends 999 to them
      cards.forEach((c, i) => {
        setTimeout(() => setMyHand((prev) => [...prev, c]), i * 200);
      });
    },
    [myPID]
  );

  /** Connect + backoff reconnect ----------------------------------- */
  const connect = useCallback(() => {
    if (!roomId || !myPID || !myName) {
      console.warn(
        "PlayScreen: missing roomId/myPID/myName → redirecting home."
      );
      navigate("/");
      return;
    }

    const ws = new WebSocket(wsUrl);
    wsRef.current = ws;

    ws.onopen = () => {
      setWsReady(true);
      backoffRef.current = 500;
      console.log("[Play] WS open:", wsUrl);
    };

    ws.onmessage = (evt) => {
      try {
        const msg: StateEnvelope = JSON.parse(evt.data);

        if (isStateEnvelope(msg)) {
          handleStateSnapshot(msg.gameState);
          return;
        }
        if (isStartTurn(msg)) {
          const { playerId } = msg;
          setGame((g) => (g ? { ...g, playerAtTurn: playerId } : g));
          return;
        }
        if (isDraw(msg)) {
          handleDraw(msg.playerId, msg.cards);
          return;
        }
        if (isRawState(msg)) {
          handleStateSnapshot(msg);
          return;
        }

        console.log("[Play] Unhandled WS message:", msg);
      } catch {
        console.warn("[Play] Non-JSON WS frame:", evt.data);
      }
    };

    ws.onerror = (e) => {
      console.error("[Play] WS error:", e);
    };

    ws.onclose = () => {
      setWsReady(false);
      const delay = Math.min(backoffRef.current, 6000);
      backoffRef.current = Math.min(delay * 2, 6000);
      if (reconnectTimer.current) clearTimeout(reconnectTimer.current);
      reconnectTimer.current = setTimeout(() => connect(), delay);
      console.log("[Play] WS closed. Reconnecting in", delay, "ms");
    };
  }, [navigate, wsUrl, roomId, myPID, myName, handleStateSnapshot, handleDraw]);

  useEffect(() => {
    connect();
    return () => {
      if (reconnectTimer.current) clearTimeout(reconnectTimer.current);
      wsRef.current?.close();
      wsRef.current = null;
    };
  }, [connect]);

  /** UI helpers */
  const isMyTurn = game?.playerAtTurn === myPID;
  const canEndTurn = !!isMyTurn; // tighten with cardsLeftToPlay if desired

  /** --- Card interactions: bank / play property --------------------- */
  const onCardClick = (card: ServerCard) => {
    if (!isMyTurn) return; // only on your turn
    setMenuCard(card);
    if (card.type === "PROPERTY") {
      // choose color if needed
      const colors = card.colors ?? [];
      setColorChoices(colors.length > 1 ? colors : null);
    } else {
      setColorChoices(null);
    }
  };

  const bankSelected = () => {
    if (!menuCard) return;
    wsSend({ type: "PlayMoney", id: menuCard.id });
    setMenuCard(null);
    setColorChoices(null);
  };

  const playPropertySelected = (color?: string) => {
    if (!menuCard) return;
    const chosen =
      color ??
      (menuCard.colors && menuCard.colors.length > 0
        ? menuCard.colors[0]
        : undefined);
    if (!chosen) {
      // no color available → nothing to do
      setMenuCard(null);
      return;
    }
    wsSend({ type: "PlayProperty", id: menuCard.id, color: chosen });
    setMenuCard(null);
    setColorChoices(null);
  };

  return (
    <div className="board-shell">
      <Mat />

      {/* Top bar */}
      <div className="play-topbar">
        <div>
          Room Code: <b>{roomCode || "—"}</b>
        </div>
        <div>
          Room ID: <b>{roomId || "—"}</b>
        </div>
        <div>
          You: <b>{myName}</b>
        </div>
        <div>
          Turn: <b>{displayName(game?.playerAtTurn)}</b>
        </div>
        <div>
          Draw pile: <b>{game?.drawPileSize ?? "-"}</b>
        </div>
        <div
          className={`ws-dot ${wsReady ? "on" : "off"}`}
          title={wsReady ? "Connected" : "Reconnecting..."}
        />
      </div>

      {/* Actions */}
      <div className="play-actions">
        <button
          className="endturn-btn"
          onClick={sendEndTurn}
          disabled={!canEndTurn}
          title={canEndTurn ? "End your turn" : "Wait for your turn"}
        >
          End Turn
        </button>
      </div>

      {/* Hand */}
      <div className={`hand-overlay ${isAnimating ? "animating" : ""}`}>
        {myHand.map((card, idx) => (
          <div
            key={`${card.id}-${idx}`}
            className="hand-card"
            title={`${card.type}${
              card.actionType ? `: ${card.actionType}` : ""
            }`}
            onClick={() => onCardClick(card)}
          >
            <img src={assetForCard(card)} alt={card.type} draggable={false} />
          </div>
        ))}
      </div>

      {/* Minimal card action menu (inline) */}
      {menuCard && (
        <div className="card-menu">
          <div className="card-menu-row">
            <span>
              Selected: #{menuCard.id} {menuCard.type}
            </span>
          </div>
          <div className="card-menu-row">
            {/* Bank is always available (server will validate) */}
            <button onClick={bankSelected}>Bank</button>

            {/* Property play */}
            {menuCard.type === "PROPERTY" && !colorChoices && (
              <button onClick={() => playPropertySelected()}>
                Play as Property
              </button>
            )}
          </div>

          {menuCard.type === "PROPERTY" && colorChoices && (
            <div className="card-menu-row">
              {colorChoices.map((c) => (
                <button key={c} onClick={() => playPropertySelected(c)}>
                  Play as {c}
                </button>
              ))}
            </div>
          )}

          <div className="card-menu-row">
            <button
              onClick={() => {
                setMenuCard(null);
                setColorChoices(null);
              }}
            >
              Cancel
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default PlayScreen;
