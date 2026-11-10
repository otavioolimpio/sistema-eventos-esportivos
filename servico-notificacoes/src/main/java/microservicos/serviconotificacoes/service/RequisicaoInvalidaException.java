package microservicos.serviconotificacoes.service;

/** Dados da requisição inválidos para o canal escolhido. Vira HTTP 400. */
public class RequisicaoInvalidaException extends RuntimeException {

    public RequisicaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
