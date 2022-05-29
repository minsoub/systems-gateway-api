package com.bithumbsystems.filter;

import com.bithumbsystems.config.Config;
import com.bithumbsystems.config.constant.GlobalConstant;
import com.bithumbsystems.exception.GatewayException;
import com.bithumbsystems.exception.GatewayExceptionHandler;
import com.bithumbsystems.model.enums.ErrorCode;
import com.bithumbsystems.request.TokenRequest;
import com.bithumbsystems.utils.IPv4ValidatorRegex;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class ApiFilter extends AbstractGatewayFilterFactory<Config> {

    @Value("${spring.client.authUrl}")
    private String authUrl;
    @Value("${spring.client.smartAdminUrl}")
    private String smartAdminUrl;
    @Value("${spring.client.lrcAppUrl}")
    private String lrcAppUrl;
    @Value("${spring.client.cpcAppUrl}")
    private String cpcAppUrl;

    @Autowired
    private WebClient webClient;

    public ApiFilter() {
        super(Config.class);
        //this.webClient = webClient;
    }

    @Bean
    public ErrorWebExceptionHandler exceptionHandler() {
        return new GatewayExceptionHandler();
    }

    @Override
    public GatewayFilter apply(final Config config) {
        return (exchange, chain) -> {
            log.info("ApiFilter called...");
            log.info("ApiFilter baseMessage: {}", config.getBaseMessage());

            if (config.isPreLooger()) {
                log.info("ApiFilter Start: {}", exchange.getRequest());
            }

            ServerHttpRequest request = exchange.getRequest();
            log.debug("validation check start");
            log.debug("header => {}", request.getHeaders());
            // 사용자 IP check
            if (!request.getHeaders().containsKey(GlobalConstant.USER_IP)) {
                throw new GatewayException(ErrorCode.INVALID_HEADER_USER_IP);
            }
            // IP Validation check
            String userIp = request.getHeaders().get(GlobalConstant.USER_IP).toString();
            if (!StringUtils.hasLength(userIp)) {
                throw new GatewayException(ErrorCode.INVALID_HEADER_USER_IP);
            }
//            if (!IPv4ValidatorRegex.isValid(userIp) ) {
//                log.debug("userIp invalidate");
//                return handleUnAuthorized(exchange);
//            }
            // 사이트 코드 체크
            if (!request.getHeaders().containsKey(GlobalConstant.SITE_ID)) {
                throw new GatewayException(ErrorCode.INVALID_HEADER_SITE_ID);
            }
            // 사이트 코드에 따른 Authorization check
            String site_id = request.getHeaders().get(GlobalConstant.SITE_ID).toString();
            if (!StringUtils.hasLength(site_id)) {
                throw new GatewayException(ErrorCode.INVALID_HEADER_SITE_ID);
            }
            log.debug("site_id => {}", site_id);
            String url = null;
            if (site_id.equals(GlobalConstant.CPC_SITE_ID)) {   // 투자보호 센터 : No Token
                log.debug("cpc_site_id ");

                return chain.filter(exchange);
            }else { // mng, lrc
                // Request Header 검증 : Token
                if (!request.getHeaders().containsKey(GlobalConstant.TOKEN_HEADER)) {
                    throw new GatewayException(ErrorCode.INVALID_HEADER_TOKEN);
                    //return handleUnAuthorized(exchange);   // 401 Error
                }
                String token = request.getHeaders().get(GlobalConstant.TOKEN_HEADER).toString()
                        .substring(GlobalConstant.BEARER.length()).trim();

                log.debug("token => {}", token);
                // Token 검증
                HttpHeaders headers = new HttpHeaders();
                headers.putAll(exchange.getRequest().getHeaders());


                TokenRequest req = TokenRequest.builder()
                        .site_id(site_id)
                        .user_ip(userIp)
                        .token(token)
                        .build();

                return webClient.method(HttpMethod.POST)
                        //.headers { it.addAll(headers) }
                        .uri("/api/v1/authorize")
                        .accept(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(req))
                        .retrieve()
                        .bodyToMono(String.class)
                        .onErrorResume(error -> {
                            //handleUnAuthorized(exchange);
                            log.debug("error => {}", error);
                            throw new RuntimeException("error => " + error);
                            //return Mono.error(new RuntimeException("error => "+error));
                        })
                        .flatMap(result -> {
                            log.debug("success result => {}", result);
                            log.debug("exchange.getRequest => {}", exchange.getRequest().getURI());

                            //ServerHttpRequest req = exchange.getRequest().mutate().uri("abc").build();

                            return chain.filter(exchange).then(Mono.fromRunnable(()-> {
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
