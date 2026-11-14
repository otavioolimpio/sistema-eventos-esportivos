package microservicos.servicoeventos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ServicoEventosApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServicoEventosApplication.class, args);
    }
}