package microservicos.servicoeventos.controller;

import jakarta.validation.Valid;
import microservicos.servicoeventos.domain.enums.StatusEvento;
import microservicos.servicoeventos.domain.model.Evento;
import microservicos.servicoeventos.domain.model.ResultadoEvento;
import microservicos.servicoeventos.dto.CriarEventoRequestDTO;
import microservicos.servicoeventos.dto.RegistrarResultadoRequestDTO;
import microservicos.servicoeventos.service.EventoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @PostMapping
    public ResponseEntity<?> criar(@Valid @RequestBody CriarEventoRequestDTO request) {
        try {
            Evento criado = eventoService.criarEvento(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(criado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Evento>> listar(
            @RequestParam(value = "modalidade", required = false) String modalidade) {
        return ResponseEntity.ok(eventoService.listarEventos(modalidade));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok(eventoService.buscarEventoPorId(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> atualizarStatus(
            @PathVariable("id") Long id,
            @RequestParam("organizadorId") Long organizadorId,
            @RequestParam("status") StatusEvento status) {
        try {
            return ResponseEntity.ok(eventoService.atualizarStatusEvento(id, organizadorId, status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/{id}/vagas/ocupar")
    public ResponseEntity<?> ocuparVaga(@PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok(eventoService.ocuparVaga(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/{id}/resultados")
    public ResponseEntity<?> registrarResultado(
            @PathVariable("id") Long id,
            @Valid @RequestBody RegistrarResultadoRequestDTO request) {
        try {
            ResultadoEvento salvo = eventoService.registrarResultado(id, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erro", e.getMessage()));
        }
    }

    // UC04-SF03 - Consultar Resultado
    @GetMapping("/{id}/resultados")
    public ResponseEntity<?> consultarResultados(@PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok(eventoService.consultarResultados(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
        }
    }
}