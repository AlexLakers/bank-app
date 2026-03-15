package com.alex.bank.ui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;


@SpringBootApplication
@EnableDiscoveryClient

public class BankUiApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankUiApplication.class, args);
	}

}
