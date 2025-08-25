import React, { useState } from "react";
import { createPortal } from "react-dom";

/* ------- images ------- */
import backdrop from "@/assets/Backdrop.svg";
import drawPileImg from "@/assets/cards/card-back.svg";
import discard1 from "@/assets/cards/action-birthday.svg";
import discard2 from "@/assets/cards/action-dealbreaker.svg";
import discard3 from "@/assets/cards/action-debt-collector.svg";

import "@/components/mats/mat_styles/playmat_shared.css";
import "./mat_styles/Playmat2.css";

import { PLAYER_ID_KEY } from "../../constants/constants";

type PlayerKey = "p1" | "p2";

const Playmat2: React.FC = () => {
  /* ------------------------------------------------------------------ */
  /* local state                                                        */
  /* ------------------------------------------------------------------ */
  const [openHandFor, setOpenHandFor] = useState<PlayerKey | null>(null);

  const myPID = sessionStorage.getItem(PLAYER_ID_KEY) ?? "";
  /* TODO – replace with real IDs from your lobby/game store            */
  const pidMap: Record<PlayerKey, string> = { p1: "p1", p2: "p2" };

  const openHand = (pk: PlayerKey) => () => setOpenHandFor(pk);
  const closeHand = () => setOpenHandFor(null);

  /* ------------------------------------------------------------------ */
  /* render                                                             */
  /* ------------------------------------------------------------------ */
  return (
    <div className="playing-mat-outline-2-players">
      {/* board backdrop */}
      <img className="backdrop" src={backdrop} alt="2-player backdrop" />
      <div className="mat-stage">
        {/* -------- Player 1 area -------------------------------------- */}
        <div className="player-1-space">
          <div className="property-collection-zone">
            <div className="property-collection" id="p1-properties" />
          </div>

          <div className="money-collection-bank">
            <div className="bank-pile" id="p1-bank"></div>
            {myPID === pidMap.p1 && (
              <button className="hand-toggle" onClick={openHand("p1")}>
                Hand
              </button>
            )}
          </div>
        </div>

        {/* -------- Player 2 area -------------------------------------- */}
        <div className="player-2-space">
          <div className="player-2-property-collection-zone">
            <div className="property-collection2" id="p2-properties" />
          </div>

          <div className="player-2-money-collection-bank">
            <div className="bank-pile2" id="p2-bank"></div>
            {myPID === pidMap.p2 && (
              <button className="hand-toggle" onClick={openHand("p2")}>
                Hand
              </button>
            )}
          </div>
        </div>

        {/* -------------------- Draw / discard piles ------------------- */}
        <div className="center-pile">
          <div className="draw-pile">
            <div className="deck">
              <img className="deck-top" src={drawPileImg} alt="Deck" />
            </div>
          </div>

          <div className="discard-pile">
            <img className="card-1" src={discard1} alt="" aria-hidden />
            <img className="card-2" src={discard2} alt="" aria-hidden />
            <img className="card-3" src={discard3} alt="" aria-hidden />
          </div>
        </div>
      </div>

      {/* -------------------- Hand modal (local player) -------------- */}
      {openHandFor &&
        myPID === pidMap[openHandFor] &&
        createPortal(
          <div className="hand-modal" onClick={closeHand}>
            <div
              className="hand-modal-inner"
              onClick={(e) => e.stopPropagation()}
            >
              <h2>Your hand</h2>
              {/* TODO – map real cards */}
              <button onClick={closeHand}>Close</button>
            </div>
          </div>,
          document.body
        )}
    </div>
  );
};

export default Playmat2;
