package microservicos.serviconotificacoes.api;

import microservicos.serviconotificacoes.dominio.Notificacao;
import microservicos.serviconotificacoes.persistencia.NotificacaoRepository;
import microservicos.serviconotificacoes.service.NotificacaoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Interface fornecida INotificacaoService, exposta via REST. */
@RestController
@RequestMapping("/notificacoes")
public class NotificacaoController {

    private final NotificacaoService servico;
    private final NotificacaoRepository repositorio;

    public NotificacaoController(NotificacaoService servico, NotificacaoRepository repositorio) {
        this.servico = servico;
        this.repositorio = repositorio;
    }

    /**
     * Envia uma notificação. Responde 201 com o registro criado; o campo "status"
     * indica se o envio foi ENVIADA ou FALHA. Uma falha do provedor externo NÃO
     * gera erro HTTP: quem chamou decide como tratá-la (RN03 do UC07).
     */
    @PostMapping
    public ResponseEntity<Notificacao> enviar(@Valid @RequestBody NotificacaoRequest requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.enviar(requisicao));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Notificacao> buscar(@PathVariable String id) {
        return repositorio.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<Notificacao> listar(@RequestParam(defaultValue = "50") int limite) {
        return repositorio.findAllByOrderByCriadaEmDesc(PageRequest.of(0, Math.max(1, Math.min(limite, 500))));
    }
}
