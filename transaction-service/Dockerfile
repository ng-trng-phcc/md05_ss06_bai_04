FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

RUN chmod +x gradlew \
    && ./gradlew bootJar --no-daemon --console=plain

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /workspace/build/libs/transaction-service.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
