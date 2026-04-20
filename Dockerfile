# Stage 1: Build the application
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy maven wrapper and pom file
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Fix line endings for mvnw (if created on Windows) and make it executable
RUN tr -d '\r' < mvnw > mvnw_unix && mv mvnw_unix mvnw && chmod +x mvnw

# Download dependencies to cache them in a layer
RUN ./mvnw dependency:go-offline

# Copy source code and build the JAR
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime environment
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose your application ports
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]