FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY servico-inscricoes/pom.xml ./pom.xml
COPY servico-inscricoes/src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]