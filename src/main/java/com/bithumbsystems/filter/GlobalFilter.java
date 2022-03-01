package com.bithumbsystems.filter;

import com.bithumbsystems.config.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class GlobalFilter extends AbstractGatewayFilterFactory<Config> {

    public GlobalFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(final Config config) {
        return (exchange, chain) -> {
          log.info("GlobalFilter baseMessage: {}", config.getBaseMessage());

          if (config.isPreLooger()) {
              log.info("GlobalFilter Start: {}", exchange.getRequest());
          }

          return chain.filter(exchange).then(Mono.fromRunnable(()-> {
              if (config.isPostLogger()) {
                  log.info("GlobalFilter End: {}", exchange.getResponse());
              }
          }));
        };
    }
}
