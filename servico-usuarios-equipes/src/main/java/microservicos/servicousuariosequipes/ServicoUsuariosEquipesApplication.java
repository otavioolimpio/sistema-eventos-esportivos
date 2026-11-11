package microservicos.servicousuariosequipes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ServicoUsuariosEquipesApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServicoUsuariosEquipesApplication.class, args);
	}

}