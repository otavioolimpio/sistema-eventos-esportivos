# Serviço de Notificações

Serviço do **Sistema de Gerenciamento de Eventos de E-Sports** (UFRPE — Análise e Projeto de Sistemas OO).
Implementa o **UC07 - Enviar Notificação**, caso de uso de inclusão usado pelo **UC02** (envio de credenciais) e pelo **UC06** (resultado da inscrição).

**Stack:** Java 21 · Spring Boot 3.2 · Spring Data JPA · SQLite · Docker

Módulo `servico-notificacoes` do monorepo `sistema-eventos-esportivos` (pai: `sistema-eventos-parent`).

## Onde estão os padrões de projeto

| Padrão | Classe | Papel |
|---|---|---|
| **Singleton** | `singleton/NotificationClientManager` | Instância única (construtor privado + `getInstance()`) que guarda os adaptadores e por onde passam todos os envios |
| **Adapter** — alvo | `adapter/INotificador` | Interface única que o sistema usa para enviar mensagens |
| **Adapter** — adaptador | `adapter/EmailAdapter` | Adapta o `JavaMailSender` (servidor SMTP) à interface `INotificador` |
| **Adapter** — adaptador | `adapter/DiscordWebhookAdapter` | Adapta a API de webhooks do Discord à interface `INotificador` |

Fluxo de um envio (mesmos passos da especificação do UC07):

```
POST /notificacoes  →  NotificacaoController
                    →  NotificacaoService
                         passo 2: NotificationClientManager.getInstance()   (Singleton)
                         passo 3: escolhe o INotificador do canal            (Adapter)
                         passo 4: EmailAdapter / DiscordWebhookAdapter enviam
                         FA01:    registra o envio no SQLite, com ou sem falha
                    ←  passo 5: devolve o resultado (status ENVIADA ou FALHA)
```

Para incluir um novo canal (ex.: SMS), basta criar uma classe `SmsAdapter implements INotificador` com `@Component` e um novo valor no enum `Canal`. Nada mais muda: é o argumento do Adapter para a apresentação.

## Como rodar

### Opção 1 — Docker (recomendado)

Pré-requisito: Docker com o Compose. De dentro da pasta `servico-notificacoes`:

```bash
docker compose up --build
```

O build usa a raiz do repositório como contexto, porque o pom pai fica lá.

Sobem dois contêineres:
- **servico-notificacoes**, em http://localhost:8084
- **mailpit**, servidor de e-mail falso: todos os e-mails enviados aparecem em **http://localhost:8025**

### Opção 2 — direto na máquina

Pré-requisitos: Java 21 e Maven.

```bash
docker run -d -p 1025:1025 -p 8025:8025 axllent/mailpit   # servidor de e-mail falso
cd servico-notificacoes
mvn spring-boot:run
```

O banco `notificacoes.db` é criado na pasta do módulo. Rodar o Maven de dentro da pasta do módulo funciona mesmo enquanto os outros módulos listados no pom pai ainda não existem.

## Testando

Enviar um e-mail (depois veja em http://localhost:8025):

```bash
curl -X POST http://localhost:8084/notificacoes \
  -H "Content-Type: application/json" \
  -d '{"canal":"EMAIL","destinatario":"chefe@exemplo.com","assunto":"Inscrição aprovada","mensagem":"A inscrição da equipe Alpha foi aprovada."}'
```

Listar as notificações registradas:

```bash
curl http://localhost:8084/notificacoes
```

No Windows (PowerShell), use `curl.exe` no lugar de `curl`, ou teste pelo Postman/Insomnia.

Testes automatizados (não precisam do Docker), de dentro da pasta `servico-notificacoes`:

```bash
mvn test
```

## Configuração

| Variável | Padrão | Para que serve |
|---|---|---|
| `PORT` | `8084` | Porta HTTP |
| `NOTIFICACAO_DB` | `notificacoes.db` (no Docker: `/data/notificacoes.db`) | Arquivo do SQLite |
| `MAIL_HOST` / `MAIL_PORT` | `localhost` / `1025` | Servidor SMTP |
| `MAIL_FROM` | `no-reply@esports.local` | Remetente dos e-mails |
| `DISCORD_WEBHOOK_URL` | vazio | Webhook do Discord. Sem ele, envios por DISCORD resultam em `FALHA` |

**Discord (opcional):** no servidor do Discord, vá em *Configurações do canal → Integrações → Webhooks → Novo webhook*, copie a URL e rode:

```bash
DISCORD_WEBHOOK_URL="https://discord.com/api/webhooks/..." docker compose up --build
```

**Gmail real (opcional, em vez do Mailpit):** crie uma *senha de app* na conta Google e defina `MAIL_HOST=smtp.gmail.com`, `MAIL_PORT=587`, `MAIL_SMTP_AUTH=true`, `MAIL_STARTTLS=true`, `SPRING_MAIL_USERNAME=seu@gmail.com` e `SPRING_MAIL_PASSWORD=senha-de-app`. Não coloque a senha no código nem no repositório.

## Integração com os outros serviços

O contrato da API e um cliente Java pronto para o Serviço de Usuários estão em **[CONTRATO-API.md](CONTRATO-API.md)**.

Para subir todos os serviços juntos na entrega, o grupo pode criar um `docker-compose.yml` na raiz do repositório, com um bloco por serviço. O deste serviço fica assim:

```yaml
  notificacoes:
    build:
      context: .
      dockerfile: servico-notificacoes/Dockerfile
    ports: ["8084:8084"]
    environment:
      MAIL_HOST: mailpit
      MAIL_PORT: "1025"
    volumes: [notificacoes-data:/data]
    depends_on: [mailpit]
```

## Estrutura

```
src/main/java/microservicos/serviconotificacoes/
├── ServicoNotificacoesApplication.java
├── api/            NotificacaoController, NotificacaoRequest, TratadorDeErros
├── adapter/        INotificador, EmailAdapter, DiscordWebhookAdapter, FalhaEnvioException
├── singleton/      NotificationClientManager
├── config/         NotificacaoConfig (registra os adaptadores no Singleton)
├── service/        NotificacaoService (fluxo do UC07)
├── persistencia/   NotificacaoRepository (Spring Data JPA + SQLite)
└── dominio/        Canal, StatusEnvio, Mensagem, ResultadoEnvio, Notificacao (entidade JPA)
```
