// dto/RegistrarResultadoRequestDTO.java
package microservicos.servicoeventos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class RegistrarResultadoRequestDTO {
    @NotNull(message = "organizadorId e obrigatorio")
    private Long organizadorId;

    @NotNull(message = "equipeId e obrigatorio")
    private Long equipeId;

    @NotNull(message = "colocacao e obrigatoria")
    @Min(value = 1, message = "Colocacao deve ser maior ou igual a 1")
    private Integer colocacao;

    private Integer pontuacao;
    private String observacao;
    private Boolean encerrarEvento;

    public Long getOrganizadorId() { return organizadorId; }
    public void setOrganizadorId(Long organizadorId) { this.organizadorId = organizadorId; }
    public Long getEquipeId() { return equipeId; }
    public void setEquipeId(Long equipeId) { this.equipeId = equipeId; }
    public Integer getColocacao() { return colocacao; }
    public void setColocacao(Integer colocacao) { this.colocacao = colocacao; }
    public Integer getPontuacao() { return pontuacao; }
    public void setPontuacao(Integer pontuacao) { this.pontuacao = pontuacao; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public Boolean getEncerrarEvento() { return encerrarEvento; }
    public void setEncerrarEvento(Boolean encerrarEvento) { this.encerrarEvento = encerrarEvento; }
}