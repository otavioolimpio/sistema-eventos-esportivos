# Build a partir da RAIZ do repositorio (o pom pai fica la):
#   docker build -f servico-usuarios-equipes/Dockerfile .
# ou, na raiz: docker compose up --build

# Etapa 1: compila o modulo com Maven
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# pom pai + pom do modulo primeiro, para o Docker reaproveitar o cache das dependencias
COPY pom.xml ./pom.xml
COPY servico-usuarios-equipes/pom.xml ./servico-usuarios-equipes/pom.xml
# -f aponta direto para o modulo: o Maven le o pom pai pelo relativePath
# sem exigir que os outros modulos listados nele estejam na imagem
RUN mvn -q -B -f servico-usuarios-equipes/pom.xml dependency:go-offline
COPY servico-usuarios-equipes/src ./servico-usuarios-equipes/src
RUN mvn -q -B -f servico-usuarios-equipes/pom.xml package -DskipTests

# Etapa 2: imagem final, so com o Java e o .jar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/servico-usuarios-equipes/target/servico-usuarios-equipes-*.jar app.jar

# O arquivo do SQLite fica em /data, que o docker-compose monta como volume
RUN mkdir -p /data
ENV USUARIOS_DB=/data/usuarios_equipes.db

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
