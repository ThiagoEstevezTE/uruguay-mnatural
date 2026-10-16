package uy.edu.um.proyectotic2026;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // para la tarea que cancela las reservas pendientes vencidas
public class Proyectotic2026Application {

	public static void main(String[] args) {
		SpringApplication.run(Proyectotic2026Application.class, args);
	}

}
