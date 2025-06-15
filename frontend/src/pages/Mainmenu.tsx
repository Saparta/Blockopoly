import React, { useState } from "react";
import "../style/Mainmenu.css";
import FallingBricks from "../components/FallingBricks";

const MainMenu = () => {
  const [name, setName] = useState("");
  const [roomPin, setRoomPin] = useState("");
  const [error, setError] = useState("");

  const isValidName = name.trim().length > 0 && name.trim().length <= 28;
  const isValidPin = /^[a-zA-Z0-9]{6}$/.test(roomPin);

  const handleJoin = async () => {
    if (!isValidName || !isValidPin) {
      setError("Please enter a valid name and 6-character room pin.");
      return;
    }

    try {
      const res = await fetch("http://localhost:8080/join-room", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name, roomPin }),
      });

      if (!res.ok) {
        setError("Room not found or server error.");
      } else {
        console.log("Joined room!");
        // Transition to game room here
      }
    } catch (err) {
      setError("Failed to connect to server.");
    }
  };

  const handleCreate = async () => {
    if (!isValidName) {
      setError("Please enter a valid name.");
      return;
    }

    try {
      const res = await fetch("http://localhost:8080/create-room", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name }),
      });

      if (!res.ok) {
        setError("Failed to create room.");
      } else {
        console.log("Room created!");
        // Transition to room lobby here
      }
    } catch (err) {
      setError("Server unavailable.");
    }
  };

  return (
    <div className="main-menu">
      <div className="form-container">
        <h2>Welcome to Blockopoly</h2>

        <input
          className="name-input"
          placeholder="Enter your name"
          value={name}
          maxLength={28}
          onChange={(e) => setName(e.target.value)}
        />

        <input
          className="roompin-input"
          placeholder="Room Pin (6 characters)"
          value={roomPin}
          maxLength={6}
          onChange={(e) => setRoomPin(e.target.value)}
        />

        {error && <div className="error-message">{error}</div>}

        <div className="button-row">
          <button className="primary-button" onClick={handleJoin}>
            Join Room
          </button>
          <button className="secondary-button" onClick={handleCreate}>
            Create Room
          </button>
        </div>
      </div>
      <div className="falling-bricks-wrapper">
        <FallingBricks />
      </div>
    </div>
  );
};

export default MainMenu;
