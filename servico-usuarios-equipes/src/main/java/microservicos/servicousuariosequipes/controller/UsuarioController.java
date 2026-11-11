package microservicos.servicousuariosequipes.controller;

import jakarta.validation.Valid;
import microservicos.servicousuariosequipes.dto.AlterarSenhaRequest;
import microservicos.servicousuariosequipes.dto.UsuarioCriadoResponse;
import microservicos.servicousuariosequipes.dto.UsuarioRequest;
import microservicos.servicousuariosequipes.dto.UsuarioResponse;
import microservicos.servicousuariosequipes.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** UC02: cadastra o usuario e dispara o e-mail com as credenciais. */
    @PostMapping
    public ResponseEntity<UsuarioCriadoResponse> criar(@Valid @RequestBody UsuarioRequest requisicao) {
        UsuarioService.UsuarioCriado criado = usuarioService.criarUsuario(requisicao.paraEntidade());
        String aviso = criado.credenciaisEnviadas() ? null
                : "Usuario cadastrado, mas as credenciais nao foram enviadas por e-mail. "
                  + "Verifique o servico de notificacoes.";
        return ResponseEntity.status(HttpStatus.CREATED).body(new UsuarioCriadoResponse(
                UsuarioResponse.de(criado.usuario()), criado.credenciaisEnviadas(), aviso));
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listarTodos().stream().map(UsuarioResponse::de).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscar(@PathVariable Long id) {
        return usuarioService.buscarPorId(id)
                .map(UsuarioResponse::de)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Alterar Senha. Responde 204 se deu certo, 400 se a senha atual estiver errada. */
    @PutMapping("/{id}/senha")
    public ResponseEntity<Void> alterarSenha(@PathVariable Long id,
                                             @Valid @RequestBody AlterarSenhaRequest requisicao) {
        usuarioService.alterarSenha(id, requisicao.senhaAtual(), requisicao.novaSenha());
        return ResponseEntity.noContent().build();
    }
}
