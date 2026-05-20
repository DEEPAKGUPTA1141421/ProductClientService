package com.ProductClientService.ProductClientService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableFeignClients
@EnableAspectJAutoProxy
public class ProductClientServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductClientServiceApplication.class, args);
	}
}