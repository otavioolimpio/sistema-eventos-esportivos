package microservicos.servicousuariosequipes.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import microservicos.servicousuariosequipes.domain.enums.Modalidade;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_equipes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Equipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome da equipe nao pode ser vazio")
    private String nome;

    @NotNull(message = "Modalidade e obrigatoria")
    @Enumerated(EnumType.STRING)
    private Modalidade modalidade;

    @ManyToOne
    @JoinColumn(name = "chefe_id")
    private Usuario chefeEquipe;

    @ManyToMany
    @JoinTable(
            name = "tb_equipe_jogadores",
            joinColumns = @JoinColumn(name = "equipe_id"),
            inverseJoinColumns = @JoinColumn(name = "usuario_id")
    )
    @Builder.Default
    private List<Usuario> jogadores = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Modalidade getModalidade() {
        return modalidade;
    }

    public void setModalidade(Modalidade modalidade) {
        this.modalidade = modalidade;
    }

    public Usuario getChefeEquipe() {
        return chefeEquipe;
    }

    public void setChefeEquipe(Usuario chefeEquipe) {
        this.chefeEquipe = chefeEquipe;
    }

    public List<Usuario> getJogadores() {
        return jogadores;
    }

    public void setJogadores(List<Usuario> jogadores) {
        this.jogadores = jogadores;
    }
}