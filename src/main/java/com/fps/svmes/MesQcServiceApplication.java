package com.fps.svmes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.fps.svmes.repositories.jpaRepo")
@EnableMongoRepositories(basePackages = "com.fps.svmes.repositories.mongoRepo")
@EnableFeignClients
@EnableScheduling
public class MesQcServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MesQcServiceApplication.class, args);
	}

}
