# Stage 1: Build the Angular application
FROM node:20-alpine AS build

WORKDIR /build

# Copy package files first for dependency caching
COPY package.json package-lock.json ./
RUN npm ci

# Copy application source and config files
COPY angular.json tsconfig.json tsconfig.app.json ./
COPY src/ src/

# Build for production (AOT, minified, tree-shaken)
RUN npm run build -- --configuration production

# Stage 2: Serve with nginx
FROM nginx:stable-alpine

# Copy built assets from the build stage
COPY --from=build /build/dist/admin-site/ /usr/share/nginx/html/

# Copy nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy entrypoint script for runtime API_BASE_URL injection
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

# Expose port 80
EXPOSE 80

# Use entrypoint for runtime configuration
ENTRYPOINT ["/docker-entrypoint.sh"]

# Health check
HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD wget -qO- http://localhost:80/health || exit 1
