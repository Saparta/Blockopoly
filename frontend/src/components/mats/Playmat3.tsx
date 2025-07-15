/* src/components/mats/Playmat3.tsx */
import React, { useState } from "react";
import { createPortal } from "react-dom";

/* ─── assets ───────────────────────────────────────────────────────── */
import backdrop from "@/assets/Backdrop.svg"; // your 3-player backdrop
import drawPileImg from "@/assets/cards/card-back.svg";
import discard1 from "@/assets/cards/action-birthday.svg";
import discard2 from "@/assets/cards/action-dealbreaker.svg";
import discard3 from "@/assets/cards/action-debt-collector.svg";

/* ─── styles ───────────────────────────────────────────────────────── */
import "@/components/mats/mat_styles/playmat_shared.css";
import "./mat_styles/Playmat3.css";

import { PLAYER_ID_KEY } from "../../constants/constants";

type PlayerKey = "p1" | "p2" | "p3";

const Playmat3: React.FC = () => {
  const [openHandFor, setOpenHandFor] = useState<PlayerKey | null>(null);

  const myPID = sessionStorage.getItem(PLAYER_ID_KEY) ?? "";
  // TODO: wire up real IDs from your lobby/game store
  const pidMap: Record<PlayerKey, string> = { p1: "p1", p2: "p2", p3: "p3" };

  const openHand = (pk: PlayerKey) => () => setOpenHandFor(pk);
  const closeHand = () => setOpenHandFor(null);

  return (
    <div className="playing-mat-outline">
      <img className="backdrop" src={backdrop} alt="3-Player Backdrop" />

      <div className="mat-stage playing-mat-3-players">
        {/* ── DRAW PILE ─────────────────────────────────────────────── */}
        <div className="draw-pile" id="draw-pile">
          <img
            className="bopoly-dealbreaker-6"
            src={drawPileImg}
            alt="Draw pile"
          />
          <div className="deck">Deck</div>
        </div>

        {/* ── DISCARD PILE ─────────────────────────────────────────── */}
        <div className="discard-pile" id="discard-pile">
          <img className="card-1" src={discard1} alt="" aria-hidden />
          <img className="card-2" src={discard2} alt="" aria-hidden />
          <img className="card-3" src={discard3} alt="" aria-hidden />
          <div className="discard-pile2">
            Discard
            <br />
            Pile
          </div>
        </div>

        {/* ── PLAYER 1 ─────────────────────────────────────────────── */}
        <div className="player-1-space">
          <div className="money-collection-bank">
            <div className="bank-pile" id="p1-bank"></div>
            {myPID === pidMap.p1 && (
              <button className="hand-toggle" onClick={openHand("p1")}>
                Hand
              </button>
            )}
          </div>
          <div className="property-collection" id="p1-properties" />
          <div className="property-collection2"></div>
          <div className="text" />
        </div>

        {/* ── PLAYER 2 ─────────────────────────────────────────────── */}
        <div className="player-2-space">
          <div className="money-collection-bank2">
            <div className="bank-pile" id="p2-bank"></div>
            {myPID === pidMap.p2 && (
              <button className="hand-toggle" onClick={openHand("p2")}>
                Hand
              </button>
            )}
          </div>
          <div className="property-collection3" id="p2-properties" />
          <div className="property-collection4"></div>
          <div className="text" />
        </div>

        {/* ── PLAYER 3 ─────────────────────────────────────────────── */}
        <div className="player-3-space">
          <div className="money-collection-bank2">
            <div className="bank-pile" id="p3-bank">
              Bank
              <br />
              Pile
            </div>
            {myPID === pidMap.p3 && (
              <button className="hand-toggle" onClick={openHand("p3")}>
                Hand
              </button>
            )}
          </div>
          <div className="property-collection3" />
          <div className="property-collection5"></div>
          <div className="text" />
        </div>
      </div>

      {/* ── HAND MODAL ───────────────────────────────────────────── */}
      {openHandFor &&
        myPID === pidMap[openHandFor] &&
        createPortal(
          <div className="hand-modal" onClick={closeHand}>
            <div
              className="hand-modal-inner"
              onClick={(e) => e.stopPropagation()}
            >
              <h2>Your hand</h2>
              {/* TODO: render actual cards here */}
              <button onClick={closeHand}>Close</button>
            </div>
          </div>,
          document.body
        )}
    </div>
  );
};

export default Playmat3;
