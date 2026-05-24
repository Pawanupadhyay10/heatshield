FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace
COPY . .
RUN mvn clean package -pl routing-service -am -DskipTests -q
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/routing-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
