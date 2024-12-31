# Use OpenJDK to run the Spring Boot application
FROM openjdk:17-jdk-slim

# Set the working directory
WORKDIR /app

# Define an argument for the JAR file location
ARG JAR_FILE=target/mes-qc-service-0.0.1-SNAPSHOT.jar

# Copy the JAR file into the container
COPY ${JAR_FILE} app.jar

# Default environment variables (can be overridden at runtime)
ENV SPRING_PROFILES_ACTIVE=local
ENV SERVER_PORT=8086

# Expose the application port (default: 8086)
EXPOSE ${SERVER_PORT}

# Run the application with the active profile and port
ENTRYPOINT ["java", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-Dserver.port=${SERVER_PORT}", "-jar", "app.jar"]
