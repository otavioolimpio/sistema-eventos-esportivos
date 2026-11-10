package microservicos.serviconotificacoes.config;

import microservicos.serviconotificacoes.adapter.INotificador;
import microservicos.serviconotificacoes.singleton.NotificationClientManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Ao subir a aplicação, registra todos os adaptadores (EmailAdapter,
 * DiscordWebhookAdapter...) na instância única do NotificationClientManager.
 *
 * O Spring encontra sozinho todas as classes que implementam INotificador;
 * por isso, um novo adaptador só precisa ser criado com @Component.
 */
@Configuration
public class NotificacaoConfig {

    @Bean
    public NotificationClientManager notificationClientManager(List<INotificador> adaptadores) {
        NotificationClientManager gerenciador = NotificationClientManager.getInstance();
        adaptadores.forEach(gerenciador::registrar);
        return gerenciador;
    }
}
