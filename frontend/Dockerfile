# Stage 1: Build the Vite production bundle
FROM node:20-alpine AS builder
WORKDIR /app

# Copy dependency manifests
COPY package.json package-lock.json ./

# Install exact dependencies
RUN npm ci

# Copy full frontend source code
COPY . .

# Build optimized static assets
RUN npm run build

# Stage 2: Serve static bundle via Nginx
FROM nginx:alpine

# Remove default nginx static assets
RUN rm -rf /usr/share/nginx/html/*

# Copy built bundle from stage 1
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy custom Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget -qO- http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
