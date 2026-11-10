package microservicos.serviconotificacoes.api;

import microservicos.serviconotificacoes.dominio.Canal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corpo do POST /notificacoes (contrato da interface INotificacaoService). */
public record NotificacaoRequest(
        @NotNull(message = "canal é obrigatório (EMAIL ou DISCORD)")
        Canal canal,

        @NotBlank(message = "destinatario é obrigatório")
        @Size(max = 200, message = "destinatario deve ter no máximo 200 caracteres")
        String destinatario,

        @Size(max = 200, message = "assunto deve ter no máximo 200 caracteres")
        String assunto,

        @NotBlank(message = "mensagem é obrigatória")
        @Size(max = 5000, message = "mensagem deve ter no máximo 5000 caracteres")
        String mensagem) {
}
