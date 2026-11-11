package microservicos.servicousuariosequipes.dto;

import microservicos.servicousuariosequipes.domain.enums.TipoPerfil;
import microservicos.servicousuariosequipes.domain.model.Usuario;

public record UsuarioResponse(Long id, String nome, String email, String cpf, TipoPerfil tipoPerfil) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(),
                usuario.getCpf(), usuario.getTipoPerfil());
    }
}
