# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
# Скачать зависимости заранее (кешируется Docker'ом)
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
# Контейнер не имеет реального IPv6-маршрута (только loopback), а DNS для
# внешних хостов (например fcm.googleapis.com) иногда отдаёт только AAAA-запись —
# JVM пытается идти по ней и падает с "Failed to establish a connection".
# preferIPv4Stack заставляет JVM всегда использовать IPv4, которым контейнер
# реально может пользоваться.
ENTRYPOINT ["java", "-Djava.net.preferIPv4Stack=true", "-jar", "app.jar"]
