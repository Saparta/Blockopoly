# ---- Builder ----
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy wrapper + gradle first (for caching)
COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle.kts build.gradle.kts ./

# Normalize + make executable (first pass)
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Optional: warm Gradle cache
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon dependencies || true

# Copy the rest of the source (this may overwrite gradlew)
COPY . .

RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Build fat jar (fail fast)
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon clean shadowJar
RUN ls -la build/libs

# ---- Runtime ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*-all.jar /app/app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
