package com.bithumbsystems.filter;

import com.bithumbsystems.config.Config;
import com.bithumbsystems.exception.GatewayException;
import com.bithumbsystems.model.enums.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class UserFilter extends AbstractGatewayFilterFactory<Config> {

    public UserFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(final Config config) {
        return (exchange, chain) -> {
            log.info("UserFilter baseMessage: {}", config.getBaseMessage());

            if (config.isPreLogger()) {
                log.info("UserFilter Start: {}", exchange.getRequest());
            }

            // Request Header 검증

            return chain.filter(exchange).doOnError(e -> {
                log.error(e.getMessage());
                throw new GatewayException(ErrorCode.SERVER_RESPONSE_ERROR);
                }).then(Mono.fromRunnable(()-> {
                if (config.isPostLogger()) {
                    log.info("UserFilter End: {}", exchange.getResponse());
                }
            }));
        };
    }
}
