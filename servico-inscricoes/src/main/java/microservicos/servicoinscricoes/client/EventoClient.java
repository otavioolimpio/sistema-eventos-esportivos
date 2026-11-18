// client/EventoClient.java
package microservicos.servicoinscricoes.client;

import microservicos.servicoinscricoes.dto.EventoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "servico-eventos", url = "${app.services.eventos.url}")
public interface EventoClient {

    @GetMapping("/api/eventos/{id}")
    EventoDTO buscarEventoPorId(@PathVariable("id") Long id);

    @PostMapping("/api/eventos/{id}/vagas/ocupar")
    void ocuparVaga(@PathVariable("id") Long id);
}