FROM node:20-bookworm

# Set working directory
WORKDIR /frontend

# Copy package.json files first
COPY package*.json ./

# Install Node dependencies
RUN npm install

# Install GUI dependencies (for Electron)
RUN apt-get update && apt-get install -y \
  dumb-init \
  libx11-dev \
  libxext-dev \
  libxrender-dev \
  libxtst-dev \
  libnss3 \
  libatk-bridge2.0-0 \
  libgtk-3-0 \
  libgbm1 \
  libasound2 \
  libxshmfence1 \
  libxcomposite1 \
  libxdamage1 \
  libxrandr2 \
  libxfixes3 \
  libxkbcommon0 \
  xvfb

# Copy ALL app source code, including folders like src/assets/
COPY . .


# Expose port for Vite
EXPOSE 5173

# Start the dev server (with GUI for Electron)
# container runs web-only script
CMD ["npm", "run", "dev:web"]