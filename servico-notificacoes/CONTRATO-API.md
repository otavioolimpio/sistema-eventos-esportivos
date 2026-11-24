# Contrato da API — Serviço de Notificações (`INotificacaoService`)

Documento para os serviços que enviam notificações (**Usuários e Equipes** e **Inscrições**):
como chamar o Serviço de Notificações para enviar uma mensagem.

## Endereço

O serviço não tem endereço fixo: ele sobe numa porta aleatória e se registra no **Eureka** com o nome `servico-notificacoes`.

| Quem chama | Como chega ao serviço |
|---|---|
| Outro microsserviço (dentro do Docker) | Cliente **OpenFeign** pelo nome: `@FeignClient(name = "servico-notificacoes")`. O Eureka informa o IP e a porta real |
| De fora do Docker (navegador, `curl`, painel) | Só pelo **API Gateway**: `http://localhost:<GATEWAY_PORT>/notificacoes` (porta no `.env` da raiz) |

## `POST /notificacoes` — enviar uma notificação

Usado pelo **UC02** (envio das credenciais do novo usuário) e pelo **UC06** (resultado da inscrição), por meio do «include» do **UC07**.

**Corpo (JSON):**

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `canal` | `"EMAIL"` ou `"DISCORD"` | sim | Por onde enviar |
| `destinatario` | texto (até 200) | sim | Para EMAIL, um e-mail válido. Para DISCORD, o nome citado na mensagem (o canal do Discord é definido pelo webhook) |
| `assunto` | texto (até 200) | não | Assunto do e-mail ou título da mensagem no Discord |
| `mensagem` | texto (até 5000) | sim | Conteúdo |

**Exemplo:**

```json
{
  "canal": "EMAIL",
  "destinatario": "chefe@exemplo.com",
  "assunto": "Suas credenciais de acesso",
  "mensagem": "Sua senha provisória é X7k9p2. Troque-a no primeiro acesso."
}
```

**Respostas:**

| HTTP | Quando | Corpo |
|---|---|---|
| `201 Created` | A requisição foi processada. **Verifique o campo `status`** | Registro da notificação (abaixo) |
| `400 Bad Request` | Campo obrigatório ausente, canal inexistente ou e-mail inválido | `{"erro": "...", "detalhes": ["..."]}` |

**Corpo do 201:**

```json
{
  "id": "6f1c2a7e-...",
  "canal": "EMAIL",
  "destinatario": "chefe@exemplo.com",
  "assunto": "Suas credenciais de acesso",
  "mensagem": "Sua senha provisória é X7k9p2. Troque-a no primeiro acesso.",
  "status": "ENVIADA",
  "erro": null,
  "criadaEm": "2026-11-11T14:30:00.123"
}
```

> **Importante — regra RN03 do UC07:** se o provedor externo falhar (servidor de e-mail fora do ar, por exemplo), a resposta continua sendo **201**, mas com `"status": "FALHA"` e o motivo em `"erro"`. O serviço que chamou **não deve desfazer** a própria operação. No UC02, isso corresponde ao **FA03**: o usuário continua cadastrado e o Organizador é avisado de que as credenciais não foram enviadas.

## Consultas (úteis para testes e para a apresentação)

- `GET /notificacoes/{id}`: uma notificação específica (404 se não existir).
- `GET /notificacoes?limite=50`: as notificações mais recentes.

## Cliente Java (OpenFeign)

É assim que os serviços de **Usuários e Equipes** e de **Inscrições** chamam este serviço. O exemplo é o do Serviço de Usuários (`servico-usuarios-equipes/src/main/java/microservicos/servicousuariosequipes/`); o de Inscrições é igual, no pacote `microservicos.servicoinscricoes.client`.

**1. A interface do cliente** (`client/NotificacaoClient.java`). O Spring gera a implementação ao subir; o endereço é resolvido pelo Eureka a cada chamada, pelo nome do serviço:

```java
package microservicos.servicousuariosequipes.client;

import microservicos.servicousuariosequipes.dto.NotificacaoRequestDTO;
import microservicos.servicousuariosequipes.dto.NotificacaoResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Cliente da interface INotificacaoService (servico-notificacoes, POST /notificacoes). */
@FeignClient(name = "servico-notificacoes", url = "${app.services.notificacoes.url:}")
public interface NotificacaoClient {

    @PostMapping("/notificacoes")
    NotificacaoResponseDTO enviarNotificacao(@RequestBody NotificacaoRequestDTO request);
}
```

O atributo `url` fica **vazio** por padrão, e aí o Feign usa o Eureka. A propriedade `app.services.notificacoes.url` só é definida nos **testes**, para apontar para um servidor falso; no Docker ela não existe.

**2. Os DTOs** (`dto/`). O cliente deve devolver um DTO, e não `void`, para poder ler o `status`:

```java
public record NotificacaoRequestDTO(String canal, String destinatario, String assunto, String mensagem) {}

@JsonIgnoreProperties(ignoreUnknown = true)
public record NotificacaoResponseDTO(String id, String status, String erro) {
    public boolean foiEnviada() { return "ENVIADA".equals(status); }
}
```

**3. Quem usa o cliente** (`client/NotificacaoGateway.java`) só considera enviado quando `status == "ENVIADA"` e nunca deixa uma exceção escapar (RN03 / FA03 do UC02):

```java
public boolean enviarEmail(String destinatario, String assunto, String mensagem) {
    try {
        NotificacaoResponseDTO resposta = client.enviarNotificacao(
                new NotificacaoRequestDTO("EMAIL", destinatario, assunto, mensagem));
        return resposta != null && resposta.foiEnviada();
    } catch (Exception e) {
        // Serviço de Notificações indisponível: tratar como falha de envio
        return false;
    }
}
```

**4. Configuração:** a aplicação precisa de `@EnableFeignClients` e da dependência `spring-cloud-starter-netflix-eureka-client`, com `eureka.client.service-url.defaultZone` apontando para o Eureka (no Docker, `EUREKA_URL=http://service-discovery:${EUREKA_PORT}/eureka/`). Nenhuma URL deste serviço vai no `application.properties`.
