package microservicos.servicoinscricoes.domain.model;

import jakarta.persistence.*;
import microservicos.servicoinscricoes.domain.enums.StatusInscricao;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_inscricoes")
public class Inscricao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long equipeId;

    @Column(nullable = false)
    private Long eventoId;

    @Column(nullable = false)
    private Long chefeEquipeId;

    private Long organizadorAvaliadorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusInscricao status;

    private String justificativa;

    private LocalDateTime dataSolicitacao;
    private LocalDateTime dataAvaliacao;

    public Inscricao() {
        this.status = StatusInscricao.PENDENTE;
        this.dataSolicitacao = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEquipeId() { return equipeId; }
    public void setEquipeId(Long equipeId) { this.equipeId = equipeId; }

    public Long getEventoId() { return eventoId; }
    public void setEventoId(Long eventoId) { this.eventoId = eventoId; }

    public Long getChefeEquipeId() { return chefeEquipeId; }
    public void setChefeEquipeId(Long chefeEquipeId) { this.chefeEquipeId = chefeEquipeId; }

    public Long getOrganizadorAvaliadorId() { return organizadorAvaliadorId; }
    public void setOrganizadorAvaliadorId(Long organizadorAvaliadorId) { this.organizadorAvaliadorId = organizadorAvaliadorId; }

    public StatusInscricao getStatus() { return status; }
    public void setStatus(StatusInscricao status) { this.status = status; }

    public String getJustificativa() { return justificativa; }
    public void setJustificativa(String justificativa) { this.justificativa = justificativa; }

    public LocalDateTime getDataSolicitacao() { return dataSolicitacao; }
    public void setDataSolicitacao(LocalDateTime dataSolicitacao) { this.dataSolicitacao = dataSolicitacao; }

    public LocalDateTime getDataAvaliacao() { return dataAvaliacao; }
    public void setDataAvaliacao(LocalDateTime dataAvaliacao) { this.dataAvaliacao = dataAvaliacao; }
}