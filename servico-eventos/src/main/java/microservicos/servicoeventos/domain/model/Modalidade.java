package microservicos.servicoeventos.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_modalidades")
public class Modalidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String nome;

    private String descricao;
    private Integer minJogadores;
    private Integer maxJogadores;

    public Modalidade() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Integer getMinJogadores() { return minJogadores; }
    public void setMinJogadores(Integer minJogadores) { this.minJogadores = minJogadores; }

    public Integer getMaxJogadores() { return maxJogadores; }
    public void setMaxJogadores(Integer maxJogadores) { this.maxJogadores = maxJogadores; }
}