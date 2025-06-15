import React from "react";
import FallingBricks from "../components/FallingBricks";
import logo from "../assets/blockopoly-logo.svg";
import { PrimaryButton } from "../components/startbutton";
import clickSound from "../assets/click.mp3";
import "../style/StartScreen.css";

type Props = {
  onStart: () => void;
};

export const StartScreen: React.FC<Props> = ({ onStart }) => {
  const handleClick = () => {
    new Audio(clickSound).play();
    setTimeout(() => {
      onStart();
    }, 600);
  };

  return (
    <div className="start-screen">
      <div className="falling-bricks-wrapper">
        <FallingBricks />
      </div>

      <div className="div">
        <img className="blockopoly-logo" alt="Blockopoly logo" src={logo} />
        <PrimaryButton onClick={handleClick} />
      </div>
    </div>
  );
};
