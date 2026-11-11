package microservicos.servicousuariosequipes.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import microservicos.servicousuariosequipes.domain.enums.TipoPerfil;

@Entity
@Table(name = "tb_usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome nao pode ser vazio")
    private String nome;

    @NotBlank(message = "Email nao pode ser vazio")
    @Email(message = "Email invalido")
    @Column(unique = true)
    private String email;

    @NotBlank(message = "CPF nao pode ser vazio")
    @Column(unique = true)
    private String cpf;

    @NotNull(message = "Perfil e obrigatorio")
    @Enumerated(EnumType.STRING)
    private TipoPerfil tipoPerfil;

    /** Hash BCrypt da senha. Nunca vai para o JSON. */
    @JsonIgnore
    @Column(name = "senha_hash")
    private String senhaHash;

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public TipoPerfil getTipoPerfil() {
        return tipoPerfil;
    }

    public void setTipoPerfil(TipoPerfil tipoPerfil) {
        this.tipoPerfil = tipoPerfil;
    }
}