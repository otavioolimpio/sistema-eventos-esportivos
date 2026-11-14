// dto/EventoDTO.java
package microservicos.servicoinscricoes.dto;

public class EventoDTO {
    private Long id;
    private String nome;
    private String modalidade;
    private Integer vagasDisponiveis;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getModalidade() { return modalidade; }
    public void setModalidade(String modalidade) { this.modalidade = modalidade; }
    public Integer getVagasDisponiveis() { return vagasDisponiveis; }
    public void setVagasDisponiveis(Integer vagasDisponiveis) { this.vagasDisponiveis = vagasDisponiveis; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}