/* src/pages/PlayScreen.tsx -------------------------------------------- */
import { PLAYERS_KEY } from "../constants/constants"; // PLAYERS_KEY === "playersInRoom"
import "../style/PlayScreen.css";
// import card from "../assets/cards/card-back.svg";
// import mat2 from "../assets/play_mats/playmat_2.svg";
// import mat3 from "../assets/play_mats/playmat_3.svg";
// import mat4 from "../assets/play_mats/playmat_4.svg";
// import mat5 from "../assets/play_mats/playmat_5.svg";
// import playmat2 from "../components/mats/Playmat2";
// import playmat3 from "../components/mats/Playmat3";
// import playmat4 from "../components/mats/Playmat4";
// import playmat5 from "../components/mats/Playmat5";
import { lazy } from "react";
const playmat2 = lazy(() => import("../components/mats/Playmat2"));
const playmat3 = lazy(() => import("../components/mats/Playmat3"));
const playmat4 = lazy(() => import("../components/mats/Playmat4"));
const playmat5 = lazy(() => import("../components/mats/Playmat5"));

// const LOBBY_API = import.meta.env.room_service ?? "http://localhost:8080";
// const GAME_API = import.meta.env.game_service ?? "http://localhost:8081";

const boardFor = (count: number) => {
  switch (count) {
    case 2:
      return playmat2;
    case 3:
      return playmat3;
    case 4:
      return playmat4;
    default:
      return playmat5; // fall-back / 5-player
  }
};

const PlayScreen: React.FC = () => {
  const raw = sessionStorage.getItem(PLAYERS_KEY) ?? "[]";
  const list = JSON.parse(raw) as unknown[];
  const count = Array.isArray(list) ? list.length : 2;
  const Mat = boardFor(count);

  // const boardSrc = boardFor(count);

  return (
    /* <img
        className="playing-board"
        src={boardSrc}
        alt={`${count}-player board`}
      /> */

    <div className="board-shell">
      <Mat />
      {/* overlay UI goes here */}
    </div>
  );
};

export default PlayScreen;
