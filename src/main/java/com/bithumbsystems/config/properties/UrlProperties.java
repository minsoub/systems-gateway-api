package com.bithumbsystems.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "sites")
@ConfigurationPropertiesScan
public class UrlProperties {
    private String authUrl;
    private String smartAdminUrl;
    private String smartAdminLrcUrl;
    private String smartAdminCpcUrl;
    private String lrcAppUrl;
    private String cpcAppUrl;
    private String lrcTokenIgnore;
}
