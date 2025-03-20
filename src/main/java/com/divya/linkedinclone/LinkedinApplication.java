package com.divya.linkedinclone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
@SpringBootApplication
@EnableJpaAuditing // Enable JPA auditing
public class LinkedinApplication {

	public static void main(String[] args) {
		SpringApplication.run(LinkedinApplication.class, args);
	}

}