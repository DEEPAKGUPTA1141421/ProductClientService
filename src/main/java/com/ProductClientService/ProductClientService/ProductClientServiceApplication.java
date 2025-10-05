package com.ProductClientService.ProductClientService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

//import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
@EnableAsync
@EnableFeignClients
@EnableAspectJAutoProxy
public class ProductClientServiceApplication {

	public static void main(String[] args) {
		// Dotenv dotenv = Dotenv.configure()
		// .directory("./src/main/resources") // path to your .env file
		// .load();

		// // Set all env vars so Spring can use them
		// dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(),
		// entry.getValue()));
		SpringApplication.run(ProductClientServiceApplication.class, args);
	}

}
// niuhiuoujovjiouuufr89u
// niuhiuyhui hihiuhuih hihihiud hkhu hyhy