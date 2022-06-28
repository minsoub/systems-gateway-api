package com.bithumbsystems.config.properties;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class UrlProperties {
    @Value("${spring.client.authUrl}")
    private String authUrl;
}
