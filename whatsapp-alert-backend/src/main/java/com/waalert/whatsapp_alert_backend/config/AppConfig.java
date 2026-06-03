package com.waalert.whatsapp_alert_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AppConfig {

    @Value("${whatsapp.api.url}")
    private String whatsappApiUrl;

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    /** Pre-configured WebClient pointed at Meta WhatsApp Cloud API. */
    @Bean("whatsAppWebClient")
    public WebClient whatsAppWebClient(WebClient.Builder builder) {
        return builder.baseUrl(whatsappApiUrl).build();
    }
}
