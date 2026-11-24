# Sistema de Gerenciamento de Eventos de E-Sports

Plataforma em **microsserviços** para organizar campeonatos de e-sports. O **Organizador** cadastra modalidades e eventos e aprova ou rejeita inscrições, o **Chefe de Equipe** monta a equipe e a inscreve num evento, e o sistema avisa os envolvidos por e-mail.

Trabalho da disciplina **Análise e Projeto de Sistemas Orientados a Objetos** (APSOO, 14108), UFRPE, prof. Lucas Albertins. **Grupo 2:** Arthur Leonardo, Guilherme Guimarães, Júlio Lucena e Otávio Olímpio.

> **Para executar:** siga o [`readme.txt`](readme.txt), que traz o passo a passo para Ubuntu e Windows. O resumo está em [Como executar](#como-executar).

---

## Casos de uso

**Atores:** Jogador, Chefe de Equipe (herda de Jogador), Organizador do Evento e Serviço Externo de Notificações («system»). Só o Organizador cria contas, e a senha provisória vai por e-mail.

| UC | Nome | Serviço | Padrão GoF |
|---|---|---|---|
| UC01 | Cadastrar Modalidade | Eventos | — |
| UC02 | Gerenciar Conta (cadastrar usuário, alterar senha) | Usuários e Equipes | (usa o UC07) |
| UC03 | Gerenciar Equipe | Usuários e Equipes | Strategy |
| UC04 | Gerenciar Evento (criar, registrar e consultar resultado) | Eventos | — |
| UC05 | Solicitar Inscrição em Evento | Inscrições | Facade |
| UC06 | Aprovar/Rejeitar Inscrição | Inscrições | Facade (usa o UC07) |
| UC07 | Enviar Notificação («include» do UC02 e do UC06) | Notificações | Adapter e Singleton |

Regras que aparecem na demonstração:
- **RN03 (UC07):** uma falha no envio de notificação não desfaz a operação. A notificação fica registrada com status `FALHA`.
- **Elenco por modalidade (Strategy):** FPS exige 5 jogadores; MOBA, 5 ou 6; Luta, 1.

---

## Arquitetura

```
              Painel Swing / curl / navegador
                            │  http://localhost:${GATEWAY_PORT}
                            ▼
                  ┌───────────────────┐  consulta  ┌────────────────────────┐
                  │    api-gateway    │───────────►│   service-discovery    │
                  │  (Spring Cloud    │            │       (Eureka)         │
                  │     Gateway)      │            │  nome → IP:porta real  │
                  └─────────┬─────────┘            └───────────▲────────────┘
                            │ lb://servico-xxx                 │ cada serviço se registra
     ┌──────────────┬───────┴────────┬────────────────┐        │ com a sua porta aleatória
     ▼              ▼                ▼                ▼        │
 Usuários e      Eventos        Inscrições      Notificações ──── SMTP ──► Mailpit
  Equipes       (SQLite)         (SQLite)         (SQLite)
  (SQLite)       (os 4 chamam uns aos outros por OpenFeign, pelo nome no Eureka)

                     rede interna eventos-net (Docker Compose)
```

Monorepo Maven (pom pai `sistema-eventos-parent`), **Java 21, Spring Boot 3.2.4 e Spring Cloud 2023.0.1**. Cada serviço de negócio usa:
- Spring Web (REST);
- Spring Data JPA + SQLite, com um banco por serviço (`ddl-auto=update`: as tabelas nascem das entidades);
- OpenFeign para chamar outros serviços pelo nome;
- cliente Eureka.

| Componente | Pasta | Porta | Publicada fora do Docker? | Papel |
|---|---|---|---|---|
| API Gateway (Spring Cloud Gateway) | `api-gateway` | `GATEWAY_PORT` (`.env`) | Sim | Única porta de entrada; roteia pelo caminho |
| Service Discovery (Eureka Server) | `service-discovery` | `EUREKA_PORT` (`.env`) | Sim (painel) | Catálogo de serviços: nome → endereço |
| Serviço de Usuários e Equipes | `servico-usuarios-equipes` | aleatória | Não | UC02, UC03 |
| Serviço de Eventos | `servico-eventos` | aleatória | Não | UC01, UC04 |
| Serviço de Inscrições | `servico-inscricoes` | aleatória | Não | UC05, UC06 (orquestração) |
| Serviço de Notificações | `servico-notificacoes` | aleatória | Não | UC07 |
| Mailpit (`axllent/mailpit`) | — | `MAILPIT_WEB_PORT` (web), `MAILPIT_SMTP_PORT` (SMTP) | Só a web | Caixa de e-mail de teste |

**Quem chama quem** (OpenFeign, sempre pelo nome registrado no Eureka):

| Serviço | Chama | Para quê |
|---|---|---|
| Usuários e Equipes | Notificações | Enviar as credenciais (UC02) e avisar o chefe da equipe criada (UC03) |
| Eventos | Usuários e Equipes | Conferir se quem cadastra modalidade, cria evento, muda o status ou registra resultado é ORGANIZADOR |
| Inscrições | Usuários e Equipes, Eventos e Notificações | Orquestrar o UC05 e o UC06 (`InscricaoFacade`) |

**Bancos** (um por serviço; nenhum serviço lê o banco de outro):

| Serviço | Arquivo | Tabelas |
|---|---|---|
| Usuários e Equipes | `usuarios_equipes.db` | `tb_usuarios`, `tb_equipes`, `tb_equipe_jogadores` |
| Eventos | `eventos.db` | `tb_modalidades`, `tb_eventos`, `tb_resultados_eventos` |
| Inscrições | `inscricoes.db` | `tb_inscricoes` (guarda só ids; o resto é consultado via Feign) |
| Notificações | `notificacoes.db` | `notificacao` |

No Docker, cada banco fica em `/data`, num volume nomeado (`usuarios-data`, `eventos-data`, `inscricoes-data`, `notificacoes-data`): `docker compose down` mantém os dados e `docker compose down -v` os apaga.

---

## Decisões de projeto

| Decisão | Justificativa | Onde está no código |
|---|---|---|
| **Eureka** como Service Discovery | Nenhuma URL ou porta de outro serviço fica no código: cada serviço se registra pelo `spring.application.name` e os outros o chamam pelo nome. É a tecnologia do repositório de referência da disciplina e se integra ao OpenFeign e ao Gateway. | `@EnableEurekaServer` em `ServiceDiscoveryApplication`; `spring-cloud-starter-netflix-eureka-client` no `pom.xml` de cada serviço |
| **Spring Cloud Gateway** | Uma única porta de entrada, que esconde os serviços. As rotas `lb://` consultam o Eureka e distribuem as chamadas se houver mais de uma instância. Da mesma família Spring Cloud: é quase só configuração. | `api-gateway/src/main/resources/application.yml` |
| **Docker Compose** como orquestrador | Um comando sobe os 7 contêineres na ordem certa (`depends_on`), numa rede interna (`eventos-net`), e reinicia quem cair (`restart: unless-stopped`). Kubernetes seria grande demais para o escopo. | `docker-compose.yml` |
| **Orquestração na `InscricaoFacade`** (e não num serviço orquestrador separado) | A inscrição é o único fluxo que atravessa vários serviços. A Facade concentra a sequência (3 passos no UC05, 8 no UC06) e é também o padrão GoF Facade do modelo. | `servico-inscricoes/.../facade/InscricaoFacade.java` (passos numerados nos comentários) |
| **Porta aleatória** nos serviços de negócio | `server.port=0`: o sistema escolhe uma porta livre e o Eureka registra a porta real, com um `instance-id` único. As únicas portas fixas (Gateway, Eureka e Mailpit) ficam no `.env`; nenhuma aparece no código, nos `application.properties` nem nos Dockerfiles. | `application.properties` de cada serviço; `.env`; `docker-compose.yml` |
| **Só Gateway, Eureka e Mailpit publicam portas** | Os 4 serviços não têm `ports` no compose: só são alcançáveis dentro da rede do Docker, pelo Gateway. | `docker-compose.yml` |
| **Um banco SQLite por serviço** (*database per service*) | Cada serviço é dono dos seus dados; os outros só os acessam pela API. SQLite não precisa de servidor, o que simplifica a entrega. | `spring.datasource.url` de cada serviço |
| **Mailpit** como servidor SMTP de teste | Mostra os e-mails enviados no navegador, sem conta de e-mail real. | Serviço `mailpit` no `docker-compose.yml` |

**Injeção de dependência e descoberta:** os clientes Feign são interfaces (`@FeignClient(name = "servico-eventos")`). O Spring gera a implementação ao subir e a injeta pelo construtor (por exemplo, na `InscricaoFacade`). O endereço de destino é resolvido pelo Eureka a cada chamada, a partir de uma cópia da lista de serviços renovada a cada 30 s.

---

## Rotas do Gateway

Todas as chamadas externas usam `http://localhost:${GATEWAY_PORT}` (padrão do `.env`: 8080).

| Caminho | Destino |
|---|---|
| `/api/usuarios/**`, `/api/equipes/**` | `lb://servico-usuarios-equipes` |
| `/api/modalidades/**`, `/api/eventos/**` | `lb://servico-eventos` |
| `/api/inscricoes/**` | `lb://servico-inscricoes` |
| `/notificacoes/**` | `lb://servico-notificacoes` |

Principais endpoints:

| Método | Caminho | Caso de uso |
|---|---|---|
| `POST` | `/api/usuarios` | UC02: cadastra o usuário e envia a senha provisória por e-mail |
| `PUT` | `/api/usuarios/{id}/senha` | UC02: altera a senha |
| `GET` | `/api/usuarios`, `/api/usuarios/{id}` | Consulta |
| `POST` | `/api/equipes` | UC03: cria a equipe (elenco validado pela Strategy) |
| `GET` | `/api/equipes`, `/api/equipes/{id}` | UC03: consulta |
| `POST` / `GET` | `/api/modalidades` | UC01 |
| `POST` / `GET` | `/api/eventos`, `GET /api/eventos/{id}` | UC04: cria e consulta eventos |
| `PATCH` | `/api/eventos/{id}/status` | UC04: muda o status do evento |
| `POST` / `GET` | `/api/eventos/{id}/resultados` | UC04: registra e consulta o resultado |
| `POST` | `/api/inscricoes` | UC05: solicita a inscrição (fica `PENDENTE`) |
| `PATCH` | `/api/inscricoes/{id}/avaliar` | UC06: aprova ou rejeita e notifica o chefe |
| `GET` | `/api/inscricoes` | Consulta |
| `POST` | `/notificacoes` | UC07: envia (sempre 201, com `status` `ENVIADA` ou `FALHA`) |
| `GET` | `/notificacoes`, `/notificacoes/{id}` | UC07: consulta |

O endpoint `POST /api/eventos/{id}/vagas/ocupar` existe para uso interno da `InscricaoFacade` (passo 5 do UC06).

---

## Padrões GoF

| Padrão | Serviço | Classes | Papel |
|---|---|---|---|
| **Strategy** | Usuários e Equipes | `domain/strategy/ValidacaoElencoStrategy`, `StrategyFPS`, `StrategyMOBA`, `StrategyLuta`, `ContextoValidacaoElenco` | O contexto escolhe a regra de elenco pela modalidade da equipe |
| **Facade** | Inscrições | `facade/InscricaoFacade` | Esconde do `InscricaoController` a sequência de chamadas a Usuários, Eventos e Notificações; o controller depende só dela |
| **Adapter** | Notificações | `adapter/INotificador` (alvo), `EmailAdapter` (adapta o `JavaMailSender`), `DiscordWebhookAdapter` (adapta o webhook do Discord) | Um canal novo é só uma classe nova que implementa `INotificador` |
| **Singleton** | Notificações | `singleton/NotificationClientManager` | Construtor privado e `getInstance()`; guarda um adaptador por canal e nunca lança exceção ao enviar |

As classes ficam em `<servico>/src/main/java/microservicos/<nome>/`.

---

## Como executar

Detalhes, Windows e solução de problemas: [`readme.txt`](readme.txt). Requisitos: Git, Docker com Compose e JDK 21 (só para o painel e os testes).

```bash
git clone https://github.com/otavioolimpio/sistema-eventos-esportivos.git
cd sistema-eventos-esportivos
docker compose up --build -d   # 1ª vez: de 8 a 30 min; depois, docker compose up -d
docker compose ps              # 7 contêineres
```

1. Espere cerca de 60 s até os 4 serviços e o `API-GATEWAY` aparecerem em `http://localhost:${EUREKA_PORT}` (padrão: 8761). Antes disso, as chamadas dão 500 ou 503.
2. Tudo é acessado por `http://localhost:${GATEWAY_PORT}` (padrão: 8080). Os e-mails aparecem em `http://localhost:${MAILPIT_WEB_PORT}` (padrão: 8025).
3. As portas estão todas no arquivo [`.env`](.env). Mudar uma porta é mudar uma linha.

Testes automatizados (não precisam do Docker): `mvn clean test` na raiz (19 testes).

---

## Demonstração

### Pelo painel (Java Swing)

O painel [`painel-demo/PainelEsports.java`](painel-demo/PainelEsports.java) é um arquivo só, sem dependências, e fala **só com o Gateway**. Ele lê as portas do `.env` e mostra no topo de onde veio cada endereço.

```bash
java painel-demo/PainelEsports.java   # a partir da raiz do repositório
```

Fluxo (IDs com os bancos vazios):
1. **Usuários (UC02):** cadastre o ORGANIZADOR (id 1), o CHEFE_EQUIPE (id 2) e 5 JOGADORES (ids 3 a 7). Cada um recebe e-mail com a senha provisória.
2. **Modalidades e Eventos (UC01, UC04):** cadastre a modalidade FPS e o evento Copa FPS (FPS, organizador 1, 2 vagas).
3. **Equipes (UC03):** crie a equipe Alpha (FPS, chefe 2) com 4 jogadores: **400**, a `StrategyFPS` recusa. Com os jogadores 3 a 7: **201**.
4. **Inscrições (UC05):** solicite a inscrição (equipe 1, evento 1, chefe 2): `PENDENTE`.
5. **Inscrições (UC06):** aprove (organizador 1): `APROVADA`, `notificacaoEnviada: true`, vagas de 2 para 1 e o e-mail "Inscricao APROVADA - Copa FPS" no Mailpit.
6. **Notificações (UC07):** veja os envios registrados e abra o Mailpit.

### Pelo terminal

- `bash scripts/fluxo-gateway.sh` (Ubuntu): roda o mesmo fluxo de uma vez pelo Gateway, lendo as portas do `.env`.
- `powershell -ExecutionPolicy Bypass -File .\demo.ps1` (Windows): roteiro interativo, passo a passo, também pelo Gateway. Inclui a demonstração da RN03 (Mailpit desligado: o cadastro continua e a notificação fica como `FALHA`).

---

## Estrutura do repositório

```
.env                       portas (único lugar com números de porta)
docker-compose.yml         os 7 contêineres, a rede eventos-net e os volumes
readme.txt                 instruções de execução
api-gateway/               Spring Cloud Gateway
service-discovery/         Eureka Server
servico-usuarios-equipes/  UC02, UC03 (Strategy)
servico-eventos/           UC01, UC04
servico-inscricoes/        UC05, UC06 (Facade)
servico-notificacoes/      UC07 (Adapter, Singleton); contrato em CONTRATO-API.md
painel-demo/               painel Swing de demonstração
scripts/fluxo-gateway.sh   fluxo completo pelo terminal
demo.ps1                   roteiro interativo para o Windows
```
