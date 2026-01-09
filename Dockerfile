# ===============================
# 1️⃣ Build Stage
# ===============================
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom and source
COPY pom.xml .
COPY src src

# Build jar
RUN mvn clean package -DskipTests

# ===============================
# 2️⃣ Runtime Stage
# ===============================
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
