FROM maven:3.9.7-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests -e -B -Dmaven.wagon.http.retryHandler.count=3 || \
    mvn clean package -DskipTests -e -B -Dmaven.wagon.http.retryHandler.count=3 || \
    mvn clean package -DskipTests -e -B -Dmaven.wagon.http.retryHandler.count=3

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
