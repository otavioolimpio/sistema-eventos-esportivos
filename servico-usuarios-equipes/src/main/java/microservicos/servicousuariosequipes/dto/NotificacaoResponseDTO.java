package microservicos.servicousuariosequipes.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Resposta do POST /notificacoes. O servico responde 201 mesmo quando o envio falha;
 * o que diz se a mensagem saiu e o campo status (ENVIADA ou FALHA).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NotificacaoResponseDTO(
        String id,
        String status,
        String erro
) {
    public boolean foiEnviada() {
        return "ENVIADA".equals(status);
    }
}
