/* src/components/mats/Playmat5.tsx */
import React, { useState } from "react";
import ReactDOM from "react-dom";
import drawPileImg from "@/assets/cards/card-back.svg";
import playmatBg from "@/assets/Backdrop.svg";

/* shared styles MUST load first */
import "@/components/mats/mat_styles/playmat_shared.css";
/*  then the mat-specific tweaks */
import "./mat_styles/Playmat5.css";

import { PLAYER_ID_KEY } from "../../constants/constants"; // "playerID"

type PlayerKey = "p1" | "p2" | "p3" | "p4" | "p5";

const Playmat5: React.FC = () => {
  const [handModal, setHandModal] = useState<PlayerKey | null>(null);

  const openHand = (pk: PlayerKey) => () => setHandModal(pk);
  const closeModal = () => setHandModal(null);

  /* grab *my* id from sessionStorage */
  const myPID = sessionStorage.getItem(PLAYER_ID_KEY) ?? "";

  /* helper maps player-key → actual pid (replace with real list) */
  const pidMap: Record<PlayerKey, string> = {
    p1: "p1",
    p2: "p2",
    p3: "p3",
    p4: "p4",
    p5: "p5",
  };
  /* ───────────────────────── render ───────────────────────── */
  return (
    <div className="playmat5-wrapper">
      <img src={playmatBg} alt="5-Player Mat" className="mat-bg" />

      {/* deck / discard */}
      <button className="zone draw-pile" aria-label="Draw a card">
        <div className="card-holder">
          <img src={drawPileImg} alt="Deck" />
        </div>
      </button>
      <div className="zone discard-pile" id="discard-pile" />

      {/* every player rectangle ↓ (use an array to keep it DRY) */}
      {(["p1", "p2", "p3", "p4", "p5"] as PlayerKey[]).map((pk) => (
        <section key={pk} className={`player-board ${pk}`}>
          <div className="property-slot" id={`${pk}-properties`} />
          <div className="bank-slot" id={`${pk}-bank`} />
          {/* Hand button ONLY if this board belongs to me */}
          {myPID === pidMap[pk] && (
            <button className="hand-toggle" onClick={openHand(pk)}>
              Hand
            </button>
          )}
        </section>
      ))}

      {/* show modal only for my hand */}
      {handModal &&
        pidMap[handModal] === myPID &&
        ReactDOM.createPortal(
          <div className="hand-modal" onClick={closeModal}>
            <div
              className="hand-modal-inner"
              onClick={(e) => e.stopPropagation()}
            >
              <h2>Your hand</h2>
              {
                <div className="card-holder">
                  <img src={drawPileImg} alt="Deck" />
                </div>
              }
              <button onClick={closeModal}>Close</button>
            </div>
          </div>,
          document.body
        )}
    </div>
  );
};

export default Playmat5;
