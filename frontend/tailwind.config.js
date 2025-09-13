/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx,css}", // covers your TSX pages/components and CSS (for @apply)
  ],
  theme: {
    extend: {
      // Preserve existing custom properties and add game-specific colors
      colors: {
        "game-blue": "#4a90e2",
        "game-gold": "#ffe58a",
        "game-dark": "#20242a",
      },
      // Ensure existing rem-based spacing works
      spacing: {
        18: "4.5rem",
        25: "6.25rem",
        58: "14.5rem",
        77: "19.25rem",
        90: "22.5rem",
      },
    },
  },
  plugins: [],
  // Ensure Tailwind doesn't conflict with existing CSS
  corePlugins: {
    preflight: false, // Disable Tailwind's base styles to avoid conflicts
  },
};
