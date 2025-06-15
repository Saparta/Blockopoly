import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import FallingBricks from "../components/FallingBricks";
import "../style/Lobby.css";

const Lobby = () => {
  const navigate = useNavigate();

  const { roomPin } = useParams();
  const [players, setPlayers] = useState([]);
  const [host, setHost] = useState("");
  const [isFull, setIsFull] = useState(false);

  const currentUser = localStorage.getItem("name");
  const isHost = currentUser === host;

  const handleLeaveRoom = () => {
    navigate("/main");
  };

  useEffect(() => {
    const interval = setInterval(() => {
      fetch(`http://localhost:8080/room/${roomPin}/players`)
        .then((res) => res.json())
        .then((data) => {
          setPlayers(data.players);
          setHost(data.host);
          setIsFull(data.players.length >= 5);
        })
        .catch(console.error);
    }, 1000);
    return () => clearInterval(interval);
  }, [roomPin]);

  const handleStartGame = () => {
    navigate("/start")
    console.log("Game starting...");
  };

  return (
    <div className="lobby-wrapper">
      <div className="falling-bricks-wrapper">
        <FallingBricks />
      </div>
      <div className="lobby">
        <h2>Join Code: {roomPin}</h2>
        <p>{players.length}/5 players</p>

        <h3>Players:</h3>
        <ol className="player-list">
          {players.map((player, i) => (
            <li key={i} className="player-slot">
              {player.name === host ? "👑 " : ""}
              {player.name}
              {player.name === host && <span className="host-label"> (Host)</span>}
            </li>
          ))}
        </ol>


        <div className="button-row">
          <button className="leave-button" onClick={handleLeaveRoom}>
            Leave Room
          </button>

          {isHost && (
            <button className="start-button" onClick={handleStartGame}>
              Start Game
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

export default Lobby;
