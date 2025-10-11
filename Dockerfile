# ----- Stage 1: Build -----
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Copy toàn bộ source code
COPY . .

# Build project (tạo file .jar)
RUN ./mvnw clean package -DskipTests

# ----- Stage 2: Run -----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy file .jar từ stage build
COPY --from=build /app/target/*.jar app.jar

# Expose port cho app
EXPOSE 8080

# Run app
ENTRYPOINT ["java", "-jar", "app.jar"]
