package com.indium.iplassignment;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching

public class IplAssignmentApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(IplAssignmentApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {

	}
}
