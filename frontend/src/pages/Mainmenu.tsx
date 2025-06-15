import React, { useState } from "react";
import "../style/Mainmenu.css";
import FallingBricks from "../components/FallingBricks";
import { useNavigate } from "react-router-dom";

const API = import.meta.env.VITE_API_BASE ?? "http://localhost:8080";

const MainMenu: React.FC = () => {
  const [name, setName] = useState("");
  const [roomPin, setRoomPin] = useState("");
  const [error, setError] = useState("");
  const navigate = useNavigate();

  // validation
  const isValidName = name.trim().length > 0 && name.trim().length <= 28;
  const isValidPin = /^[A-Z0-9]{6}$/.test(roomPin);

  // ───── JOIN ROOM ─────
  const handleJoin = async () => {
    setError("");

    if (!isValidName || !isValidPin) {
      setError("Enter a name and 6-char pin.");
      return;
    }

    try {
      const res = await fetch(`${API}/joinRoom/${roomPin}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: name.trim() }),
      });

      if (!res.ok) {
        setError("Room not found or server error.");
      } else {
        console.log("Joined room!");
        navigate(`/lobby/${roomPin}`);
        // Transition to game room here
      }
    } catch (err) {
      setError("Failed to connect to server.");
    }
  };

  // ───── CREATE ROOM ─────
  const handleCreate = async () => {
    setError("");

    if (!isValidName) {
      setError("Please enter a name.");
      return;
    }

    try {
      const res = await fetch(`${API}/create-room`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: name.trim() }),
      });

      if (!res.ok) {
        setError("Unable to create room.");
        return;
      }
      console.log("Room created!");
      // TODO: navigate to new room lobby
    } catch {
      setError("Backend unreachable.");
    }
  };

  // ───── UI ─────
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
          onChange={(e) => setRoomPin(e.target.value.toUpperCase())}
        />

        {error && <div className="error-message">{error}</div>}

        <div className="button-row">
          <button
            className="primary-button"
            onClick={handleJoin}
            disabled={!isValidName || !isValidPin}
          >
            Join Room
          </button>
          <button
            className="secondary-button"
            onClick={handleCreate}
            disabled={!isValidName}
          >
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
