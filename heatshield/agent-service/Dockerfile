FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY weather-ingestion-service/pom.xml weather-ingestion-service/
COPY heat-processing-service/pom.xml heat-processing-service/
COPY agent-service/pom.xml agent-service/
COPY routing-service/pom.xml routing-service/
COPY notification-service/pom.xml notification-service/
COPY agent-service/src agent-service/src
RUN apk add --no-cache maven && \
    mvn clean package -pl agent-service -am -DskipTests -q
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/agent-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
