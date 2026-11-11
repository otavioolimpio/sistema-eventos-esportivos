package microservicos.servicousuariosequipes.client;

import microservicos.servicousuariosequipes.dto.NotificacaoRequestDTO;
import microservicos.servicousuariosequipes.dto.NotificacaoResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Ponto unico de envio de notificacoes deste servico («include» do UC07).
 *
 * Nunca lanca excecao: se o servico de notificacoes estiver fora do ar ou
 * registrar FALHA, devolve false e quem chamou decide o que fazer
 * (RN03 do UC07 / FA03 do UC02: o cadastro nao e desfeito).
 */
@Component
public class NotificacaoGateway {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoGateway.class);

    private final NotificacaoClient client;

    public NotificacaoGateway(NotificacaoClient client) {
        this.client = client;
    }

    /** @return true se o servico de notificacoes confirmou o envio (status ENVIADA). */
    public boolean enviarEmail(String destinatario, String assunto, String mensagem) {
        try {
            NotificacaoResponseDTO resposta = client.enviarNotificacao(
                    new NotificacaoRequestDTO("EMAIL", destinatario, assunto, mensagem));
            if (resposta != null && resposta.foiEnviada()) {
                return true;
            }
            log.warn("Servico de notificacoes nao conseguiu enviar o e-mail para {}: {}",
                    destinatario, resposta == null ? "resposta vazia" : resposta.erro());
            return false;
        } catch (Exception e) {
            log.warn("Nao foi possivel contatar o servico de notificacoes: {}", e.getMessage());
            return false;
        }
    }
}
