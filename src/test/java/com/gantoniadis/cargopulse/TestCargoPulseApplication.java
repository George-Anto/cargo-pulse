package com.gantoniadis.cargopulse;

import org.springframework.boot.SpringApplication;

public class TestCargoPulseApplication {

	public static void main(String[] args) {
		SpringApplication.from(CargoPulseApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
