#!/bin/sh
set -e

# Default API base URL if not provided
API_BASE_URL="${API_BASE_URL:-https://tfg-api.locknet.com.ar}"

# Replace the placeholder in compiled JS files with the actual API URL
find /usr/share/nginx/html -type f -name '*.js' -exec sed -i "s|__API_BASE_URL__|${API_BASE_URL}|g" {} +

# Start nginx
exec nginx -g 'daemon off;'
