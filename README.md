# Sistema de Eventos Esportivos

Monorepo contendo a arquitetura de microserviços desenvolvida com Java 21, Spring Boot 3 e banco de dados SQLite.

## Microserviços do Ecossistema

* **`servico-usuarios-equipes`**: Gestão de contas de usuários (Jogadores, Chefes de Equipe, Organizadores) e gestão de equipes.
* **`servico-eventos`**: Cadastro de modalidades e gerenciamento de eventos esportivos.
* **`servico-inscricoes`**: Solicitação e aprovação/rejeição de inscrições em eventos.
* **`servico-notificacoes`**: Envio de notificações para os usuários.

## Estrutura do Projeto

```text
sistema-eventos-esportivos/
├── pom.xml                           # Parent POM (Gerenciamento centralizado de dependências)
├── .gitignore                        # Regras de ignorados (compilados, IDEs e bancos .db)
├── README.md                         # Documentação do ecossistema
└── servico-usuarios-equipes/         # Microserviço de Usuários e Equipes (Porta 8081)
    ├── pom.xml
    └── src/

## Tecnologias Utilizadas

* **Linguagem:** Java 21
* **Framework:** Spring Boot 3.2+
* **Persistência:** Spring Data JPA + SQLite
* **Gerenciador de Dependências:** Maven (Multi-módulo)

## Como Executar

### Pré-requisitos
* Java JDK 21+
* Maven 3.8+

### Compilação de todos os serviços
Na raiz do repositório, execute:
```bash
mvn clean package
```

### Testes automatizados
```bash
mvn -f servico-usuarios-equipes/pom.xml test
```

* `UsuarioServiceTest`: criar usuário envia as credenciais por e-mail; falha no envio não desfaz o cadastro (FA03).
* `IntegracaoNotificacoesTest`: sobe o serviço inteiro com um servidor HTTP falso no lugar do `servico-notificacoes` e confere a URL, o caminho `/notificacoes` e o JSON enviados.

### Subindo tudo com Docker
Na raiz do repositório:
```bash
docker compose up --build
```

| Serviço | URL |
| :--- | :--- |
| `servico-usuarios-equipes` | http://localhost:8081 |
| `servico-notificacoes` | http://localhost:8084 |
| Mailpit (caixa de e-mail de teste) | http://localhost:8025 |

### Teste manual: criar usuário dispara notificação
```bash
curl -X POST http://localhost:8081/api/usuarios \
  -H "Content-Type: application/json" \
  -d '{"nome":"Ana","email":"ana@exemplo.com","cpf":"111.222.333-44","tipoPerfil":"CHEFE_EQUIPE"}'
```

A resposta traz `"credenciaisEnviadas": true`. O e-mail com a senha provisória aparece no Mailpit (http://localhost:8025), e o registro aparece em `GET http://localhost:8084/notificacoes`.

No Windows (PowerShell), use `curl.exe` no lugar de `curl`, ou teste pelo Postman/Insomnia.
