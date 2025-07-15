import React, { useState } from "react";
import { createPortal } from "react-dom";

/* ─── Assets ───────────────────────────────────────────────────────── */
import backdrop4 from "@/assets/Backdrop.svg"; // 4-player backdrop
import drawPileImg from "@/assets/cards/card-back.svg";
import discard1 from "@/assets/cards/action-birthday.svg";
import discard2 from "@/assets/cards/action-dealbreaker.svg";
import discard3 from "@/assets/cards/action-debt-collector.svg";

/* ─── Styles ───────────────────────────────────────────────────────── */
import "@/components/mats/mat_styles/playmat_shared.css";
import "./mat_styles/Playmat4.css";

import { PLAYER_ID_KEY } from "../../constants/constants";

type PlayerKey = "p1" | "p2" | "p3" | "p4";

const Playmat4: React.FC = () => {
  const [openHandFor, setOpenHandFor] = useState<PlayerKey | null>(null);

  const myPID = sessionStorage.getItem(PLAYER_ID_KEY) ?? "";
  // TODO: replace these stub IDs with your real game/lobby IDs
  const pidMap: Record<PlayerKey, string> = {
    p1: "p1",
    p2: "p2",
    p3: "p3",
    p4: "p4",
  };

  const openHand = (pk: PlayerKey) => () => setOpenHandFor(pk);
  const closeHand = () => setOpenHandFor(null);

  return (
    <div className="playing-mat-outline-4-players">
      {/* Full-viewport backdrop */}
      <img className="backdrop" src={backdrop4} alt="4-player backdrop" />

      {/* ── DRAW PILE ────────────────────────────────────────────── */}
      <div className="draw-pile" id="draw-pile">
        <img className="deck-top" src={drawPileImg} alt="Draw pile" />
        <div className="deck">Deck</div>
      </div>

      {/* ── PLAYER 3 (right) ────────────────────────────────────── */}
      <div className="player-3-space">
        <div className="money-collection-bank">
          <div className="bank-pile" id="p3-bank"></div>
          {myPID === pidMap.p3 && (
            <button className="hand-toggle" onClick={openHand("p3")}>
              Hand
            </button>
          )}
        </div>
        <div className="property-collection" id="p3-properties" />
        <div className="property-collection2"></div>
        <div className="text" />
      </div>

      {/* ── PLAYER 4 (top) ───────────────────────────────────────── */}
      <div className="player-4-space">
        <div className="money-collection-bank">
          <div className="bank-pile" id="p4-bank"></div>
          {myPID === pidMap.p4 && (
            <button className="hand-toggle" onClick={openHand("p4")}>
              Hand
            </button>
          )}
        </div>
        <div className="property-collection" id="p4-properties" />
        <div className="property-collection2"></div>
        <div className="text" />
      </div>

      {/* ── PLAYER 1 (left) ──────────────────────────────────────── */}
      <div className="player-1-space">
        <div className="money-collection-bank">
          <div className="bank-pile" id="p1-bank"></div>
          {myPID === pidMap.p1 && (
            <button className="hand-toggle" onClick={openHand("p1")}>
              Hand
            </button>
          )}
        </div>
        <div className="property-collection3" id="p1-properties" />
        <div className="property-collection4"></div>
        <div className="text" />
      </div>

      {/* ── PLAYER 2 (bottom) ────────────────────────────────────── */}
      <div className="player-2-space">
        <div className="money-collection-bank">
          <div className="bank-pile" id="p2-bank"></div>
          {myPID === pidMap.p2 && (
            <button className="hand-toggle" onClick={openHand("p2")}>
              Hand
            </button>
          )}
        </div>
        <div className="property-collection" id="p2-properties" />
        <div className="property-collection2"></div>
        <div className="text" />
      </div>

      {/* ── DISCARD PILE ─────────────────────────────────────────── */}
      <div className="discard-pile" id="discard-pile">
        <img className="card-1" src={discard1} alt="" aria-hidden />
        <img className="card-2" src={discard2} alt="" aria-hidden />
        <img className="card-3" src={discard3} alt="" aria-hidden />
        <div className="discard-pile2"></div>
      </div>

      {/* ── HAND MODAL ────────────────────────────────────────────── */}
      {openHandFor &&
        myPID === pidMap[openHandFor] &&
        createPortal(
          <div className="hand-modal" onClick={closeHand}>
            <div
              className="hand-modal-inner"
              onClick={(e) => e.stopPropagation()}
            >
              <h2>Your hand</h2>
              {/* TODO: render cards */}
              <button onClick={closeHand}>Close</button>
            </div>
          </div>,
          document.body
        )}
    </div>
  );
};

export default Playmat4;
