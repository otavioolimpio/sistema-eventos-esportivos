# Build a partir da RAIZ do repositorio (o pom pai fica la):
#   docker build -f service-discovery/Dockerfile .
# ou, na raiz: docker compose up --build

# Etapa 1: compila o modulo com Maven
# --mount=type=cache: o repositorio local do Maven (/root/.m2) fica num cache do
# BuildKit compartilhado entre os modulos, para nao baixar tudo de novo a cada build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml ./pom.xml
COPY service-discovery/pom.xml ./service-discovery/pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -f service-discovery/pom.xml dependency:go-offline
COPY service-discovery/src ./service-discovery/src
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -f service-discovery/pom.xml package -DskipTests

# Etapa 2: imagem final, so com o Java e o .jar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/service-discovery/target/service-discovery-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
