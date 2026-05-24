FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace
# Copy entire repo for parent POM access
COPY . .
RUN mvn clean package -pl weather-ingestion-service -am -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/weather-ingestion-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
