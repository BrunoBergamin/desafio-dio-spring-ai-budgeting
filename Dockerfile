# ---------- 1. Frontend (React + Vite) ----------
FROM node:22-alpine AS frontend
WORKDIR /ui
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

# ---------- 2. Backend (Maven) ----------
# Quem for testar nao precisa de Java, Maven nem Node instalados: tudo acontece dentro da imagem.
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
# O build do React entra no jar como conteudo estatico: uma porta so, sem CORS
COPY --from=frontend /ui/dist ./src/main/resources/static
RUN mvn -B clean package -DskipTests

# ---------- 3. Imagem final ----------
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && useradd -r -u 1001 lumi
COPY --from=build /app/target/*.jar app.jar
USER 1001
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=45s --retries=5 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
