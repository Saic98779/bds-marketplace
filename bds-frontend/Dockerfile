# ---------- Stage 1: build the static app ----------
FROM node:20-alpine AS build
WORKDIR /app

# Install dependencies first so they are cached until package*.json changes
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY . .
RUN npm run build

# ---------- Stage 2: nginx serves the app and proxies /api to the backend ----------
FROM nginx:1.27-alpine

# The nginx image renders /etc/nginx/templates/*.template with these variables at start-up
ENV BDS_API_UPSTREAM=http://bds-backend:8080 \
    DNS_RESOLVER=127.0.0.11
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s CMD wget -q -O /dev/null http://127.0.0.1/healthz || exit 1
