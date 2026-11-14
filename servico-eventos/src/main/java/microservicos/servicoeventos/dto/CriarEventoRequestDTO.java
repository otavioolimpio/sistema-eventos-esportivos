// dto/CriarEventoRequestDTO.java
package microservicos.servicoeventos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CriarEventoRequestDTO {
    @NotBlank(message = "nome e obrigatorio")
    private String nome;

    private String descricao;

    @NotBlank(message = "modalidade e obrigatoria")
    private String modalidade;

    @NotNull(message = "organizadorId e obrigatorio")
    private Long organizadorId;

    @NotNull(message = "vagasTotais e obrigatorio")
    @Min(value = 2, message = "O evento deve ter pelo menos 2 vagas")
    private Integer vagasTotais;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getModalidade() { return modalidade; }
    public void setModalidade(String modalidade) { this.modalidade = modalidade; }
    public Long getOrganizadorId() { return organizadorId; }
    public void setOrganizadorId(Long organizadorId) { this.organizadorId = organizadorId; }
    public Integer getVagasTotais() { return vagasTotais; }
    public void setVagasTotais(Integer vagasTotais) { this.vagasTotais = vagasTotais; }
}