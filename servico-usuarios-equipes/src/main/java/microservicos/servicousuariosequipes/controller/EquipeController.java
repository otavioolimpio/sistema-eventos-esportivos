package microservicos.servicousuariosequipes.controller;

import jakarta.validation.Valid;
import microservicos.servicousuariosequipes.dto.EquipeRequest;
import microservicos.servicousuariosequipes.dto.EquipeResponse;
import microservicos.servicousuariosequipes.service.EquipeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/equipes")
public class EquipeController {

    private final EquipeService equipeService;

    public EquipeController(EquipeService equipeService) {
        this.equipeService = equipeService;
    }

    /** UC03: valida o elenco pela modalidade, salva e notifica o chefe da equipe. */
    @PostMapping
    public ResponseEntity<EquipeResponse> criar(@Valid @RequestBody EquipeRequest requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EquipeResponse.de(equipeService.criarEquipe(requisicao)));
    }

    @GetMapping
    public List<EquipeResponse> listar() {
        return equipeService.listarTodas().stream().map(EquipeResponse::de).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipeResponse> buscar(@PathVariable Long id) {
        return equipeService.buscarPorId(id)
                .map(EquipeResponse::de)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
