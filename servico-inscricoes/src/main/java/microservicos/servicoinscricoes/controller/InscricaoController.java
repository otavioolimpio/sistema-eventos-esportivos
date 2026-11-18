package microservicos.servicoinscricoes.controller;

import jakarta.validation.Valid;
import microservicos.servicoinscricoes.domain.model.Inscricao;
import microservicos.servicoinscricoes.dto.AvaliarInscricaoRequestDTO;
import microservicos.servicoinscricoes.dto.InscricaoResponseDTO;
import microservicos.servicoinscricoes.dto.SolicitarInscricaoRequestDTO;
import microservicos.servicoinscricoes.facade.InscricaoFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API REST das inscricoes. Conversa APENAS com a {@link InscricaoFacade} (padrao Facade):
 * nao conhece os clientes Feign nem o repositorio.
 */
@RestController
@RequestMapping("/api/inscricoes")
public class InscricaoController {

    private final InscricaoFacade inscricaoFacade;

    public InscricaoController(InscricaoFacade inscricaoFacade) {
        this.inscricaoFacade = inscricaoFacade;
    }

    // UC05 - Solicitar Inscricao em Evento
    @PostMapping
    public ResponseEntity<?> solicitar(@Valid @RequestBody SolicitarInscricaoRequestDTO request) {
        try {
            Inscricao criada = inscricaoFacade.solicitarInscricao(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(criada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erro", e.getMessage()));
        }
    }

    // UC06 - Aprovar/Rejeitar Inscricao
    @PatchMapping("/{id}/avaliar")
    public ResponseEntity<?> avaliar(
            @PathVariable("id") Long id,
            @Valid @RequestBody AvaliarInscricaoRequestDTO request) {
        try {
            InscricaoResponseDTO resposta = inscricaoFacade.avaliarInscricao(id, request);
            return ResponseEntity.ok(resposta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Inscricao>> listar(
            @RequestParam(value = "eventoId", required = false) Long eventoId) {
        return ResponseEntity.ok(inscricaoFacade.listarInscricoes(eventoId));
    }
}