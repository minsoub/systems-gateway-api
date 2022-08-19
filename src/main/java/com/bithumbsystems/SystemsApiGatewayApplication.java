package com.bithumbsystems;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan("com.bithumbsystems.config")
@SpringBootApplication
public class SystemsApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SystemsApiGatewayApplication.class);
        app.addListeners(new ApplicationPidFileWriter());
        app.run(args);
    }
}
