package microservicos.servicoeventos.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_resultados_eventos")
public class ResultadoEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long eventoId;

    @Column(nullable = false)
    private Long equipeId;

    @Column(nullable = false)
    private Integer colocacao;

    private Integer pontuacao;
    private String observacao;

    public ResultadoEvento() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEventoId() { return eventoId; }
    public void setEventoId(Long eventoId) { this.eventoId = eventoId; }

    public Long getEquipeId() { return equipeId; }
    public void setEquipeId(Long equipeId) { this.equipeId = equipeId; }

    public Integer getColocacao() { return colocacao; }
    public void setColocacao(Integer colocacao) { this.colocacao = colocacao; }

    public Integer getPontuacao() { return pontuacao; }
    public void setPontuacao(Integer pontuacao) { this.pontuacao = pontuacao; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
}