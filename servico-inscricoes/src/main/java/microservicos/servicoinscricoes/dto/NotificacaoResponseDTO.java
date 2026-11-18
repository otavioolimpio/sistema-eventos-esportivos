// dto/NotificacaoResponseDTO.java
package microservicos.servicoinscricoes.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Resposta do POST /notificacoes (contrato da INotificacaoService).
 * O Servico de Notificacoes responde 201 mesmo quando o envio falha (RN03 do UC07);
 * quem diz se a mensagem saiu e o campo status (ENVIADA ou FALHA).
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
