# syntax=docker/dockerfile:1

# ---- Dependencies stage ----
# Cached separately from application code, same reasoning as the
# backend's Dockerfile: package.json/package-lock.json change far less
# often than src/, so this layer is reused on most rebuilds.
FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

# ---- Build stage ----
FROM node:22-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
# NEXT_PUBLIC_* values ARE baked into the build (Next.js inlines them at
# build time) - this is passed as a build arg, not hardcoded, so a real
# deployment supplies its real API URL at image-build time. See infra's
# docker-compose.prod.yml for how this gets wired.
ARG NEXT_PUBLIC_API_BASE_URL
ENV NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_BASE_URL
RUN npm run build

# ---- Runtime stage ----
FROM node:22-alpine AS runtime
WORKDIR /app

# Install wget for healthcheck
RUN apk add --no-cache wget

RUN addgroup -S docshare && adduser -S docshare -G docshare
USER docshare

# "standalone" output (see next.config.ts) contains only what's needed to
# run - a minimal node_modules subset plus the compiled server, not the
# full dependency tree from the build stage.
COPY --from=build --chown=docshare:docshare /app/.next/standalone ./
COPY --from=build --chown=docshare:docshare /app/.next/static ./.next/static
COPY --from=build --chown=docshare:docshare /app/public ./public

EXPOSE 3000

# Set hostname to bind to all interfaces (0.0.0.0) instead of container IP
ENV HOSTNAME=0.0.0.0

HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
  CMD wget -qO- http://localhost:3000 || exit 1

ENTRYPOINT ["node", "server.js"]
