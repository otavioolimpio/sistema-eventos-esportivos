package microservicos.serviconotificacoes.service;

import microservicos.serviconotificacoes.api.NotificacaoRequest;
import microservicos.serviconotificacoes.dominio.Canal;
import microservicos.serviconotificacoes.dominio.Mensagem;
import microservicos.serviconotificacoes.dominio.Notificacao;
import microservicos.serviconotificacoes.dominio.ResultadoEnvio;
import microservicos.serviconotificacoes.dominio.StatusEnvio;
import microservicos.serviconotificacoes.persistencia.NotificacaoRepository;
import microservicos.serviconotificacoes.singleton.NotificationClientManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Fluxo principal do UC07 - Enviar Notificação.
 */
@Service
public class NotificacaoService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final NotificacaoRepository repositorio;

    public NotificacaoService(NotificacaoRepository repositorio) {
        this.repositorio = repositorio;
    }

    public Notificacao enviar(NotificacaoRequest requisicao) {
        // Passo 1: o caso de uso base informa destinatário, canal e conteúdo.
        validar(requisicao);
        Mensagem mensagem = new Mensagem(requisicao.destinatario(), requisicao.assunto(), requisicao.mensagem());

        // Passo 2: obtém a instância única do gerenciador (Singleton).
        NotificationClientManager gerenciador = NotificationClientManager.getInstance();

        // Passos 3 e 4: o gerenciador escolhe o adaptador do canal (Adapter) e envia.
        ResultadoEnvio resultado = gerenciador.enviar(requisicao.canal(), mensagem);

        // FA01: em caso de falha, a tentativa também é registrada.
        Notificacao notificacao = repositorio.save(new Notificacao(
                requisicao.canal(),
                requisicao.destinatario(),
                requisicao.assunto(),
                requisicao.mensagem(),
                resultado.status(),
                resultado.erro()));

        if (resultado.status() == StatusEnvio.FALHA) {
            log.warn("Falha ao enviar notificação {} por {}: {}", notificacao.getId(), notificacao.getCanal(), resultado.erro());
        }

        // Passo 5: devolve o resultado ao caso de uso base.
        return notificacao;
    }

    private void validar(NotificacaoRequest requisicao) {
        if (requisicao.canal() == Canal.EMAIL && !EMAIL.matcher(requisicao.destinatario()).matches()) {
            throw new RequisicaoInvalidaException("Para o canal EMAIL, o destinatario deve ser um e-mail válido.");
        }
    }
}
