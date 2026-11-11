package microservicos.servicousuariosequipes.domain.model;

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
}