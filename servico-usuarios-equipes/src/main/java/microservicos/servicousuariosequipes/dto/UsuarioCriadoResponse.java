package microservicos.servicousuariosequipes.dto;

/**
 * Resposta do cadastro (UC02). Se credenciaisEnviadas for false, o usuario
 * continua cadastrado e o aviso orienta o Organizador (FA03 do UC02).
 */
public record UsuarioCriadoResponse(UsuarioResponse usuario, boolean credenciaisEnviadas, String aviso) {
}
