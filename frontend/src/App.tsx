import React, { useState } from "react";
import { StartScreen } from "./pages/StartScreen";
import GameScreen from "./pages/Mainmenu";
import { AnimatePresence, motion } from "framer-motion";

function App() {
  const [started, setStarted] = useState(false);
  const [showSplash, setShowSplash] = useState(false);

  const handleStart = () => {
    setShowSplash(true);
    setTimeout(() => {
      setStarted(true);
    }, 2000); // splash duration
  };

  return (
    <AnimatePresence mode="wait">
      {!started && !showSplash && (
        <motion.div
          key="start"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
        >
          <StartScreen onStart={handleStart} />
        </motion.div>
      )}

      {showSplash && !started && (
        <motion.div
          key="splash"
          className="splash-screen"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
        >
          <h1>Loading Blockopoly...</h1>
        </motion.div>
      )}

      {started && (
        <motion.div
          key="game"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
        >
          <GameScreen />
        </motion.div>
      )}
    </AnimatePresence>
  );
}

export default App;
