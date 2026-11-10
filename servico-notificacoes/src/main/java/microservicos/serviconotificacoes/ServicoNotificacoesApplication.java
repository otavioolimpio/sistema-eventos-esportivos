package microservicos.serviconotificacoes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Serviço de Notificações do Sistema de Gerenciamento de Eventos de E-Sports.
 *
 * Implementa o UC07 - Enviar Notificação, caso de uso de inclusão acionado pelo
 * UC02 (envio de credenciais) e pelo UC06 (resultado da inscrição).
 *
 * Padrões GoF aplicados:
 *  - Singleton: {@link microservicos.serviconotificacoes.singleton.NotificationClientManager}
 *  - Adapter:   {@link microservicos.serviconotificacoes.adapter.INotificador} e suas implementações
 */
@SpringBootApplication
public class ServicoNotificacoesApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServicoNotificacoesApplication.class, args);
    }
}
