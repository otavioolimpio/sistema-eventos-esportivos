# Sistema de Gerenciamento de Eventos Esportivos (Microserviços)

Arquitetura distribuída baseada em microserviços para gestão de usuários, equipes, modalidades, eventos esportivos, inscrições e notificações por e-mail.

---

## Arquitetura e Serviços

| Serviço | Porta | Responsabilidade Principal | Casos de Uso |
| :--- | :---: | :--- | :--- |
| **`servico-usuarios-equipes`** | `8081` | Cadastro de usuários (`ORGANIZADOR`, `CHEFE_EQUIPE`, `JOGADOR`) e gestão de equipes | UC02, UC03 |
| **`servico-eventos`** | `8082` | Cadastro de modalidades esportivas, criação de eventos, controle de vagas e resultados | UC01, UC04 |
| **`servico-inscricoes`** | `8083` | Solicitação e avaliação de inscrições com integração síncrona via OpenFeign | UC05, UC06, UC07 |
| **`servico-notificacoes`** | `8084` | Disparo de notificações por e-mail desacoplado via SMTP | UC02, UC07 |
| **`mailpit`** | `8025` / `1025` | Servidor SMTP local e interface web (`http://localhost:8025`) para inspeção dos e-mails | Infraestrutura |

### Roadmap Arquitetural
- **`api-gateway` (`8080`)**: Ponto único de entrada e roteamento para os microserviços.
- **`servico-orquestrador` (`8085`)**: Orquestração de transações distribuídas (Saga) para fluxos complexos de inscrição e compensação.

---

## Tecnologias Utilizadas

- **Linguagem & Framework:** Java 21, Spring Boot 3.2.4
- **Comunicação entre Serviços:** Spring Cloud OpenFeign
- **Persistência:** Spring Data JPA + SQLite (banco isolado por microserviço)
- **Containerização:** Docker & Docker Compose (builds multi-stage com Maven 3.9 e Eclipse Temurin 21)
- **Testes de E-mail & E2E:** Mailpit + Scripts automatizados em PowerShell

---

## Como Executar o Projeto

### Pré-requisitos
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) em execução
- PowerShell (Windows) ou terminal equivalente

### 1. Subir os Containers

Na raiz do repositório (`sistema-eventos-esportivos`), execute:

```powershell
# Na primeira execução, recomenda-se subir em etapas para evitar concorrência no download do Maven:
docker compose up --build -d mailpit servico-notificacoes
docker compose up --build -d servico-usuarios-equipes
docker compose up --build -d servico-eventos servico-inscricoes
```

Nas execuções seguintes (com as imagens já construídas), basta rodar:

```powershell
docker compose up -d
```

### 2. Verificar Status dos Serviços

```powershell
docker compose ps
```

Todos os 5 containers (`mailpit`, `servico-notificacoes`, `servico-usuarios-equipes`, `servico-eventos` e `servico-inscricoes`) devem aparecer com status **`Up`**.

---

## Como Testar (Fluxo Ponta a Ponta)

Com os containers ativos, execute o script de integração completa que valida do **UC01 ao UC07** entre os 4 microserviços:

```powershell
powershell -ExecutionPolicy Bypass -File .\testar-fluxo-completo.ps1
```

O script executa automaticamente:
1. Criação de `ORGANIZADOR`, `CHEFE_EQUIPE` e `JOGADOR` (`8081` → `8084`).
2. Cadastro da modalidade esportiva (`8082` → `8081`).
3. Criação do evento com controle de vagas (`8082`).
4. Formação e vínculo da equipe (`8081`).
5. Solicitação de inscrição da equipe no evento (`8083` → `8081` e `8082`).
6. Aprovação da inscrição pelo organizador, decremento de vagas e disparo de e-mail (`8083` → `8082` e `8084`).
7. Registro e consulta do resultado final do evento (`8082`).

Para visualizar os e-mails enviados durante o teste, acesse **`http://localhost:8025`**.

---

## Principais Endpoints

### Usuários e Equipes (`http://localhost:8081`)
- `POST /api/usuarios` — Cadastra um novo usuário e dispara e-mail de boas-vindas
- `GET /api/usuarios/{id}` — Consulta usuário por ID
- `POST /api/equipes` — Cria uma equipe vinculada a um chefe e jogadores
- `GET /api/equipes/{id}` — Consulta detalhes da equipe

### Modalidades e Eventos (`http://localhost:8082`)
- `POST /api/modalidades` — Cadastra uma nova modalidade (restrito a `ORGANIZADOR`)
- `POST /api/eventos` — Cria um evento esportivo com total de vagas
- `GET /api/eventos/{id}` — Consulta evento e vagas disponíveis
- `POST /api/eventos/{id}/resultados` — Registra colocação/pontuação das equipes
- `GET /api/eventos/{id}/resultados` — Lista o ranking final do evento

### Inscrições (`http://localhost:8083`)
- `POST /api/inscricoes` — Solicita inscrição de uma equipe em um evento
- `PATCH /api/inscricoes/{id}/avaliar` — Aprova ou recusa inscrição, atualiza vagas e notifica o chefe da equipe

### Notificações (`http://localhost:8084`)
- `POST /api/notificacoes` — Envia notificação por e-mail via SMTP