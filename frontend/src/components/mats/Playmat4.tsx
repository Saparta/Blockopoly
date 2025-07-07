import React, { useState } from "react";
import ReactDOM from "react-dom";
import drawPileImg from "@/assets/cards/card-back.svg";
import discardPileImg from "@/assets/cards/action-birthday.svg";
import playmatBg from "@/assets/Backdrop.svg";
import { DiscardPile } from "../DiscardPile";

/* shared first, then mat‑specific */
import "@/components/mats/mat_styles/playmat_shared.css";
import "./mat_styles/Playmat4.css";

import { PLAYER_ID_KEY } from "../../constants/constants"; // "playerID"

type PlayerKey = "p1" | "p2" | "p3" | "p4";

const Playmat4: React.FC = () => {
  const [handModal, setHandModal] = useState<PlayerKey | null>(null);
  const myPID = sessionStorage.getItem(PLAYER_ID_KEY) ?? "";

  const openHand = (pk: PlayerKey) => () => setHandModal(pk);
  const closeModal = () => setHandModal(null);

  // stub map until real backend IDs available
  const pidMap: Record<PlayerKey, string> = {
    p1: "p1",
    p2: "p2",
    p3: "p3",
    p4: "p4",
  };

  return (
    <div className="playmat4-wrapper">
      {/* background mat \*/}
      <img src={playmatBg} alt="4‑Player Mat" className="mat-bg" />

      {/* deck + discard */}
      <button className="zone draw-pile" aria-label="Draw a card">
        <div className="card-holder">
          <img src={drawPileImg} alt="Deck" />
        </div>
      </button>
      <div className="zone discard-pile" id="discard-pile">
        <div className="card-holder">
          <img src={discardPileImg} alt="Deck" />
        </div>
        <DiscardPile />
      </div>

      {/* four player rectangles */}
      {(["p1", "p2", "p3", "p4"] as PlayerKey[]).map((pk) => (
        <section key={pk} className={`player-board ${pk}`}>
          <div className="property-slot" id={`${pk}-properties`} />
          <div className="bank-slot" id={`${pk}-bank`} />
          {myPID === pidMap[pk] && (
            <button className="hand-toggle" onClick={openHand(pk)}>
              Hand
            </button>
          )}
        </section>
      ))}

      {handModal &&
        pidMap[handModal] === myPID &&
        ReactDOM.createPortal(
          <div className="hand-modal" onClick={closeModal}>
            <div
              className="hand-modal-inner"
              onClick={(e) => e.stopPropagation()}
            >
              <h2>Your hand</h2>
              {/* TODO: render card list */}
              <button onClick={closeModal}>Close</button>
            </div>
          </div>,
          document.body
        )}
    </div>
  );
};

export default Playmat4;
