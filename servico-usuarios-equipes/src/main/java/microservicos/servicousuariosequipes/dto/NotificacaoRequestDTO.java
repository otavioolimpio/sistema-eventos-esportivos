package microservicos.servicousuariosequipes.dto;

/** Corpo do POST /notificacoes, conforme servico-notificacoes/CONTRATO-API.md. */
public record NotificacaoRequestDTO(
        String canal,
        String destinatario,
        String assunto,
        String mensagem
) {}
