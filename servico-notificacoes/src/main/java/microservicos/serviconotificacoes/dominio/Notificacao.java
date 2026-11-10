package microservicos.serviconotificacoes.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro de uma notificação processada pelo serviço.
 * Toda tentativa de envio é registrada, inclusive as que falham (FA01 do UC07).
 */
@Entity
@Table(name = "notificacao")
public class Notificacao {

    @Id
    @Column(length = 36)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Canal canal;

    @Column(nullable = false, length = 200)
    private String destinatario;

    @Column(length = 200)
    private String assunto;

    @Column(nullable = false, length = 5000)
    private String mensagem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusEnvio status;

    @Column(length = 2000)
    private String erro;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    /** Exigido pelo JPA. */
    protected Notificacao() {
    }

    public Notificacao(Canal canal, String destinatario, String assunto, String mensagem,
                       StatusEnvio status, String erro) {
        this.id = UUID.randomUUID().toString();
        this.canal = canal;
        this.destinatario = destinatario;
        this.assunto = assunto;
        this.mensagem = mensagem;
        this.status = status;
        this.erro = erro;
        this.criadaEm = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public Canal getCanal() {
        return canal;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public String getAssunto() {
        return assunto;
    }

    public String getMensagem() {
        return mensagem;
    }

    public StatusEnvio getStatus() {
        return status;
    }

    public String getErro() {
        return erro;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }
}
