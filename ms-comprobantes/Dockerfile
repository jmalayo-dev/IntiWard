# syntax=docker/dockerfile:1
# Se construye desde la raíz del repo: docker build -f ms-comprobantes/Dockerfile .

# ---- Etapa 1: compilar ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
# La caché de ~/.m2 se comparte entre las 8 imágenes: solo la primera descarga dependencias
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -B -q -pl ms-comprobantes -am package -DskipTests

# ---- Etapa 2: correr ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/ms-comprobantes/target/*.jar app.jar
USER app
EXPOSE 8085
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=12 \
  CMD wget -qO- http://localhost:8085/actuator/health | grep -q UP || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
