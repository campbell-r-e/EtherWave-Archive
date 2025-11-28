# Multi-stage build for Angular frontend
FROM node:20-alpine AS build

WORKDIR /app

# Copy package files and install dependencies
COPY package*.json ./
RUN npm ci

# Copy source and build for production
COPY . .
RUN npm run build --prod

# Runtime stage with nginx
FROM nginx:alpine

# Copy built app to nginx
COPY --from=build /app/dist/logbook-ui/browser /usr/share/nginx/html

# Copy custom nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
