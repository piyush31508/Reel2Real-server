# ===============================
# 1️⃣ Build Stage
# ===============================
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src src

# Build jar
RUN ./mvnw clean package -DskipTests

# ===============================
# 2️⃣ Runtime Stage
# ===============================
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Render provides PORT dynamically
EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
