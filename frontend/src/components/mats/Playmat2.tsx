import React, { useState } from "react";
import { createPortal } from "react-dom";
import { useDroppable } from "@dnd-kit/core";

/* ------- images ------- */
import backdrop from "@/assets/Backdrop.svg";

import "@/components/mats/mat_styles/playmat_shared.css";
import "./mat_styles/Playmat2.css";

import { PLAYER_ID_KEY } from "../../constants/constants";

/** Keep this in sync with PlayScreen's type */
export type PlaymatProps = {
  layout: Partial<Record<"p1" | "p2", string>>; // seat -> playerId
  myPID: string;
  names: Record<string, string>;
  discardImages?: string[]; // newest last
};

type PlayerKey = "p1" | "p2";

const Playmat2: React.FC<PlaymatProps> = ({
  layout,
  myPID,
  names,
  discardImages = [],
}) => {
  const [openHandFor, setOpenHandFor] = useState<PlayerKey | null>(null);

  // pull the true pids from layout
  const pidMap: Record<PlayerKey, string> = {
    p1: layout.p1 ?? "",
    p2: layout.p2 ?? "",
  };

  const openHand = (pk: PlayerKey) => () => setOpenHandFor(pk);
  const closeHand = () => setOpenHandFor(null);

  // ---- dnd-kit droppable zones (per seat) ---------------------------
  const { setNodeRef: setP1BankRef, isOver: p1BankOver } = useDroppable({
    id: `bank:pid:${pidMap.p1}`,
  });
  const { setNodeRef: setP1PropsRef, isOver: p1PropsOver } = useDroppable({
    id: `collect:pid:${pidMap.p1}`,
  });

  const { setNodeRef: setP2BankRef, isOver: p2BankOver } = useDroppable({
    id: `bank:pid:${pidMap.p2}`,
  });
  const { setNodeRef: setP2PropsRef, isOver: p2PropsOver } = useDroppable({
    id: `collect:pid:${pidMap.p2}`,
  });

  const { setNodeRef: setDiscardRef, isOver: discardOver } = useDroppable({
    id: "discard",
  });

  // compute three visible discard images: 1 = oldest, 3 = newest
  const lastThree = discardImages.slice(-3);
  const [d1, d2, d3] = [
    lastThree[0] ?? null, // oldest of visible
    lastThree[1] ?? null,
    lastThree[2] ?? null, // newest
  ];

  const nameFor = (pid?: string) => (pid && names[pid]) || "Opponent";

  return (
    <div className="playing-mat-outline-2-players">
      {/* board backdrop */}
      <img className="backdrop" src={backdrop} alt="2-player backdrop" />

      <div className="mat-stage">
        {/* -------- Player 1 area -------------------------------------- */}
        <div className="player-1-space">
          <div
            className={`property-collection-zone droppable ${
              p1PropsOver ? "is-over" : ""
            }`}
          >
            <div
              className="property-collection"
              id="p1-properties"
              ref={setP1PropsRef}
              aria-label={`${nameFor(pidMap.p1)} property collection`}
            />
          </div>

          <div className="money-collection-bank">
            <div
              className={`bank-pile droppable ${p1BankOver ? "is-over" : ""}`}
              id="p1-bank"
              ref={setP1BankRef}
              aria-label={`${nameFor(pidMap.p1)} bank`}
            />
            {myPID === pidMap.p1 && (
              <button className="hand-toggle" onClick={openHand("p1")}>
                Hand
              </button>
            )}
          </div>
        </div>

        {/* -------- Player 2 area -------------------------------------- */}
        <div className="player-2-space">
          <div
            className={`player-2-property-collection-zone droppable ${
              p2PropsOver ? "is-over" : ""
            }`}
          >
            <div
              className="property-collection2"
              id="p2-properties"
              ref={setP2PropsRef}
              aria-label={`${nameFor(pidMap.p2)} property collection`}
            />
          </div>

          <div className="player-2-money-collection-bank">
            <div
              className={`bank-pile2 droppable ${p2BankOver ? "is-over" : ""}`}
              id="p2-bank"
              ref={setP2BankRef}
              aria-label={`${nameFor(pidMap.p2)} bank`}
            />
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
            <div className="deck" />
          </div>

          <div
            className={`discard-pile droppable ${discardOver ? "is-over" : ""}`}
            ref={setDiscardRef}
            aria-label="discard pile"
          >
            {/* dynamic discard thumbnails; remove placeholders */}
            {d1 && <img className="card-1" src={d1} alt="" aria-hidden />}
            {d2 && <img className="card-2" src={d2} alt="" aria-hidden />}
            {d3 && (
              <img className="card-3" src={d3} alt="Most recent discard" />
            )}
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
              {/* TODO – map real cards here if you want a modal hand view */}
              <button onClick={closeHand}>Close</button>
            </div>
          </div>,
          document.body
        )}
    </div>
  );
};

export default Playmat2;
