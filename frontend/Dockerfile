# Dockerfile
FROM node:20-bookworm

# Update and upgrade system packages to reduce vulnerabilities
RUN apt-get update && apt-get upgrade -y

# Set the working directory inside the container
WORKDIR /src

# Install global tools
RUN npm install -g vite

# mandatory: Install GUI dependencies (for Electron later)
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
  libxkbcommon0


CMD [ "bash" ]