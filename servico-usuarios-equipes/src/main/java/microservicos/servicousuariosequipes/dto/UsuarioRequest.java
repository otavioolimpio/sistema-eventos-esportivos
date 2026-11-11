package microservicos.servicousuariosequipes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import microservicos.servicousuariosequipes.domain.enums.TipoPerfil;
import microservicos.servicousuariosequipes.domain.model.Usuario;

/** Corpo do POST /api/usuarios (UC02). A senha nao vem aqui: o sistema gera uma provisoria. */
public record UsuarioRequest(
        @NotBlank(message = "nome e obrigatorio") String nome,
        @NotBlank(message = "email e obrigatorio") @Email(message = "email invalido") String email,
        @NotBlank(message = "cpf e obrigatorio") String cpf,
        @NotNull(message = "tipoPerfil e obrigatorio (JOGADOR, CHEFE_EQUIPE ou ORGANIZADOR)") TipoPerfil tipoPerfil
) {
    public Usuario paraEntidade() {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setCpf(cpf);
        usuario.setTipoPerfil(tipoPerfil);
        return usuario;
    }
}
