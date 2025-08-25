import React, { useState } from "react";
import { createPortal } from "react-dom";

/* ─── Assets ───────────────────────────────────────────────────────── */
import backdrop5 from "@/assets/Backdrop.svg";
import drawPileImg from "@/assets/cards/card-back.svg";
import discard1 from "@/assets/cards/action-birthday.svg";
import discard2 from "@/assets/cards/action-dealbreaker.svg";
import discard3 from "@/assets/cards/action-debt-collector.svg";

/* ─── Styles ───────────────────────────────────────────────────────── */
import "@/components/mats/mat_styles/playmat_shared.css";
import "./mat_styles/Playmat5.css";

import { PLAYER_ID_KEY } from "../../constants/constants";

type PlayerKey = "p1" | "p2" | "p3" | "p4" | "p5";

const Playmat5: React.FC = () => {
  const [openHandFor, setOpenHandFor] = useState<PlayerKey | null>(null);
  const myPID = sessionStorage.getItem(PLAYER_ID_KEY) ?? "";

  // Stubbed IDs — replace with real ones
  const pidMap: Record<PlayerKey, string> = {
    p1: "p1",
    p2: "p2",
    p3: "p3",
    p4: "p4",
    p5: "p5",
  };

  const openHand = (pk: PlayerKey) => () => setOpenHandFor(pk);
  const closeHand = () => setOpenHandFor(null);

  return (
    <div className="playing-mat-outline-5-players">
      {/* Full-viewport backdrop */}
      <img className="backdrop" src={backdrop5} alt="5-player backdrop" />

      {/* draw pile */}
      <div className="draw-pile" id="draw-pile">
        <div className="deck">
          <img className="deck-top" src={drawPileImg} alt="Draw pile" />
        </div>
      </div>

      <div className="discard-pile" id="discard-pile">
        <img className="card-1" src={discard1} alt="" aria-hidden />
        <img className="card-2" src={discard2} alt="" aria-hidden />
        <img className="card-3" src={discard3} alt="" aria-hidden />
        <div className="discard-pile2"></div>
      </div>

      {/* PLAYER 3 (top-right) */}
      <div className="player-3-space">
        <div className="text" />
        <div className="property-collection" id="p3-properties" />
        <div className="property-collection2"></div>
        <div className="money-collection-bank">
          <div className="bank-pile" id="p3-bank"></div>
          {myPID === pidMap.p3 && (
            <button className="hand-toggle" onClick={openHand("p3")}>
              Hand
            </button>
          )}
        </div>
      </div>

      {/* PLAYER 4 (top-left) */}
      <div className="player-4-space">
        <div className="money-collection-bank2">
          <div className="bank-pile2" id="p4-bank"></div>
          {myPID === pidMap.p4 && (
            <button className="hand-toggle" onClick={openHand("p4")}>
              Hand
            </button>
          )}
        </div>
        <div className="property-collection3" id="p4-properties" />
        <div className="property-collection4"></div>
        <div className="text2" />
      </div>

      {/* PLAYER 1 (upper-left) */}
      <div className="player-1-space">
        <div className="money-collection-bank3">
          <div className="bank-pile3" id="p1-bank"></div>
          {myPID === pidMap.p1 && (
            <button className="hand-toggle" onClick={openHand("p1")}>
              Hand
            </button>
          )}
        </div>
        <div className="property-collection5" id="p1-properties" />
        <div className="property-collection6"></div>
        <div className="text2" />
      </div>

      {/* PLAYER 5 (right-center) */}
      <div className="player-5-space">
        <div className="money-collection-bank3">
          <div className="bank-pile3" id="p5-bank"></div>
          {myPID === pidMap.p5 && (
            <button className="hand-toggle" onClick={openHand("p5")}>
              Hand
            </button>
          )}
        </div>
        <div className="property-collection5" id="p5-properties" />
        <div className="property-collection6"></div>
        <div className="text2" />
      </div>

      {/* PLAYER 2 (bottom-right) */}
      <div className="player-2-space">
        <div className="money-collection-bank4">
          <div className="bank-pile4" id="p2-bank"></div>
          {myPID === pidMap.p2 && (
            <button className="hand-toggle" onClick={openHand("p2")}>
              Hand
            </button>
          )}
        </div>
        <div className="property-collection7" id="p2-properties" />
        <div className="property-collection2"></div>
        <div className="text2" />
      </div>

      {/* discard pile */}

      {/* hand modal */}
      {openHandFor &&
        myPID === pidMap[openHandFor] &&
        createPortal(
          <div className="hand-modal" onClick={closeHand}>
            <div
              className="hand-modal-inner"
              onClick={(e) => e.stopPropagation()}
            >
              <h2>Your hand</h2>
              {/* TODO: render actual cards */}
              <button onClick={closeHand}>Close</button>
            </div>
          </div>,
          document.body
        )}
    </div>
  );
};

export default Playmat5;
