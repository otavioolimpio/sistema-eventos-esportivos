// dto/InscricaoResponseDTO.java
package microservicos.servicoinscricoes.dto;

import microservicos.servicoinscricoes.domain.model.Inscricao;

public class InscricaoResponseDTO {
    private Inscricao inscricao;
    private boolean notificacaoEnviada;
    private String aviso;

    public InscricaoResponseDTO(Inscricao inscricao, boolean notificacaoEnviada, String aviso) {
        this.inscricao = inscricao;
        this.notificacaoEnviada = notificacaoEnviada;
        this.aviso = aviso;
    }

    public Inscricao getInscricao() { return inscricao; }
    public boolean isNotificacaoEnviada() { return notificacaoEnviada; }
    public String getAviso() { return aviso; }
}