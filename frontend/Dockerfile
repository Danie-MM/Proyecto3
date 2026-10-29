# Entorno de desarrollo: Node y npm quedan dentro de Docker.
FROM node:20.11.1-bookworm-slim@sha256:357deca6eb61149534d32faaf5e4b2e4fa3549c2be610ee1019bf340ea8c51ec AS dependencies

# Chromium permite ejecutar las pruebas de Angular sin instalar un navegador de pruebas en Windows.
RUN apt-get update \
    && apt-get install -y --no-install-recommends chromium ca-certificates \
    && rm -rf /var/lib/apt/lists/*

ENV NG_CLI_ANALYTICS=false \
    CHROME_BIN=/usr/bin/chromium \
    NPM_CONFIG_UPDATE_NOTIFIER=false
WORKDIR /app
RUN chown node:node /app
USER node

COPY --chown=node:node package.json package-lock.json .npmrc ./
RUN npm ci --no-audit --no-fund

FROM dependencies AS development
COPY --chown=node:node . .
RUN npm run build

EXPOSE 4200
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=12 \
    CMD node -e "fetch('http://127.0.0.1:4200/comprobar-entorno',{signal:AbortSignal.timeout(4000)}).then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"
CMD ["npm", "start"]
