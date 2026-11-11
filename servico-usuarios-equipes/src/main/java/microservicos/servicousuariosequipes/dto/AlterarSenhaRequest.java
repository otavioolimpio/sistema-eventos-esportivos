package microservicos.servicousuariosequipes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlterarSenhaRequest(
        @NotBlank(message = "senhaAtual e obrigatoria") String senhaAtual,
        @NotBlank(message = "novaSenha e obrigatoria")
        @Size(min = 6, max = 100, message = "novaSenha deve ter entre 6 e 100 caracteres") String novaSenha
) {}
