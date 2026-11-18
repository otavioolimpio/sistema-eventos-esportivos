package microservicos.servicoeventos.client;

import microservicos.servicoeventos.dto.UsuarioDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "servico-usuarios-equipes", url = "${app.services.usuarios-equipes.url:}")
public interface UsuarioEquipeClient {

    @GetMapping("/api/usuarios/{id}")
    UsuarioDTO buscarUsuarioPorId(@PathVariable("id") Long id);
}