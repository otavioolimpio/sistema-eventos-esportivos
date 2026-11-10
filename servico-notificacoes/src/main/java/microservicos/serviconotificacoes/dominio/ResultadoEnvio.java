package microservicos.serviconotificacoes.dominio;

/** Resultado devolvido ao caso de uso base (passo 5 do UC07). */
public record ResultadoEnvio(StatusEnvio status, String erro) {

    public static ResultadoEnvio sucesso() {
        return new ResultadoEnvio(StatusEnvio.ENVIADA, null);
    }

    public static ResultadoEnvio falha(String erro) {
        return new ResultadoEnvio(StatusEnvio.FALHA, erro);
    }
}
