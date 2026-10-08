package com.ecommerce.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SbEcomApplication {

	public static void main(String[] args) {
		SpringApplication.run(SbEcomApplication.class, args);
	}

}
