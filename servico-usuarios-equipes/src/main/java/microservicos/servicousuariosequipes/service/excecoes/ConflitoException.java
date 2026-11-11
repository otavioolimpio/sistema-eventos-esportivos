package microservicos.servicousuariosequipes.service.excecoes;

/** Dado que ja existe, como e-mail ou CPF repetido (HTTP 409). */
public class ConflitoException extends RuntimeException {
    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
