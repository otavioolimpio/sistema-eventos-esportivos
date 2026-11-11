package microservicos.servicousuariosequipes.dto;

public record NotificacaoRequestDTO(
        String destinatario,
        String mensagem,
        String canal
) {}