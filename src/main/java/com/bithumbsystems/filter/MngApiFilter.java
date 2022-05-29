package com.bithumbsystems.filter;

import com.bithumbsystems.config.Config;
import com.bithumbsystems.config.constant.GlobalConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class MngApiFilter  extends AbstractGatewayFilterFactory<Config> {

    public MngApiFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(final Config config) {
        return (exchange, chain) -> {
            log.info("MngApiFilter baseMessage: {}", config.getBaseMessage());

            if (config.isPreLooger()) {
                log.info("MngApiFilter Start: {}", exchange.getRequest());
            }

            ServerHttpRequest request = exchange.getRequest();

            // Request Header 검증 : Token
            if (!request.getHeaders().containsKey(GlobalConstant.TOKEN_HEADER)) {
                return handleUnAuthorized(exchange);   // 401 Error
            }



            return chain.filter(exchange).then(Mono.fromRunnable(()-> {
                if (config.isPostLogger()) {
                    log.info("MngApiFilter End: {}", exchange.getResponse());
                }
            }));
        };
    }

    private Mono<Void> handleUnAuthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();

        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }
}
