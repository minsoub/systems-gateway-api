package com.bithumbsystems.utils;

import java.time.Duration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

public class CommonUtil {

    public static String getUserIp(ServerHttpRequest request) {
        String ip = null;
         if (request.getHeaders().containsKey("X-Forwarded-For")) {
            ip = request.getHeaders().get("X-Forwarded-For").get(0);
        }
        if (!StringUtils.hasLength(ip)  &&  request.getHeaders().containsKey("Proxy-Client-IP")) {
            ip = request.getHeaders().get("Proxy-Client-IP").get(0);
        }
        if (!StringUtils.hasLength(ip)  &&  request.getHeaders().containsKey("WL-Proxy-Client-IP")) {
            ip = request.getHeaders().get("WL-Proxy-Client-IP").get(0);
        }
        if (!StringUtils.hasLength(ip)  &&  request.getHeaders().containsKey("HTTP_CLIENT_IP")) {
            ip = request.getHeaders().get("HTTP_CLIENT_IP").get(0);
        }
        if (!StringUtils.hasLength(ip)  &&  request.getHeaders().containsKey("HTTP_CLIENT_IP")) {
            ip = request.getHeaders().get("HTTP_CLIENT_IP").get(0);
        }
        if (!StringUtils.hasLength(ip)  &&  request.getHeaders().containsKey("X-Real-IP")) {
            ip = request.getHeaders().get("X-Real-IP").get(0);
        }
        if (!StringUtils.hasLength(ip)  &&  request.getHeaders().containsKey("X-Real-IP")) {
            ip = request.getHeaders().get("X-Real-IP").get(0);
        }
        if (!StringUtils.hasLength(ip)  &&  request.getHeaders().containsKey("X-Real-IP")) {
            ip = request.getRemoteAddress().getAddress().getAddress().toString();
        }

        return ip;
    }

    public static WebClient getWebClient(String url)
    {
        ConnectionProvider provider = ConnectionProvider.builder("fixed")
            .maxConnections(500)
            .maxIdleTime(Duration.ofSeconds(10))
            .maxLifeTime(Duration.ofSeconds(30))
            .pendingAcquireTimeout(Duration.ofSeconds(30))
            .lifo()
            .evictInBackground(Duration.ofSeconds(40)).build();

        return WebClient.builder()
            .baseUrl(url)
            .clientConnector(new ReactorClientHttpConnector(HttpClient.create(provider)))
            .build();
    }
}
