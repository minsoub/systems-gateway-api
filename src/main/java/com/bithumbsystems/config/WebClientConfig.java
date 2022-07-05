package com.bithumbsystems.config;

import com.bithumbsystems.config.properties.UrlProperties;
import java.time.Duration;
import javax.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Slf4j
@Getter
@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final UrlProperties urlProperties;

    @Bean
    public WebClient webClient()
    {
//        HttpClient httpClient = HttpClient.create()
//            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
//            .option(ChannelOption.SO_KEEPALIVE, true)
//            .doOnConnected(conn -> conn
//                .addHandlerLast(new ReadTimeoutHandler(10))
//                .addHandlerLast(new WriteTimeoutHandler(10)));
//        ClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);

//        return WebClient.builder()
//            .baseUrl(urlProperties.getAuthUrl())
//            .clientConnector(connector)
//            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
//            .build();

        ConnectionProvider provider = ConnectionProvider.builder("fixed")
            .maxConnections(500)
            .maxIdleTime(Duration.ofSeconds(10))
            .maxLifeTime(Duration.ofSeconds(30))
            .pendingAcquireTimeout(Duration.ofSeconds(30))
            .lifo()
            .evictInBackground(Duration.ofSeconds(40)).build();

        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(HttpClient.create(provider)))
            .build();

    }

    @PreDestroy
    public void stopClient() {

    }
}
