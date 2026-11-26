# Stage 1: Build Spring Boot executable JAR using Maven Wrapper
FROM eclipse-temurin:27-jdk AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw -B dependency:go-offline

COPY src/ src/
COPY frontend/ frontend/
RUN ./mvnw -B -DskipTests package

# Stage 2: Lightweight JRE runtime
FROM eclipse-temurin:27-jre
WORKDIR /app
COPY --from=build /app/target/cartpulse-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
