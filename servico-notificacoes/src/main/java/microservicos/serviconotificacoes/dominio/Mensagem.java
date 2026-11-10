package microservicos.serviconotificacoes.dominio;

/**
 * Mensagem no formato interno do sistema, independente do provedor.
 * Cada adaptador converte esta mensagem para o formato do seu provedor.
 */
public record Mensagem(String destinatario, String assunto, String conteudo) {
}
