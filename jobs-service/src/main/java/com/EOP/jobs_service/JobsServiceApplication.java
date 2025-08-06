package com.EOP.jobs_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {
		"com.EOP.jobs_service",
		"com.EOP.common_lib.common.security"
})
@EnableDiscoveryClient
@EnableCaching
public class JobsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobsServiceApplication.class, args);
	}

}
