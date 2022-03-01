package com.bithumbsystems.config;

import lombok.Getter;

@Getter
public class Config {
    private String baseMessage;
    private boolean preLooger;
    private boolean postLogger;

    public Config(String baseMessage, boolean preLooger, boolean postLogger) {
        this.baseMessage = baseMessage;
        this.preLooger = preLooger;
        this.postLogger = postLogger;
    }
}
