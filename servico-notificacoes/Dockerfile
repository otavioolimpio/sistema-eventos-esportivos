# Build a partir da RAIZ do repositorio (o pom pai fica la):
#   docker build -f servico-notificacoes/Dockerfile .
# ou, de dentro desta pasta: docker compose up --build

# Etapa 1: compila o modulo com Maven
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# pom pai + pom do modulo primeiro, para o Docker reaproveitar o cache das dependencias
COPY pom.xml ./pom.xml
COPY servico-notificacoes/pom.xml ./servico-notificacoes/pom.xml
# -f aponta direto para o modulo: o Maven le o pom pai pelo relativePath
# sem exigir que os outros modulos listados nele ja existam
RUN mvn -q -B -f servico-notificacoes/pom.xml dependency:go-offline
COPY servico-notificacoes/src ./servico-notificacoes/src
RUN mvn -q -B -f servico-notificacoes/pom.xml package -DskipTests

# Etapa 2: imagem final, so com o Java e o .jar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/servico-notificacoes/target/servico-notificacoes-*.jar app.jar

# O arquivo do SQLite fica em /data, que o docker-compose monta como volume
RUN mkdir -p /data
ENV NOTIFICACAO_DB=/data/notificacoes.db

EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
