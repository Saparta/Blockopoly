/*  src/pages/MainMenu.tsx  */
import React, { useState, useEffect, useRef } from "react";
import "../style/Mainmenu.css";
import FallingBricks from "../components/FallingBricks";
import { useNavigate } from "react-router-dom";

const API = import.meta.env.VITE_API_BASE ?? "http://localhost:8080";

const MainMenu: React.FC = () => {
  /* state */
  const [name, setName] = useState("");
  const [codeInput, setCodeInput] = useState("");
  const [error, setError] = useState("");
  const navigate = useNavigate();

  const esRef = useRef<EventSource | null>(null);
  const navigatedRef = useRef(false);

  const isValidName = name.trim().length > 0 && name.trim().length <= 28;
  const isValidCode = /^[A-Z0-9]{6}$/.test(codeInput);

  /* navigate once helper */
  const goLobby = (code: string) => {
    if (navigatedRef.current) return;
    navigatedRef.current = true;
    localStorage.setItem("name", name.trim());
    console.log("[NAV] → /lobby/" + code);
    navigate(`/lobby/${code}`, { state: { name: name.trim() } });
  };

  /* open SSE on a given URL */
  const openStream = (url: string) => {
    esRef.current?.close();
    console.log("[SSE] open", url);

    const es = new EventSource(url);
    esRef.current = es;

    es.addEventListener("open", () => console.log("[SSE] connected"));

    /* ① MAIN handler — named INITIAL event */
    es.addEventListener("INITIAL", (ev) => {
      console.log("[SSE] INITIAL", ev.data);

      let payload: { playerID?: string; roomCode?: string } = {};
      try {
        payload = JSON.parse(ev.data);
      } catch {
        console.warn("[SSE] INITIAL not JSON"); // shouldn’t happen
      }

      if (payload.playerID) {
        sessionStorage.setItem("blockopolyPID", payload.playerID);
      }

      if (payload.roomCode) {
        goLobby(payload.roomCode); // 🔑 navigate
      }
    });

    es.onerror = () => {
      console.error("[SSE] error");
      es.close();
      setError("Lost connection to server.");
    };
  };

  /* JOIN */
  const handleJoin = () => {
    setError("");
    if (!isValidName || !isValidCode) {
      setError("Enter a name and 6-character room code.");
      return;
    }
    const url = `${API}/joinRoom/${codeInput}/${encodeURIComponent(
      name.trim()
    )}`;
    openStream(url);
  };

  /* CREATE */
  const handleCreate = () => {
    setError("");
    if (!isValidName) {
      setError("Please enter a name.");
      return;
    }
    const url = `${API}/createRoom/${encodeURIComponent(name.trim())}`;
    openStream(url);
  };

  /* cleanup */
  useEffect(() => () => esRef.current?.close(), []);

  /* UI */
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
          placeholder="Room Code (6 characters)"
          value={codeInput}
          maxLength={6}
          onChange={(e) => setCodeInput(e.target.value.toUpperCase())}
        />

        {error && <div className="error-message">{error}</div>}

        <div className="button-row">
          <button
            className="primary-button"
            onClick={handleJoin}
            disabled={!isValidName || !isValidCode}
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
