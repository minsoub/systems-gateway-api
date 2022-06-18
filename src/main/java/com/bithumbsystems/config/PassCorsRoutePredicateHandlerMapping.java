package com.bithumbsystems.config;

import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.cloud.gateway.handler.FilteringWebHandler;
import org.springframework.cloud.gateway.handler.RoutePredicateHandlerMapping;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Log4j2
public class PassCorsRoutePredicateHandlerMapping extends RoutePredicateHandlerMapping {

    public PassCorsRoutePredicateHandlerMapping(FilteringWebHandler webHandler, RouteLocator routeLocator,
                                                GlobalCorsProperties globalCorsProperties, Environment environment) {
        super(webHandler, routeLocator, globalCorsProperties, environment);
    }

    @Override
    public Mono<Object> getHandler(ServerWebExchange exchange) {
        logger.info("[PassCorsRoutePredicateHandlerMapping] getHandler");
        return getHandlerInternal(exchange).map(handler -> {
            log.info(exchange.getLogPrefix() + "Mapped to " + handler);

            // CORS 체크 로직 제거

            return handler;
        });
    }

    @Bean
    @Primary
    public RoutePredicateHandlerMapping passCorsRoutePredicateHandlerMapping(
            FilteringWebHandler webHandler, RouteLocator routeLocator,
            GlobalCorsProperties globalCorsProperties, Environment environment) {
        return new PassCorsRoutePredicateHandlerMapping(webHandler, routeLocator,
                globalCorsProperties, environment);
    }
}