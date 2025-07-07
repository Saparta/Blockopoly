// src/components/DiscardPile.tsx
import React, { useState } from "react";
import deckPileImg from "@/assets/cards/card-back.svg"; // placeholder back
import type { CSSProperties } from "react";

const MAX_HISTORY = 3;
const OFFSET = 8; // px shift per card

export const DiscardPile: React.FC = () => {
  // holds the card-front URLs as strings
  const [history, setHistory] = useState<string[]>([]);

  // sample function: call this when a real card is played
  const addToDiscard = (cardSrc: string) => {
    setHistory((prev) => {
      const next = [...prev, cardSrc];
      // keep only the last MAX_HISTORY cards
      return next.slice(-MAX_HISTORY);
    });
  };

  // for demo: add a back‐of‐deck card on click
  const demoDraw = () => addToDiscard(deckPileImg);

  return (
    <div className="zone discard-pile" id="discard-pile">
      {history.map((src, idx) => {
        // idx: 0 = oldest, last = most recent
        // compute how far from the top of the pile this card is
        const offset = (history.length - 1 - idx) * OFFSET;
        const style: CSSProperties = {
          position: "absolute",
          top: `${offset}px`,
          left: `${offset}px`,
          zIndex: idx,
          width: "100%",
          height: "auto",
        };
        return <img key={idx} src={src} alt="" style={style} />;
      })}

      {/* DEMO: remove in real game */}
      <button
        onClick={demoDraw}
        style={{
          position: "absolute",
          bottom: "-2rem",
          left: 0,
          padding: "4px 8px",
          fontSize: "0.8rem",
        }}
      >
        +Discard
      </button>
    </div>
  );
};
