package microservicos.servicoinscricoes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ServicoInscricoesApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServicoInscricoesApplication.class, args);
    }
}