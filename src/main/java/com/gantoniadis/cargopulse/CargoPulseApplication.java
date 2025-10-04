package com.gantoniadis.cargopulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource("classpath:secrets.properties")
public class CargoPulseApplication {

	public static void main(String[] args) {
		SpringApplication.run(CargoPulseApplication.class, args);
	}

}
