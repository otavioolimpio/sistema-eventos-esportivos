package microservicos.serviconotificacoes.adapter;

import microservicos.serviconotificacoes.dominio.Canal;
import microservicos.serviconotificacoes.dominio.Mensagem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * PADRÃO ADAPTER — adaptador concreto para e-mail.
 *
 * Classe adaptada (Adaptee): {@link JavaMailSender}, que trabalha com
 * {@link SimpleMailMessage} e lança {@link MailException}.
 * Este adaptador converte a {@link Mensagem} interna para o formato do servidor
 * SMTP e converte os erros do provedor em {@link FalhaEnvioException}.
 */
@Component
public class EmailAdapter implements INotificador {

    private final JavaMailSender mailSender;
    private final String remetente;

    public EmailAdapter(JavaMailSender mailSender,
                        @Value("${notificacao.email.remetente}") String remetente) {
        this.mailSender = mailSender;
        this.remetente = remetente;
    }

    @Override
    public Canal canal() {
        return Canal.EMAIL;
    }

    @Override
    public void enviar(Mensagem mensagem) {
        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(remetente);
        email.setTo(mensagem.destinatario());
        email.setSubject(mensagem.assunto() == null ? "" : mensagem.assunto());
        email.setText(mensagem.conteudo());

        try {
            mailSender.send(email);
        } catch (MailException e) {
            throw new FalhaEnvioException("Falha no envio de e-mail: " + e.getMessage(), e);
        }
    }
}
