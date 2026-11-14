// dto/AvaliarInscricaoRequestDTO.java
package microservicos.servicoinscricoes.dto;

import jakarta.validation.constraints.NotNull;

public class AvaliarInscricaoRequestDTO {
    @NotNull(message = "organizadorId e obrigatorio")
    private Long organizadorId;

    @NotNull(message = "aprovada e obrigatorio (true ou false)")
    private Boolean aprovada;

    private String justificativa;

    public Long getOrganizadorId() { return organizadorId; }
    public void setOrganizadorId(Long organizadorId) { this.organizadorId = organizadorId; }
    public Boolean getAprovada() { return aprovada; }
    public void setAprovada(Boolean aprovada) { this.aprovada = aprovada; }
    public String getJustificativa() { return justificativa; }
    public void setJustificativa(String justificativa) { this.justificativa = justificativa; }
}