#!/bin/bash

# Configuration
APP_NAME="flex-gateway"
IMAGE_NAME="px-gateway:staging"
PORT=8080

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}🚀 Starting Deployment...${NC}"

# 1. Pull changes
#git pull origin master

# 2. Build the JAR file locally
# This generates the file in target/ for the Dockerfile to pick up
echo -e "${GREEN}📦 Building JAR with Maven...${NC}"
#mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Maven build failed! Check your Java code.${NC}"
    exit 1
fi

# 3. Build Docker Image
echo -e "${GREEN}🔨 Building Docker image...${NC}"
docker build -t $IMAGE_NAME .

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Docker build failed!${NC}"
    exit 1
fi

# 4. Cleanup & Restart
echo -e "${GREEN}♻️ Restarting container...${NC}"
docker stop $APP_NAME 2>/dev/null || true
docker rm $APP_NAME 2>/dev/null || true

docker run -d \
  -p $PORT:8080 \
  --name $APP_NAME \
  --restart unless-stopped \
  --add-host=host.docker.internal:host-gateway \
  -e SPRING_DATA_MONGODB_URI="mongodb://host.docker.internal:27017/px-gateway" \
  -e SPRING_MONGODB_URI="mongodb://host.docker.internal:27017/px-gateway" \
  $IMAGE_NAME

echo -e "${GREEN}✅ Deployment Complete!${NC}"
docker logs -f $APP_NAME
