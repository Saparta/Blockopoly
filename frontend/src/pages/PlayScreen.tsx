/* src/pages/PlayScreen.tsx -------------------------------------------- */
import React from "react";
import { PLAYERS_KEY } from "../constants"; // PLAYERS_KEY === "playersInRoom"
// import card from "../assets/cards/card-back.svg";
import mat2 from "../assets/play_mats/playmat_2.svg";
import mat3 from "../assets/play_mats/playmat_3.svg";
import mat4 from "../assets/play_mats/playmat_4.svg";
import mat5 from "../assets/play_mats/playmat_5.svg";

// const LOBBY_API = import.meta.env.room_service ?? "http://localhost:8080";
// const GAME_API = import.meta.env.game_service ?? "http://localhost:8081";

const boardFor = (count: number): string => {
  switch (count) {
    case 2:
      return mat2;
    case 3:
      return mat3;
    case 4:
      return mat4;
    default:
      return mat5; // fall-back / 5-player
  }
};

const PlayScreen: React.FC = () => {
  const raw = sessionStorage.getItem(PLAYERS_KEY) ?? "[]";
  const list = JSON.parse(raw) as unknown[];
  const count = Array.isArray(list) ? list.length : 2;

  const boardSrc = boardFor(count);

  return (
    <div className="play-wrapper">
      <img
        className="playing-board"
        src={boardSrc}
        alt={`${count}-player board`}
      />
    </div>
  );
};

export default PlayScreen;
