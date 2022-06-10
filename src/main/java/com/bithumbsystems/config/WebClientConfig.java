package com.bithumbsystems.config;

import com.bithumbsystems.config.properties.UrlConfig;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.annotation.PreDestroy;

@Slf4j
@Getter
@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final UrlConfig urlProperties;

    @Bean
    public WebClient webClient()
    {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .doOnConnected(
                        connection -> connection
                                .addHandlerLast(new ReadTimeoutHandler(10))
                                .addHandlerLast(new WriteTimeoutHandler(10))
                );

        ClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);
        log.debug("auth url => {}", urlProperties.getAuthUrl());

        WebClient webClient = WebClient.builder()
                .baseUrl(urlProperties.getAuthUrl())
                .clientConnector(connector)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        return webClient;
    }

//    @PreDestroy
//    public void stopClient() {
//
//    }
}
