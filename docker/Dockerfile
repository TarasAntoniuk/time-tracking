# Dockerfile for Time Tracking API
# This uses pre-built JAR from GitHub Actions

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy JAR file (will be copied by GitHub Actions)
COPY app/app.jar app.jar

# Expose port
EXPOSE 8081

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]