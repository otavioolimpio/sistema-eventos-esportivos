// dto/SolicitarInscricaoRequestDTO.java
package microservicos.servicoinscricoes.dto;

import jakarta.validation.constraints.NotNull;

public class SolicitarInscricaoRequestDTO {
    @NotNull(message = "equipeId e obrigatorio")
    private Long equipeId;

    @NotNull(message = "eventoId e obrigatorio")
    private Long eventoId;

    @NotNull(message = "chefeId e obrigatorio")
    private Long chefeId;

    public Long getEquipeId() { return equipeId; }
    public void setEquipeId(Long equipeId) { this.equipeId = equipeId; }
    public Long getEventoId() { return eventoId; }
    public void setEventoId(Long eventoId) { this.eventoId = eventoId; }
    public Long getChefeId() { return chefeId; }
    public void setChefeId(Long chefeId) { this.chefeId = chefeId; }
}