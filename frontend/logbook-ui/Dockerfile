# Multi-stage build for Angular frontend
FROM node:22-alpine AS build

RUN npm install -g npm@latest

WORKDIR /app

# Copy package files and install dependencies
COPY package*.json ./
RUN npm ci --prefer-offline

# Copy source and build for production
COPY . .
RUN npm run build

# Runtime stage with nginx
FROM nginx:stable-alpine

# Copy built app to nginx
COPY --from=build /app/dist/logbook-ui/browser /usr/share/nginx/html

# Copy custom nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
