#!/bin/bash
# A simple script to get the app running on a brand new Ubuntu/Oracle Linux server

echo "1. Installing Docker and Docker Compose..."
sudo apt-get update -y
sudo apt-get install -y docker.io docker-compose-v2
sudo systemctl enable docker
sudo systemctl start docker

echo "2. Setting up environment variables..."
if [ ! -f .env ]; then
    cp .env.example .env
    echo "Created .env from .env.example. Please edit .env with your actual DOMAIN and Google credentials."
fi

echo "3. Starting the Production Stack..."
sudo docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build

echo "Deployment complete! Your app should be live in ~2 minutes."
