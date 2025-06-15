import React from "react";
import { BrowserRouter as Router, Routes, Route, useNavigate } from "react-router-dom";
import { StartScreen } from "./pages/StartScreen";
import GameScreen from "./pages/Mainmenu";
import Lobby from "./pages/Lobby";
import { AnimatePresence, motion } from "framer-motion";

const AnimatedRoute = ({ children }) => (
  <motion.div
    initial={{ opacity: 0 }}
    animate={{ opacity: 1 }}
    exit={{ opacity: 0 }}
    key={location.pathname}
  >
    {children}
  </motion.div>
);

function App() {
  return (
    <Router>
      <AnimatePresence mode="wait">
        <Routes>
          <Route
            path="/"
            element={
              <AnimatedRoute>
                <StartScreen />
              </AnimatedRoute>
            }
          />
          <Route
            path="/main"
            element={
              <AnimatedRoute>
                <GameScreen />
              </AnimatedRoute>
            }
          />
          <Route
            path="/lobby/:roomPin"
            element={
              <AnimatedRoute>
                <Lobby />
              </AnimatedRoute>
            }
          />
        </Routes>
      </AnimatePresence>
    </Router>
  );
}

export default App;