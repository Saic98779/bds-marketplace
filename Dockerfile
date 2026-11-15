# ---------- Stage 1: build the jar with Maven (unit tests run here) ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Download dependencies first so they are cached until a pom.xml changes
COPY pom.xml .
COPY bds-backend/pom.xml bds-backend/
RUN mvn -B -q -pl bds-backend -am dependency:go-offline

COPY bds-backend/src bds-backend/src
RUN mvn -B -pl bds-backend -am package && cp bds-backend/target/bds-backend-*.jar app.jar

# ---------- Stage 2: small runtime image ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as a non-root user; /app/data holds uploaded documents and should be mounted as a volume
RUN groupadd --system app && useradd --system --gid app app \
    && mkdir -p /app/data && chown app:app /app/data
COPY --from=build /workspace/app.jar app.jar
USER app

EXPOSE 8080
VOLUME /app/data
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0" \
    BDS_STORAGE_ROOT=/app/data
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
