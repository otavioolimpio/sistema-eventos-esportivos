package microservicos.serviconotificacoes.adapter;

import microservicos.serviconotificacoes.dominio.Canal;
import microservicos.serviconotificacoes.dominio.Mensagem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * PADRÃO ADAPTER — adaptador concreto para o Discord.
 *
 * Classe adaptada (Adaptee): a API de Webhooks do Discord, que espera um POST
 * HTTP com um JSON no formato {"content": "..."} (máximo de 2000 caracteres).
 * Este adaptador converte a {@link Mensagem} interna para esse formato.
 *
 * O canal de destino é definido pela URL do webhook (variável DISCORD_WEBHOOK_URL);
 * o destinatário é citado no texto da mensagem.
 */
@Component
public class DiscordWebhookAdapter implements INotificador {

    private static final int LIMITE_DISCORD = 2000;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper;
    private final String webhookUrl;

    public DiscordWebhookAdapter(ObjectMapper objectMapper,
                                 @Value("${notificacao.discord.webhook-url:}") String webhookUrl) {
        this.objectMapper = objectMapper;
        this.webhookUrl = webhookUrl;
    }

    @Override
    public Canal canal() {
        return Canal.DISCORD;
    }

    @Override
    public void enviar(Mensagem mensagem) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            throw new FalhaEnvioException("Webhook do Discord não configurado (variável DISCORD_WEBHOOK_URL).");
        }

        HttpRequest requisicao = HttpRequest.newBuilder(URI.create(webhookUrl))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(montarCorpo(mensagem)))
                .build();

        try {
            HttpResponse<String> resposta = httpClient.send(requisicao, HttpResponse.BodyHandlers.ofString());
            if (resposta.statusCode() >= 300) {
                throw new FalhaEnvioException("Discord respondeu HTTP " + resposta.statusCode() + ": " + resposta.body());
            }
        } catch (IOException e) {
            throw new FalhaEnvioException("Falha de comunicação com o Discord: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FalhaEnvioException("Envio ao Discord interrompido.", e);
        }
    }

    /** Converte a mensagem interna para o JSON esperado pelo webhook do Discord. */
    private String montarCorpo(Mensagem mensagem) {
        StringBuilder texto = new StringBuilder();
        if (mensagem.assunto() != null && !mensagem.assunto().isBlank()) {
            texto.append("**").append(mensagem.assunto()).append("**\n");
        }
        texto.append("Para: ").append(mensagem.destinatario()).append("\n");
        texto.append(mensagem.conteudo());

        String conteudo = texto.length() > LIMITE_DISCORD
                ? texto.substring(0, LIMITE_DISCORD - 3) + "..."
                : texto.toString();

        try {
            return objectMapper.writeValueAsString(Map.of("content", conteudo));
        } catch (JsonProcessingException e) {
            throw new FalhaEnvioException("Não foi possível montar a mensagem para o Discord.", e);
        }
    }
}
