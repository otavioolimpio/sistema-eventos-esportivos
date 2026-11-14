FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY servico-eventos/pom.xml ./pom.xml
COPY servico-eventos/src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]