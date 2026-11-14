// dto/CriarModalidadeRequestDTO.java
package microservicos.servicoeventos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CriarModalidadeRequestDTO {
    @NotBlank(message = "nome e obrigatorio")
    private String nome;

    private String descricao;
    private Integer minJogadores;
    private Integer maxJogadores;

    @NotNull(message = "organizadorId e obrigatorio")
    private Long organizadorId;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Integer getMinJogadores() { return minJogadores; }
    public void setMinJogadores(Integer minJogadores) { this.minJogadores = minJogadores; }
    public Integer getMaxJogadores() { return maxJogadores; }
    public void setMaxJogadores(Integer maxJogadores) { this.maxJogadores = maxJogadores; }
    public Long getOrganizadorId() { return organizadorId; }
    public void setOrganizadorId(Long organizadorId) { this.organizadorId = organizadorId; }
}