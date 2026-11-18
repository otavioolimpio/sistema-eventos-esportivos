// client/UsuarioEquipeClient.java
package microservicos.servicoinscricoes.client;

import microservicos.servicoinscricoes.dto.EquipeDTO;
import microservicos.servicoinscricoes.dto.UsuarioDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "servico-usuarios-equipes", url = "${app.services.usuarios-equipes.url}")
public interface UsuarioEquipeClient {

    @GetMapping("/api/equipes/{id}")
    EquipeDTO buscarEquipePorId(@PathVariable("id") Long id);

    @GetMapping("/api/usuarios/{id}")
    UsuarioDTO buscarUsuarioPorId(@PathVariable("id") Long id);
}