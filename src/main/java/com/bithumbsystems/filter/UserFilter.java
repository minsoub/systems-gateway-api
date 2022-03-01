package com.bithumbsystems.filter;

import com.bithumbsystems.config.Config;
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

            if (config.isPreLooger()) {
                log.info("UserFilter Start: {}", exchange.getRequest());
            }

            return chain.filter(exchange).then(Mono.fromRunnable(()-> {
                if (config.isPostLogger()) {
                    log.info("UserFilter End: {}", exchange.getResponse());
                }
            }));
        };
    }
}
