# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copia o POM pai da raiz e o POM do microserviço
COPY pom.xml ./pom.xml
COPY servico-usuarios-equipes/pom.xml ./servico-usuarios-equipes/pom.xml
COPY servico-usuarios-equipes/src ./servico-usuarios-equipes/src

# Compila apenas o microservico de usuarios e equipes
RUN mvn clean package -pl servico-usuarios-equipes -am -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copia o JAR gerado no estágio de build
COPY --from=build /app/servico-usuarios-equipes/target/*.jar app.jar

# Porta exposta pelo microservico
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]