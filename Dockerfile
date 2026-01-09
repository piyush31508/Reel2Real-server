# Use official Java image
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy jar file
COPY target/*.jar app.jar

# Expose port
EXPOSE 8000

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
