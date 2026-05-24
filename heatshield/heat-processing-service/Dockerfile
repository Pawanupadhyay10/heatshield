FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
# Copy parent POM first
COPY pom.xml .
# Copy all service POMs for dependency resolution
COPY weather-ingestion-service/pom.xml weather-ingestion-service/
COPY heat-processing-service/pom.xml heat-processing-service/
COPY agent-service/pom.xml agent-service/
COPY routing-service/pom.xml routing-service/
COPY notification-service/pom.xml notification-service/
# Copy source of THIS service only
COPY heat-processing-service/src heat-processing-service/src
RUN apk add --no-cache maven && \
    mvn clean package -pl heat-processing-service -am -DskipTests -q
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/heat-processing-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
