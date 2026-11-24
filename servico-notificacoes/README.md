# Serviço de Notificações

Serviço do **Sistema de Gerenciamento de Eventos de E-Sports** (UFRPE — Análise e Projeto de Sistemas OO).
Implementa o **UC07 - Enviar Notificação**, caso de uso de inclusão usado pelo **UC02** (envio de credenciais) e pelo **UC06** (resultado da inscrição).

**Stack:** Java 21 · Spring Boot 3.2 · Spring Data JPA · SQLite · Eureka (cliente) · Docker

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

O serviço faz parte do sistema completo e sobe junto com os outros pelo `docker-compose.yml` da **raiz** do repositório (veja o `readme.txt` da raiz):

```bash
docker compose up --build -d
```

- O serviço sobe numa **porta aleatória** (`server.port=0`) e se registra no Eureka como `servico-notificacoes`. Ele não publica porta fora do Docker.
- De fora, o acesso é **só pelo API Gateway**, na rota `/notificacoes/**`.
- Os e-mails enviados aparecem no **Mailpit**, em `http://localhost:<MAILPIT_WEB_PORT>` (porta definida no `.env` da raiz).
- O banco SQLite fica em `/data/notificacoes.db`, no volume `notificacoes-data`.

### Direto na máquina (opcional)

Pré-requisitos: Java 21, Maven, o Eureka (`service-discovery`) e um servidor SMTP (por exemplo, o Mailpit) rodando. Defina a porta do Eureka e a porta SMTP e, de dentro da pasta `servico-notificacoes`:

```bash
export EUREKA_PORT=8761 SPRING_MAIL_PORT=1025
mvn spring-boot:run
```

O banco `notificacoes.db` é criado na pasta do módulo.

## Testando

Pelo API Gateway (`GATEWAY_PORT` do `.env`; padrão 8080). Enviar um e-mail e depois vê-lo no Mailpit:

```bash
curl -X POST http://localhost:8080/notificacoes \
  -H "Content-Type: application/json" \
  -d '{"canal":"EMAIL","destinatario":"chefe@exemplo.com","assunto":"Inscrição aprovada","mensagem":"A inscrição da equipe Alpha foi aprovada."}'
```

Listar as notificações registradas:

```bash
curl http://localhost:8080/notificacoes
```

No Windows (PowerShell), use `curl.exe` no lugar de `curl`, ou teste pelo Postman/Insomnia.

Testes automatizados (não precisam do Docker nem do Eureka), de dentro da pasta `servico-notificacoes` ou com `mvn clean test` na raiz:

```bash
mvn test
```

## Configuração

Nenhuma porta fica no código: a porta HTTP é aleatória e as demais vêm de variáveis de ambiente (no Docker, definidas pelo `docker-compose.yml` a partir do `.env` da raiz).

| Variável | Padrão | Para que serve |
|---|---|---|
| `EUREKA_URL` ou `EUREKA_PORT` | no Docker: `http://service-discovery:${EUREKA_PORT}/eureka/` | Onde o serviço se registra (Eureka) |
| `NOTIFICACAO_DB` | `notificacoes.db` (no Docker: `/data/notificacoes.db`) | Arquivo do SQLite |
| `MAIL_HOST` (ou `SPRING_MAIL_HOST`) | `localhost` (no Docker: `mailpit`) | Servidor SMTP |
| `SPRING_MAIL_PORT` | no Docker: `${MAILPIT_SMTP_PORT}` do `.env` | Porta do servidor SMTP |
| `MAIL_FROM` | `no-reply@esports.local` | Remetente dos e-mails |
| `DISCORD_WEBHOOK_URL` | vazio | Webhook do Discord. Sem ele, envios por DISCORD resultam em `FALHA` |

**Discord (opcional):** no servidor do Discord, vá em *Configurações do canal → Integrações → Webhooks → Novo webhook*, copie a URL e passe-a ao contêiner na variável `DISCORD_WEBHOOK_URL` (por exemplo, acrescentando-a ao `environment` do `servico-notificacoes` no `docker-compose.yml`).

**Gmail real (opcional, em vez do Mailpit):** crie uma *senha de app* na conta Google e defina `MAIL_HOST=smtp.gmail.com`, `SPRING_MAIL_PORT=587`, `MAIL_SMTP_AUTH=true`, `MAIL_STARTTLS=true`, `SPRING_MAIL_USERNAME=seu@gmail.com` e `SPRING_MAIL_PASSWORD=senha-de-app`. Não coloque a senha no código nem no repositório.

## Integração com os outros serviços

O contrato da API e o cliente OpenFeign usado pelos outros serviços estão em **[CONTRATO-API.md](CONTRATO-API.md)**. Hoje chamam este serviço:
- **Serviço de Usuários e Equipes:** credenciais do novo usuário (UC02) e aviso de equipe criada (UC03);
- **Serviço de Inscrições:** resultado da inscrição (UC06, passo 8 da `InscricaoFacade`).

No `docker-compose.yml` da raiz, o bloco deste serviço não tem `ports` (o acesso externo é pelo Gateway):

```yaml
  servico-notificacoes:
    build:
      context: .
      dockerfile: servico-notificacoes/Dockerfile
    environment:
      - EUREKA_URL=http://service-discovery:${EUREKA_PORT}/eureka/
      - SPRING_MAIL_HOST=mailpit
      - SPRING_MAIL_PORT=${MAILPIT_SMTP_PORT}
    volumes:
      - notificacoes-data:/data
    depends_on:
      - service-discovery
      - mailpit
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
