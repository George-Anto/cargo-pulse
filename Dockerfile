# --- STAGE 1: BUILD PHASE ---
# Purpose: Compiles the source code into an executable JAR file.

# Use the official Maven image which bundles both Maven and JDK 21 on a small Alpine Linux base.
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

# Set the working directory inside the build container.
WORKDIR /app

# Copy project definition files (pom.xml) and source code (src) into the container.
COPY pom.xml .
COPY src ./src

# Execute the Maven build:
# - 'mvn clean install -DskipTests' compiles the code and packages it into a JAR.
# - '--mount' caches downloaded Maven dependencies in the host's /root/.m2, significantly speeding up subsequent builds.
RUN --mount=type=cache,target=/root/.m2 mvn clean install -DskipTests

# -----------------------------------------------------------------------

# --- STAGE 2: RUNTIME PHASE ---
# Purpose: Creates the final, small image containing only the JRE and the application JAR.

# Use a lightweight JRE base image (Temurin 21) for the final runtime environment.
FROM eclipse-temurin:21-jre-alpine

# Set non-root user for enhanced security (best practice).
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Set the main application directory.
WORKDIR /app

# Copy the built application JAR from the previous 'build' stage into the current image.
COPY --from=build /app/target/*.jar app.jar

# Inform Docker that the container will listen on the application's SSL port (8443).
EXPOSE 8443

# Define the command to run the application when the container starts.
# This executes the Java application using the single JAR file.
ENTRYPOINT ["java", "-jar", "app.jar"]