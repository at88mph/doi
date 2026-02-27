# Build Stage
FROM node:22-alpine AS builder

WORKDIR /app

# Build arguments for subpath deployment
# Set NEXT_PUBLIC_BASE_PATH to deploy on a subpath (e.g., /rafts)
ARG NEXT_PUBLIC_BASE_PATH=""

# Cloudflare Turnstile site key (for bot protection)
ARG NEXT_PUBLIC_TURNSTILE_SITE_KEY=""

# Copy package files
COPY package*.json ./

# Temporarily remove the prepare script to prevent husky errors during install
RUN npm pkg delete scripts.prepare && \
    npm ci --ignore-scripts

# Copy source code
COPY . .

# Build the Next.js application
# IMPORTANT: NEXT_PUBLIC_BASE_PATH must be set at BUILD time for subpath routing
ENV NEXT_TELEMETRY_DISABLED=1
ENV NODE_ENV=production
ENV NEXTAUTH_DEBUG=true
ENV NEXT_PUBLIC_BASE_PATH=${NEXT_PUBLIC_BASE_PATH}
ENV NEXT_PUBLIC_TURNSTILE_SITE_KEY=${NEXT_PUBLIC_TURNSTILE_SITE_KEY}

RUN npm run build

# Production Stage
FROM node:22-alpine AS runner

WORKDIR /app

# Install dumb-init for proper signal handling
RUN apk add --no-cache dumb-init

# Create non-root user
RUN addgroup -g 1001 -S nextjs && \
    adduser -S nextjs -u 1001 -G nextjs

# Copy built application
COPY --from=builder /app/public ./public
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static

# Set environment variables
ENV NODE_ENV=production
ENV NEXTAUTH_DEBUG=true
ENV UI_REVIEW_ENABLED=true
ENV PORT=8080
ENV HOSTNAME="0.0.0.0"

# Change ownership to non-root user
RUN chown -R nextjs:nextjs /app

USER nextjs

EXPOSE 8080

# Use dumb-init to handle signals properly
ENTRYPOINT ["dumb-init", "--"]
CMD ["node", "server.js"]