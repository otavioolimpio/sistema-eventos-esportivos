package microservicos.servicousuariosequipes.client;

import microservicos.servicousuariosequipes.dto.NotificacaoRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "servico-notificacoes", url = "${app.services.notificacoes.url:http://localhost:8083}")
public interface NotificacaoClient {

    @PostMapping("/api/notificacoes")
    void enviarNotificacao(@RequestBody NotificacaoRequestDTO request);
}