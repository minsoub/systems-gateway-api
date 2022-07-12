package com.bithumbsystems.filter;

import com.bithumbsystems.config.Config;
import com.bithumbsystems.config.constant.GlobalConstant;
import com.bithumbsystems.exception.GatewayException;
import com.bithumbsystems.exception.GatewayExceptionHandler;
import com.bithumbsystems.model.enums.ErrorCode;
import com.bithumbsystems.request.TokenRequest;
import com.bithumbsystems.utils.CommonUtil;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Slf4j
@Component
public class ApiFilter extends AbstractGatewayFilterFactory<Config> {

    @Value("${sites.auth-url}")
    private String authUrl;
    @Value("${sites.smart-admin-url}")
    private String smartAdminUrl;
    @Value("${sites.lrc-app-url}")
    private String lrcAppUrl;
    @Value("${sites.cpc-app-url}")
    private String cpcAppUrl;
    @Value("#{'${sites.lrc-token-ignore}'.split(',')}")
    private List<String> tokenIgnoreLrc;

    public ApiFilter() {
        super(Config.class);
    }

    @Bean
    public ErrorWebExceptionHandler exceptionHandler() {
        return new GatewayExceptionHandler();
    }

    public WebClient getWebClient(String url)
    {
        ConnectionProvider provider = ConnectionProvider.builder("fixed")
            .maxIdleTime(Duration.ofSeconds(20))
            .maxLifeTime(Duration.ofSeconds(60))
            .pendingAcquireTimeout(Duration.ofSeconds(60))
            .evictInBackground(Duration.ofSeconds(120))
            .metrics(true)
            .build();

        HttpClient httpClient = HttpClient.create(provider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            .doOnConnected(
                conn -> conn.addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
                    .addHandlerLast(new WriteTimeoutHandler(60, TimeUnit.SECONDS))
            ).compress(true).wiretap(true);

        return WebClient.builder()
            .baseUrl(url)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }

    @Override
    public GatewayFilter apply(final Config config) {
        AtomicReference<String> goUrl = new AtomicReference<>("");
        return (exchange, chain) -> {
            log.info("ApiFilter called...");
            log.info("ApiFilter baseMessage: {}", config.getBaseMessage());

            if (config.isPreLogger()) {
                log.info("ApiFilter Start: {}", exchange.getRequest());
            }

            ServerHttpRequest request = exchange.getRequest();
            log.debug("validation check start");
            log.debug("header => {}", request.getHeaders());
            // 사용자 IP check
            String userIp = CommonUtil.getUserIp(request);
            log.debug("user IP => {}", userIp);

            // 사이트 코드 체크
            if (!request.getHeaders().containsKey(GlobalConstant.SITE_ID)) {
                log.debug(">>>>> SITE ID NOT CONTAINS <<<<<");
                log.debug(">>>>>HEADER => {}", request.getHeaders());
                log.debug(">>>>>URI => {}", request.getURI());
                throw new GatewayException(ErrorCode.INVALID_HEADER_SITE_ID);
            }
            // 사이트 코드에 따른 Authorization check
            String siteId = request.getHeaders().getFirst(GlobalConstant.SITE_ID);
            if (!StringUtils.hasLength(siteId)) {
                log.debug(">>>>> SITE ID NOT FOUND <<<<<");
                log.debug(">>>>> header => {}", request.getHeaders());
                log.debug(">>>>> URI => {}", request.getURI());
                log.debug(">>>>> siteId => {}", siteId);
                throw new GatewayException(ErrorCode.INVALID_HEADER_SITE_ID);
            }
            log.debug("site_id => {}", siteId);

            log.debug("properties url [lrc : {}, cp : {}, mng : {}", lrcAppUrl, cpcAppUrl, smartAdminUrl);
            log.debug("tt : {}", authUrl);  // urlConfig.getAuthUrl());

            // Redirect URI
            if (siteId.equals(GlobalConstant.LRC_SITE_ID)) {
                goUrl.set(lrcAppUrl);
            }else if(siteId.equals(GlobalConstant.CPC_SITE_ID)) {
                goUrl.set(cpcAppUrl);
            }else {
                goUrl.set(smartAdminUrl);
            }

            // Header에 user_ip를 넣어야 한다.
            log.debug(exchange.getRequest().getURI().toString());
            log.debug(exchange.getRequest().getURI().getHost());
            log.debug(exchange.getRequest().getURI().getPath());
            log.debug(exchange.getRequest().getURI().getRawQuery()); //  .getQueryParams().toString());
            log.debug(exchange.getRequest().getURI().getRawPath());

            String replaceUrl = goUrl.get() + exchange.getRequest().getURI().getPath();
            if (StringUtils.hasLength(exchange.getRequest().getURI().getQuery())) {
                replaceUrl += "?"+exchange.getRequest().getURI().getRawQuery();
            }
            log.debug("replaceUrl:"+ replaceUrl);
            URI uri = URI.create(replaceUrl);
            ServerHttpRequest serverHttpRequest = exchange.getRequest().mutate()
                    .headers(httpHeaders -> {
                        httpHeaders.add("user_ip", userIp);
                    })
                    .uri(uri)
                    .build();
            log.debug(">>> tokenIgnoreLrc:", tokenIgnoreLrc.contains(exchange.getRequest().getURI().getPath()));

            if (siteId.equals(GlobalConstant.CPC_SITE_ID)) {   // 투자보호 센터 : No Token
                log.debug("cpc_site_id ");
                return chain.filter(exchange.mutate().request(serverHttpRequest).build()).then(Mono.fromRunnable(() -> {
                    if (config.isPostLogger()) {
                        log.info("ApiFilter End: {}", exchange.getResponse());
                    }
                }));

                //return chain.filter(exchange);
            }else if (siteId.equals(GlobalConstant.LRC_SITE_ID)
                    && (tokenIgnoreLrc.contains(exchange.getRequest().getURI().getPath()) || (exchange.getRequest().getURI().getPath()).indexOf("/api/v1/lrc/user/join/valid") == 0)
            ){
                log.debug("lrc token ignore path:"+ exchange.getRequest().getURI().getPath());
                return chain.filter(exchange.mutate().request(serverHttpRequest).build()).then(Mono.fromRunnable(() -> {
                    if (config.isPostLogger()) {
                        log.info("ApiFilter End: {}", exchange.getResponse());
                    }
                }));
            }else { // mng, lrc
                // Request Header 검증 : Token
                if (!request.getHeaders().containsKey(GlobalConstant.TOKEN_HEADER)) {
                    throw new GatewayException(ErrorCode.INVALID_HEADER_TOKEN);
                    //return handleUnAuthorized(exchange);   // 401 Error
                }
                String token = request.getHeaders().getFirst(GlobalConstant.TOKEN_HEADER).toString()
                        .substring(GlobalConstant.BEARER.length()).trim();

                log.debug("token => {}", token);
                // Token 검증
                TokenRequest req = TokenRequest.builder()
                        .site_id(siteId)
                        .user_ip(userIp)
                        .token(token)
                        .build();

                log.debug("token data => {}", req);

                return getWebClient(authUrl).mutate().build()
                        .method(HttpMethod.POST)
                        //.headers { it.addAll(headers) }
                        .uri("/api/v1/authorize")
                        .accept(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(req))
                        .retrieve()
                        .bodyToMono(String.class)
                        .onErrorResume(error -> {
                            //handleUnAuthorized(exchange);
                            log.debug("onErrorResume ==========================");
                            log.debug("error => {}", error);
                            throw new GatewayException(ErrorCode.SERVER_RESPONSE_ERROR);
                            //throw new RuntimeException("error => " + error);
                            //return Mono.error(new RuntimeException("error => "+error));
                        })
                        .flatMap(result -> {
                            log.debug("success result => {}", result);
                            log.debug("exchange.getRequest => {}", exchange.getRequest().getURI());

                            //ServerHttpRequest req = exchange.getRequest().mutate().uri("abc").build();   // uri change
                            return chain.filter(exchange.mutate().request(serverHttpRequest).build()).then(Mono.fromRunnable(()-> {
                                if (config.isPostLogger()) {
                                    log.info("ApiFilter End: {}", exchange.getResponse());
                                }
                            }));
                        });
                        //.subscribe(result -> log.info("result => {}", result));
            }
        };
    }

    private Mono<Void> handleUnAuthorized(ServerWebExchange exchange) {
        log.debug("handleUnAuthorized error");
        ServerHttpResponse response = exchange.getResponse();

        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }
}
