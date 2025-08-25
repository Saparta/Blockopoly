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

/* ---------- Shared DnD mime ---------- */
export const CARD_MIME = "application/x-blockopoly-card";

/* URL-based import so no svg.d.ts is required */
const cardBack = new URL("../assets/cards/card-back.svg", import.meta.url).href;

/* mats */
const Playmat2 = lazy(() => import("../components/mats/Playmat2"));
const Playmat3 = lazy(() => import("../components/mats/Playmat3"));
const Playmat4 = lazy(() => import("../components/mats/Playmat4"));
const Playmat5 = lazy(() => import("../components/mats/Playmat5"));

/* ---------- Server-aligned types (subset we use) ---------- */
export type ServerCardType =
  | "GENERAL_ACTION"
  | "RENT_ACTION"
  | "PROPERTY"
  | "MONEY";
export type ServerCard = {
  id: number; // 999 when hidden
  type: ServerCardType;
  value?: number | null; // value may be null for rainbow wilds on backend side
  actionType?: string; // e.g., PASS_GO, SLY_DEAL...
  colors?: string[]; // color names from backend enum
};
type ServerPlayerState = {
  hand: ServerCard[];
  propertyCollection: Record<string, unknown>; // placeholder; server owns truth
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

/* ---------- Client→Server actions (match Kotlin @SerialName) ---------- */
type Color = string;
type Action =
  | { type: "EndTurn" }
  | { type: "PlayMoney"; id: number }
  | { type: "PlayProperty"; id: number; color: Color }
  | { type: "PassGo"; id: number };

/* ---------- Config ---------- */
const GAME_API = import.meta.env.game_service ?? "http://localhost:8081";
const toWs = (base: string) =>
  base
    .replace(/^http(s?):\/\//, (_, s) => (s ? "wss://" : "ws://"))
    .replace(/\/+$/, "");

/* ---------- Helpers ---------- */
const assetForCard = (c: ServerCard) =>
  !c || c.id === 999 ? cardBack : cardAssetMap[c.id] ?? cardBack;
const boardFor = (count: number) => {
  switch (count) {
    case 2:
      return Playmat2;
    case 3:
      return Playmat3;
    case 4:
      return Playmat4;
    default:
      return Playmat5;
  }
};

/* ---------- Type guards (no `any`) ---------- */
function isObject(v: unknown): v is Record<string, unknown> {
  return typeof v === "object" && v !== null;
}
function isStateEnvelope(
  m: unknown
): m is { type: "STATE"; gameState: ServerGameState } {
  return isObject(m) && m.type === "STATE" && "gameState" in m;
}
function isStartTurn(
  m: unknown
): m is { type: "START_TURN"; playerId: string } {
  return (
    isObject(m) && m.type === "START_TURN" && typeof m.playerId === "string"
  );
}
function isDraw(
  m: unknown
): m is { type: "DRAW"; playerId: string; cards: ServerCard[] } {
  return (
    isObject(m) &&
    m.type === "DRAW" &&
    typeof m.playerId === "string" &&
    Array.isArray(m.cards)
  );
}
function isRawState(m: unknown): m is ServerGameState {
  return (
    isObject(m) &&
    "playerAtTurn" in m &&
    "playerState" in m &&
    "drawPileSize" in m
  );
}

/* ---------- Lobby players (for names) ---------- */
type LobbyPlayer = { id: string; name?: string | null };

const PlayScreen: React.FC = () => {
  const { roomCode = "" } = useParams(); // pretty code (display only)
  const navigate = useNavigate();

  const myName = sessionStorage.getItem(NAME_KEY) || "";
  const myPID = sessionStorage.getItem(PLAYER_ID_KEY) || "";
  const roomId = sessionStorage.getItem(ROOM_ID_KEY) || ""; // REAL backend ID

  /* name map */
  const playersRaw = sessionStorage.getItem(PLAYERS_KEY) ?? "[]";
  const players = useMemo(
    () => (JSON.parse(playersRaw) as LobbyPlayer[]).filter(Boolean),
    [playersRaw]
  );
  const nameById = useMemo(() => {
    const m: Record<string, string> = {};
    for (const p of players) {
      if (p?.id && p.name && p.name.trim()) {
        m[p.id] = p.name.trim();
      }
    }
    if (myPID && myName && myName.trim()) {
      m[myPID] = myName.trim();
    }
    return m;
  }, [players, myPID, myName]);

  /* UI state */
  const [game, setGame] = useState<ServerGameState | null>(null);
  const [myHand, setMyHand] = useState<ServerCard[]>([]);
  const [isAnimating, setIsAnimating] = useState(false);
  const [wsReady, setWsReady] = useState(false);

  /* choose color modal when dropping multicolor props */
  const [pendingPropDrop, setPendingPropDrop] = useState<{
    card: ServerCard;
  } | null>(null);

  /* layout (p1/p2) derived from order once known */
  const [layout, setLayout] = useState<{ p1: string; p2: string } | null>(null);

  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const backoffRef = useRef(500);

  const wsUrl = useMemo(() => {
    const base = toWs(GAME_API);
    return `${base}/ws/play/${encodeURIComponent(roomId)}/${encodeURIComponent(
      myPID
    )}`;
  }, [roomId, myPID]);

  /* send helper */
  const wsSend = useCallback(
    (action: Action) => {
      const ws = wsRef.current;
      if (!ws || ws.readyState !== WebSocket.OPEN) return;
      ws.send(JSON.stringify({ playerId: myPID, command: action }));
    },
    [myPID]
  );

  /* hand animation */
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
      return prev;
    });
  }, []);

  /* END TURN */
  const sendEndTurn = useCallback(() => wsSend({ type: "EndTurn" }), [wsSend]);

  /* message handlers */
  const handleStateSnapshot = useCallback(
    (snapshot: ServerGameState) => {
      setGame(snapshot);
      if (!layout && snapshot.playerOrder?.length >= 2) {
        setLayout({ p1: snapshot.playerOrder[0], p2: snapshot.playerOrder[1] });
      }
      const mine = snapshot.playerState?.[myPID];
      animateToNewHand(mine?.hand ?? []);
    },
    [myPID, animateToNewHand, layout]
  );

  const handleDraw = useCallback(
    (playerId: string, cards: ServerCard[]) => {
      if (playerId !== myPID) return;
      cards.forEach((c, i) =>
        setTimeout(() => setMyHand((prev) => [...prev, c]), i * 200)
      );
    },
    [myPID]
  );

  /* connect */
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
    };
    ws.onmessage = (evt) => {
      try {
        const msg: StateEnvelope = JSON.parse(evt.data);
        if (isStateEnvelope(msg)) {
          handleStateSnapshot(msg.gameState);
          return;
        }
        if (isStartTurn(msg)) {
          setGame((g) => (g ? { ...g, playerAtTurn: msg.playerId } : g));
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
    ws.onerror = (e) => console.error("[Play] WS error:", e);
    ws.onclose = () => {
      setWsReady(false);
      const delay = Math.min(backoffRef.current, 6000);
      backoffRef.current = Math.min(delay * 2, 6000);
      if (reconnectTimer.current) clearTimeout(reconnectTimer.current);
      reconnectTimer.current = setTimeout(() => connect(), delay);
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

  /* name helper */
  const displayName = useCallback(
    (pid?: string | null) => {
      if (!pid) return "null";
      if (pid === myPID) return (myName && myName.trim()) || "You";

      const n = nameById[pid];
      return n && n.trim() ? n.trim() : "Opponent";
    },
    [nameById, myPID, myName]
  );

  /* DnD — hand cards draggable */
  const onDragStartCard = (card: ServerCard) => (e: React.DragEvent) => {
    e.dataTransfer.effectAllowed = "move";
    e.dataTransfer.setData(CARD_MIME, JSON.stringify(card));
  };

  /* guards */
  const isMyTurn = game?.playerAtTurn === myPID;
  const canEndTurn = !!isMyTurn;

  /* drops received from mat */
  const dropToBank = useCallback(
    (playerKey: "p1" | "p2", card: ServerCard) => {
      if (!layout) return;
      const myKey =
        layout.p1 === myPID ? "p1" : layout.p2 === myPID ? "p2" : null;
      if (!isMyTurn || !myKey || playerKey !== myKey) return;

      // Bank MONEY or ACTION (including RENT); backend validates.
      if (
        card.type === "MONEY" ||
        card.type === "GENERAL_ACTION" ||
        card.type === "RENT_ACTION"
      ) {
        wsSend({ type: "PlayMoney", id: card.id });
      } else {
        console.warn(
          "Ignoring bank drop for PROPERTY (not liquid money):",
          card.id
        );
      }
    },
    [isMyTurn, myPID, layout, wsSend]
  );

  const dropToProps = useCallback(
    (playerKey: "p1" | "p2", card: ServerCard) => {
      if (!layout) return;
      const myKey =
        layout.p1 === myPID ? "p1" : layout.p2 === myPID ? "p2" : null;
      if (!isMyTurn || !myKey || playerKey !== myKey) return;

      if (card.type !== "PROPERTY") return;
      const colors = card.colors ?? [];
      if (colors.length <= 1) {
        // single-color property; use the only color
        const color = colors[0] ?? "";
        if (!color) return;
        wsSend({ type: "PlayProperty", id: card.id, color });
      } else {
        // multicolor or rainbow → open picker
        setPendingPropDrop({ card });
      }
    },
    [isMyTurn, myPID, layout, wsSend]
  );

  const dropToDiscard = useCallback(
    (card: ServerCard) => {
      if (!isMyTurn) return;
      // auto-play simple actions only
      if (card.type === "GENERAL_ACTION" && card.actionType === "PASS_GO") {
        wsSend({ type: "PassGo", id: card.id });
        return;
      }
      console.warn(
        "Action needs targeting UI; not auto-playing:",
        card.actionType
      );
    },
    [isMyTurn, wsSend]
  );

  /* choose color modal commit */
  const commitPropertyColor = (color: string) => {
    if (!pendingPropDrop) return;
    wsSend({ type: "PlayProperty", id: pendingPropDrop.card.id, color });
    setPendingPropDrop(null);
  };

  /* ----- choose mat by count ----- */
  const playerCount = (game?.playerOrder?.length ?? players.length) || 2;
  const Mat = boardFor(playerCount);

  /* discard images */
  const discardImages = (game?.discardPile ?? []).slice(-3).map(assetForCard);

  return (
    <div className="board-shell">
      <Mat
        /* DnD + names + discard */
        layout={boardFor(playerCount) === Mat ? layout : null}
        myPID={myPID}
        names={nameById}
        discardImages={discardImages}
        onDropBank={dropToBank}
        onDropProps={dropToProps}
        onDropDiscard={dropToDiscard}
      />

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
        >
          End Turn
        </button>
      </div>

      {/* Hand — draggable */}
      <div className={`hand-overlay ${isAnimating ? "animating" : ""}`}>
        {myHand.map((card, idx) => (
          <div
            key={`${card.id}-${idx}`}
            className="hand-card"
            draggable
            onDragStart={onDragStartCard(card)}
            title={`${card.type}${
              card.actionType ? `: ${card.actionType}` : ""
            }`}
          >
            <img src={assetForCard(card)} alt={card.type} draggable={false} />
          </div>
        ))}
      </div>

      {/* Color picker modal for multicolor/rainbow property drops */}
      {pendingPropDrop && (
        <div className="hand-modal" onClick={() => setPendingPropDrop(null)}>
          <div
            className="hand-modal-inner"
            onClick={(e) => e.stopPropagation()}
          >
            <h3>Choose a color to play this property</h3>
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
              {(pendingPropDrop.card.colors ?? []).map((c) => (
                <button key={c} onClick={() => commitPropertyColor(c)}>
                  {c}
                </button>
              ))}
            </div>
            <div style={{ marginTop: 12 }}>
              <button onClick={() => setPendingPropDrop(null)}>Cancel</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default PlayScreen;
