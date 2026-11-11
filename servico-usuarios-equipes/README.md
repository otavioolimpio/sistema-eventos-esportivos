# Serviço de Usuários e Equipes (`servico-usuarios-equipes`)

Microserviço responsável pelo gerenciamento de contas de usuários (**UC02**) e pela criação/manutenção de equipes esportivas (**UC03**), incluindo validações automatizadas de elenco por modalidade e integração com o serviço de notificações (**UC07**).

---

## Tecnologias e Dependências

- **Linguagem & Framework:** Java 21 / Spring Boot 3.2.4
- **Persistência de Dados:** Spring Data JPA + SQLite (`org.xerial:sqlite-jdbc` e `hibernate-community-dialects`)
- **Comunicação Inter-Serviços:** Spring Cloud OpenFeign (comunicação REST com `servico-notificacoes`)
- **Produtividade & Utilidades:** Lombok, Bean Validation (`spring-boot-starter-validation`)
- **Containerização:** Docker (suporte a build multi-módulo)

---

## Arquitetura e Padrões de Projeto

### 1. Strategy Pattern (`ValidacaoElencoStrategy`)
Para garantir a integridade da formação das equipes de acordo com a modalidade esportiva (UC03), foi aplicado o padrão **Strategy**:
- **`StrategyFPS`**: Exige exatamente 5 jogadores.
- **`StrategyMOBA`**: Exige entre 5 e 6 jogadores.
- **`StrategyLuta`**: Exige exatamente 1 jogador.
- **`ContextoValidacaoElenco`**: Resolve e executa dinamicamente a estratégia correspondente no momento do cadastro/edição da equipe.

### 2. Comunicação Inter-Serviços com OpenFeign
Integração com o `servico-notificacoes` (**UC07**):
- **`NotificacaoClient`**: Declara os contratos REST (`POST /api/notificacoes`) para notificar o chefe de equipe assim que o grupo for criado com sucesso.

---

## Endpoints Principais

| Método | Endpoint | Descrição | Casos de Uso |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/usuarios` | Cadastra um novo usuário | UC02 |
| `GET` | `/api/usuarios` | Lista todos os usuários cadastrados | UC02 |
| `GET` | `/api/usuarios/{id}` | Busca um usuário por ID | UC02 |
| `POST` | `/api/equipes` | Cadastra uma nova equipe (valida elenco + notifica chefe) | UC03 / UC07 |
| `GET` | `/api/equipes` | Lista todas as equipes | UC03 |
| `GET` | `/api/equipes/{id}` | Busca equipe e elenco detalhado | UC03 |

---

## Configuração (`application.properties`)

```properties
server.port=8081
spring.application.name=servico-usuarios-equipes

# Banco de Dados SQLite
spring.datasource.url=jdbc:sqlite:usuarios_equipes.db
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Integração com o Serviço de Notificações
app.services.notificacoes.url=http://localhost:8083