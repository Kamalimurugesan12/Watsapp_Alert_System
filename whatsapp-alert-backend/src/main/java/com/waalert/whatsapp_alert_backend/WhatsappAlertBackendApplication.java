package com.waalert.whatsapp_alert_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WhatsappAlertBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(WhatsappAlertBackendApplication.class, args);
	}

}
