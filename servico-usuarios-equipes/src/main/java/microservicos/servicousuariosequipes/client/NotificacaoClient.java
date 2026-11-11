package microservicos.servicousuariosequipes.client;

import microservicos.servicousuariosequipes.dto.NotificacaoRequestDTO;
import microservicos.servicousuariosequipes.dto.NotificacaoResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Cliente da interface INotificacaoService (servico-notificacoes, POST /notificacoes). */
@FeignClient(name = "servico-notificacoes", url = "${app.services.notificacoes.url}")
public interface NotificacaoClient {

    @PostMapping("/notificacoes")
    NotificacaoResponseDTO enviarNotificacao(@RequestBody NotificacaoRequestDTO request);
}
