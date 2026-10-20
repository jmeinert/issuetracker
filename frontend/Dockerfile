FROM node:24.22.0-alpine AS build

WORKDIR /build

COPY package.json package-lock.json ./

RUN --mount=type=cache,target=/root/.npm npm ci

COPY . .

RUN npm run build


FROM nginxinc/nginx-unprivileged:stable-alpine

COPY --from=build /build/dist /usr/share/nginx/html

EXPOSE 8080 8443
