# Stage 1: build frontend
FROM node:22-alpine AS frontend-build
WORKDIR /app
RUN corepack enable && corepack prepare pnpm@11.1.2 --activate
COPY frontend/package.json frontend/pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile
COPY frontend/ .
RUN pnpm run build

# Stage 2: build backend (embed pre-built frontend dist)
FROM eclipse-temurin:26-jdk AS backend-build
WORKDIR /app
COPY . .
COPY --from=frontend-build /app/dist frontend/dist
RUN ./gradlew bootJar -x buildFrontend -x test --no-daemon

# Stage 3: runtime
FROM eclipse-temurin:26-jre-jammy
WORKDIR /app
COPY --from=backend-build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
