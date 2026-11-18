// client/NotificacaoClient.java
package microservicos.servicoinscricoes.client;

import microservicos.servicoinscricoes.dto.NotificacaoRequestDTO;
import microservicos.servicoinscricoes.dto.NotificacaoResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Cliente da interface INotificacaoService (servico-notificacoes, POST /notificacoes).
 * Devolve o DTO de resposta: o envio so conta como feito quando status == "ENVIADA".
 */
@FeignClient(name = "servico-notificacoes", url = "${app.services.notificacoes.url}")
public interface NotificacaoClient {

    @PostMapping("/notificacoes")
    NotificacaoResponseDTO enviarNotificacao(@RequestBody NotificacaoRequestDTO request);
}
