#!/bin/sh
set -e

# Default only when API_BASE_URL is UNSET. Note "-" (not ":-") so an explicit
# empty string is preserved: "" means "same origin" behind the ALB on EKS.
API_BASE_URL="${API_BASE_URL-http://localhost:8080}"
envsubst '${API_BASE_URL}' < /usr/share/nginx/html/config.template.js > /usr/share/nginx/html/config.js

exec nginx -g "daemon off;"
