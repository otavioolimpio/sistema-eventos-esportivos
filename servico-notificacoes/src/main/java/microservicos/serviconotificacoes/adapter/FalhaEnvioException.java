package microservicos.serviconotificacoes.adapter;

/** Lançada por um adaptador quando o provedor externo falha (FA01 do UC07). */
public class FalhaEnvioException extends RuntimeException {

    public FalhaEnvioException(String mensagem) {
        super(mensagem);
    }

    public FalhaEnvioException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
