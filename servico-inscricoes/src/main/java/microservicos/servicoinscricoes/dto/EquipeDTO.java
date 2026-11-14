package microservicos.servicoinscricoes.dto;

public class EquipeDTO {
    private Long id;
    private String nome;
    private String modalidade;
    private Long chefeId;
    private UsuarioDTO chefeEquipe;

    public Long resolverChefeId() {
        if (chefeId != null) return chefeId;
        return chefeEquipe != null ? chefeEquipe.getId() : null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getModalidade() { return modalidade; }
    public void setModalidade(String modalidade) { this.modalidade = modalidade; }

    public Long getChefeId() { return chefeId; }
    public void setChefeId(Long chefeId) { this.chefeId = chefeId; }

    public UsuarioDTO getChefeEquipe() { return chefeEquipe; }
    public void setChefeEquipe(UsuarioDTO chefeEquipe) { this.chefeEquipe = chefeEquipe; }
}