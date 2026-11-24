package microservicos.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway do sistema (Spring Cloud Gateway).
 *
 * E a unica porta de entrada publicada (GATEWAY_PORT, no .env). Cada requisicao e encaminhada
 * para o microsservico certo pelo nome registrado no Eureka (lb://servico-xxx);
 * as rotas ficam em application.yml. Os servicos de negocio nao ficam expostos
 * fora da rede do Docker.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
