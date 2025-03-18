FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

COPY . .

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk
WORKDIR /app

COPY --from=builder /app/target/openlibrary-es-1.0.0.jar openlibrary-es.jar

ENTRYPOINT ["java", "-jar", "/app/openlibrary-es.jar"]
