# Sistema de Eventos Esportivos

Monorepo contendo a arquitetura de microserviços desenvolvida com Java 21, Spring Boot 3 e banco de dados SQLite.

## Microserviços do Ecossistema

* **`servico-usuarios-equipes`**: Gestão de contas de usuários (Jogadores, Chefes de Equipe, Organizadores) e gestão de equipes.
* **`servico-eventos`**: Cadastro de modalidades e gerenciamento de eventos esportivos.
* **`servico-inscricoes`**: Solicitação e aprovação/rejeição de inscrições em eventos.
* **`servico-notificacoes`**: Envio de notificações para os usuários.

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