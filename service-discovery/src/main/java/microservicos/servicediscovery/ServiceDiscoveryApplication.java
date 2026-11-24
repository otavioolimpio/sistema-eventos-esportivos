package microservicos.servicediscovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Service Discovery do sistema (Eureka Server).
 *
 * Cada microsservico se registra aqui pelo seu spring.application.name
 * (servico-usuarios-equipes, servico-eventos, servico-inscricoes, servico-notificacoes).
 * Os clientes OpenFeign e o API Gateway consultam este registro para descobrir
 * onde cada servico esta, por isso nenhuma URL de outro servico fica fixa no codigo.
 *
 * Painel: http://localhost:<EUREKA_PORT> (porta definida no .env da raiz)
 */
@SpringBootApplication
@EnableEurekaServer
public class ServiceDiscoveryApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceDiscoveryApplication.class, args);
    }
}
