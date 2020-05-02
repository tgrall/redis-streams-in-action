  
# build stage
FROM node:lts-alpine as build-stage
WORKDIR /app
COPY *.json ./
RUN npm install
COPY . .

EXPOSE 3000
CMD ["node", "server.js", "--CONF_FILE", "./config.prod.json" ]

