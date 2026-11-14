// client/NotificacaoClient.java
package microservicos.servicoinscricoes.client;

import microservicos.servicoinscricoes.dto.NotificacaoRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notificacoes-client", url = "${app.services.notificacoes.url}")
public interface NotificacaoClient {

    @PostMapping("/notificacoes")
    void enviarNotificacao(@RequestBody NotificacaoRequestDTO request);
}