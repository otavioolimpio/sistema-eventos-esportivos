# Contrato da API — Serviço de Notificações (`INotificacaoService`)

Documento para a dupla do **Serviço de Usuários** (e, depois, do Serviço de Inscrições):
como chamar o Serviço de Notificações para enviar uma mensagem.

## Endereço

| Onde o serviço que chama está rodando | URL base |
|---|---|
| Na máquina, fora do Docker | `http://localhost:8084` |
| Em outro contêiner do mesmo `docker-compose` | `http://notificacoes:8084` |

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

## Exemplo de cliente Java para o Serviço de Usuários

Basta copiar esta classe para o módulo `servico-usuarios-equipes` (Spring Boot 3.2 ou superior) e injetá-la onde o UC02 envia as credenciais:

```java
package br.com.microservicos.servicousuariosequipes.cliente;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/** Cliente da interface INotificacaoService (Serviço de Notificações). */
@Component
public class NotificacaoClient {

    private final RestClient restClient;

    public NotificacaoClient(@Value("${servicos.notificacoes.url:http://localhost:8084}") String urlBase) {
        this.restClient = RestClient.builder().baseUrl(urlBase).build();
    }

    /**
     * Envia as credenciais do novo usuário (UC02, SF01, passo 6).
     * @return true se o e-mail foi enviado; false se falhou (FA03 do UC02)
     */
    public boolean enviarCredenciais(String email, String senhaProvisoria) {
        Map<String, String> corpo = Map.of(
                "canal", "EMAIL",
                "destinatario", email,
                "assunto", "Suas credenciais de acesso",
                "mensagem", "Sua senha provisória é " + senhaProvisoria
                        + ". Troque-a no primeiro acesso.");
        try {
            Map<?, ?> resposta = restClient.post()
                    .uri("/notificacoes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .body(Map.class);
            return resposta != null && "ENVIADA".equals(resposta.get("status"));
        } catch (RestClientException e) {
            // Serviço de Notificações indisponível: tratar como falha de envio (FA03 do UC02)
            return false;
        }
    }
}
```

No `application.properties` do Serviço de Usuários, quando rodar no Docker:

```properties
servicos.notificacoes.url=${NOTIFICACOES_URL:http://localhost:8084}
```

e no `docker-compose` defina `NOTIFICACOES_URL: http://notificacoes:8084`.
