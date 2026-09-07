FROM node:20-bookworm-slim AS build

WORKDIR /workspace/frontend
ARG VITE_TICKET_AUTH_MODE=DEMO
ARG VITE_TICKET_AUTH_ISSUER_URI=
ARG VITE_TICKET_AUTH_CLIENT_ID=
ARG VITE_TICKET_AUTH_AUDIENCE=
ARG VITE_TICKET_AUTH_SCOPE=openid profile email
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
ENV VITE_TICKET_API_BASE_URL=/api \
    VITE_DEMO_MODE=false \
    VITE_TICKET_AUTH_MODE=$VITE_TICKET_AUTH_MODE \
    VITE_TICKET_AUTH_ISSUER_URI=$VITE_TICKET_AUTH_ISSUER_URI \
    VITE_TICKET_AUTH_CLIENT_ID=$VITE_TICKET_AUTH_CLIENT_ID \
    VITE_TICKET_AUTH_AUDIENCE=$VITE_TICKET_AUTH_AUDIENCE \
    VITE_TICKET_AUTH_SCOPE=$VITE_TICKET_AUTH_SCOPE
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /workspace/frontend/dist /usr/share/nginx/html
COPY deploy/staging/nginx.conf.template /etc/nginx/templates/default.conf.template

EXPOSE 8080
