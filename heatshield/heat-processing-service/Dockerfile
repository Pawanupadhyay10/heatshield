FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace
RUN apk add --no-cache maven
COPY . .
RUN mvn clean package -pl heat-processing-service -am -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/heat-processing-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
