package microservicos.servicoeventos.controller;

import jakarta.validation.Valid;
import microservicos.servicoeventos.domain.model.Modalidade;
import microservicos.servicoeventos.dto.CriarModalidadeRequestDTO;
import microservicos.servicoeventos.service.EventoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/modalidades")
public class ModalidadeController {

    private final EventoService eventoService;

    public ModalidadeController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @PostMapping
    public ResponseEntity<?> cadastrar(@Valid @RequestBody CriarModalidadeRequestDTO request) {
        try {
            Modalidade criada = eventoService.cadastrarModalidade(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(criada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Modalidade>> listar() {
        return ResponseEntity.ok(eventoService.listarModalidades());
    }
}