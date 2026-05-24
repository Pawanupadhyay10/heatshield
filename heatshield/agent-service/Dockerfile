FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace
COPY . .
RUN mvn clean package -pl agent-service -am -DskipTests -q
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/agent-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
