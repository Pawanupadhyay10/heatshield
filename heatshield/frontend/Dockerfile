FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
ARG VITE_MAPBOX_TOKEN
ARG VITE_MS2_BASE
ARG VITE_MS3_BASE
ARG VITE_MS4_BASE
ARG VITE_MS5_BASE
RUN npm run build
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
