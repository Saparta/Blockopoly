# syntax=docker/dockerfile:1

############################
# ---- 1. Build stage ---- #
############################
FROM gradle:8.7-jdk21 AS builder
WORKDIR /home/ktor

# ----- copy build scripts first (enables layer-caching of dependencies) -----
COPY gradlew gradlew.bat gradle/ ./
COPY settings.gradle.kts build.gradle.kts ./

RUN ./gradlew --no-daemon dependencies || true

# ----- now copy the rest of the source and build -----
COPY . .
RUN ./gradlew --no-daemon shadowJar                 # produces build/libs/<name>-all.jar


#############################
# ---- 2. Runtime stage --- #
#############################
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Grab the shaded JAR from the previous stage
COPY --from=builder /home/ktor/build/libs/*-all.jar app.jar

# Run as an unprivileged user (optional but recommended)
RUN addgroup -S ktor && adduser -S ktor -G ktor
USER ktor

EXPOSE 8080
ENV JAVA_OPTS="-Xms128m -Xmx256m"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]

