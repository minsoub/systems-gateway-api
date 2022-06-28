package com.bithumbsystems.config;

import lombok.Getter;

@Getter
public class Config {
    private final String baseMessage;
    private final boolean preLogger;
    private final boolean postLogger;

    public Config(String baseMessage, boolean preLogger, boolean postLogger) {
        this.baseMessage = baseMessage;
        this.preLogger = preLogger;
        this.postLogger = postLogger;
    }
}
