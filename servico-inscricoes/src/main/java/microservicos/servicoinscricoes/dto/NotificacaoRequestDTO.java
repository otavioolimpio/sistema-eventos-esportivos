// dto/NotificacaoRequestDTO.java
package microservicos.servicoinscricoes.dto;

public class NotificacaoRequestDTO {
    private String canal;
    private String destinatario;
    private String assunto;
    private String mensagem;

    public NotificacaoRequestDTO(String canal, String destinatario, String assunto, String mensagem) {
        this.canal = canal;
        this.destinatario = destinatario;
        this.assunto = assunto;
        this.mensagem = mensagem;
    }

    public String getCanal() { return canal; }
    public String getDestinatario() { return destinatario; }
    public String getAssunto() { return assunto; }
    public String getMensagem() { return mensagem; }
}