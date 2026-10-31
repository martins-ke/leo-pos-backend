package com.leo.pos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LeoApplication {

	public static void main(String[] args) {
		SpringApplication.run(LeoApplication.class, args);
	}

}
