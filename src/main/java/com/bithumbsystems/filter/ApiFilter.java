package com.bithumbsystems.filter;

import com.bithumbsystems.config.Config;
import com.bithumbsystems.config.constant.GlobalConstant;
import com.bithumbsystems.exception.GatewayException;
import com.bithumbsystems.exception.GatewayExceptionHandler;
import com.bithumbsystems.exception.GatewayStatusException;
import com.bithumbsystems.model.enums.ErrorCode;
import com.bithumbsystems.request.TokenRequest;
import com.bithumbsystems.utils.CommonUtil;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
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

  public WebClient getWebClient(String url) {
    ConnectionProvider provider = ConnectionProvider.builder("fixed")
        .maxIdleTime(Duration.ofSeconds(20))
        .maxLifeTime(Duration.ofSeconds(600))
        .pendingAcquireTimeout(Duration.ofSeconds(60))
        .evictInBackground(Duration.ofSeconds(120)).build();

    HttpClient httpClient = HttpClient.create(provider)
        .doOnConnected(
            conn -> conn.addHandlerLast(new ReadTimeoutHandler(60, TimeUnit.SECONDS))
                .addHandlerLast(new WriteTimeoutHandler(120, TimeUnit.SECONDS))
        ).compress(true).wiretap(true);

    return WebClient.builder()
        .baseUrl(url)
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
  }

  @Override
  public GatewayFilter apply(final Config config) {
    return (exchange, chain) -> {
      log.info("ApiFilter called...");
      log.info("ApiFilter baseMessage: {}", config.getBaseMessage());

      if (config.isPreLogger()) {
        log.info("ApiFilter Start: {}", exchange.getRequest());
      }

      ServerHttpRequest request = exchange.getRequest();
      log.debug("header => {}", request.getHeaders());
      // ????????? IP check
      String userIp = CommonUtil.getUserIp(request);
      log.debug("user IP => {}", userIp);

      String siteId = validateRequest(request);
      log.debug("site_id => {}", siteId);

      log.debug("properties url [lrc : {}, cp : {}, mng : {}", lrcAppUrl, cpcAppUrl, smartAdminUrl);
      log.debug("authUrl : {}", authUrl);
      AtomicReference<String> goUrl = getRedirectUrl(siteId);

      // Header??? user_ip??? ????????? ??????.
      log.debug(exchange.getRequest().getURI().toString());
      log.debug(exchange.getRequest().getURI().getHost());
      log.debug(exchange.getRequest().getURI().getPath());
      log.debug(exchange.getRequest().getURI().getRawQuery());
      log.debug(exchange.getRequest().getURI().getRawPath());

      String replaceUrl = goUrl.get() + exchange.getRequest().getURI().getPath();
      if (StringUtils.hasLength(exchange.getRequest().getURI().getQuery())) {
        replaceUrl += "?" + exchange.getRequest().getURI().getRawQuery();
      }
      log.debug("replaceUrl:" + replaceUrl);
      URI uri = URI.create(replaceUrl);
      ServerHttpRequest serverHttpRequest = exchange.getRequest().mutate()
          .headers(httpHeaders -> httpHeaders.add("user_ip", userIp))
          .uri(uri)
          .build();
      log.debug(">>> tokenIgnoreLrc: {}",
          tokenIgnoreLrc.contains(exchange.getRequest().getURI().getPath()));

      if (siteId.equals(GlobalConstant.CPC_SITE_ID)) {   // ???????????? ?????? : No Token
        return cpcTransfer(exchange, chain, serverHttpRequest);
      } else if (isLrcTransferWithoutAuth(exchange, siteId)) {
        return transferApiWithoutAuth(
            "lrc token ignore path:" + exchange.getRequest().getURI().getPath(),
            chain, exchange, serverHttpRequest);
      } else { // mng, lrc
        // Request Header ?????? : Token
        TokenRequest req = getTokenRequest(request, userIp, siteId);

        return checkAuthorization(req)
            .flatMap(result -> {
              log.debug("success result => {}", result);
              log.debug("exchange.getRequest => {}", exchange.getRequest().getURI());
              return transferApi(config, exchange, chain, serverHttpRequest);
            });
      }
    };
  }

  private boolean isLrcTransferWithoutAuth(ServerWebExchange exchange, String siteId) {
    return siteId.equals(GlobalConstant.LRC_SITE_ID) && (
        tokenIgnoreLrc.contains(exchange.getRequest().getURI().getPath())
            || (exchange.getRequest().getURI().getPath()).indexOf("/api/v1/lrc/user/join/valid")
            == 0
            || (exchange.getRequest().getURI().getPath()).indexOf(
            "/api/v1/lrc/user/password/reset/info") == 0
    );
  }

  private Mono<Void> transferApi(Config config, ServerWebExchange exchange,
      GatewayFilterChain chain, ServerHttpRequest serverHttpRequest) {
    return chain.filter(exchange.mutate().request(serverHttpRequest).build())
        .doOnError(e -> {
          log.error(e.getMessage());
          if (e instanceof org.springframework.web.server.ResponseStatusException) {
            String httpStatusText = String.valueOf(((ResponseStatusException) e).getStatus());
            log.debug(">> ResponseStatusException:{}",
                httpStatusText);  // >> ResponseStatusException:504 GATEWAY_TIMEOUT
            throw new GatewayStatusException(httpStatusText);
          } else {
            throw new GatewayException(ErrorCode.SERVER_RESPONSE_ERROR);
          }
        })
        .then(Mono.fromRunnable(() -> {
          if (config.isPostLogger()) {
            log.info("ApiFilter End: {}", exchange.getResponse());
          }
        }));
  }

  private Mono<String> checkAuthorization(TokenRequest req) {
    return getWebClient(authUrl).mutate().build()
        .method(HttpMethod.POST)
        .uri("/api/v1/authorize")
        .accept(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(req))
        .retrieve()
        .onStatus(
            httpStatus -> httpStatus != HttpStatus.OK,
            clientResponse -> clientResponse.createException()
                .flatMap(
                    it -> {
                      if (it.getStatusCode().equals(HttpStatus.CONFLICT)) {
                        return Mono.error(new GatewayException(ErrorCode.USER_ALREADY_LOGIN));
                      } else if(it.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
                        return Mono.error(new GatewayException(ErrorCode.AUTHORIZATION_FAIL));
                      } else {
                        return Mono.error(new GatewayException(ErrorCode.EXPIRED_TOKEN));
                      }
                    }))
        .bodyToMono(String.class)
        .doOnError(error -> {
          log.error("error {}", error.getMessage());
          if(error.getMessage().equals(ErrorCode.USER_ALREADY_LOGIN.toString())) {
            throw new GatewayException(ErrorCode.USER_ALREADY_LOGIN);
          } else if(error.getMessage().equals(ErrorCode.AUTHORIZATION_FAIL.toString())) {
            throw new GatewayException(ErrorCode.AUTHORIZATION_FAIL);
          } else {
            throw new GatewayException(ErrorCode.EXPIRED_TOKEN);
          }
        });
  }

  private TokenRequest getTokenRequest(ServerHttpRequest request, String userIp, String siteId) {
    if (!request.getHeaders().containsKey(GlobalConstant.TOKEN_HEADER)) {
      throw new GatewayException(ErrorCode.INVALID_HEADER_TOKEN);
    }
    String token = Objects.requireNonNull(
            request.getHeaders().getFirst(GlobalConstant.TOKEN_HEADER))
        .substring(GlobalConstant.BEARER.length())
        .trim();

    String role = request.getHeaders().getFirst(GlobalConstant.ACTIVE_ROLE);

    log.debug("token => {}", token);
    // Token ??????
    TokenRequest req = TokenRequest.builder()
        .requestUri(request.getURI().getPath())
        .method(request.getMethod())
        .siteId(siteId)
        .userIp(userIp)
        .token(token)
        .activeRole(role)
        .build();

    log.debug("token data => {}", req);
    return req;
  }

  private Mono<Void> transferApiWithoutAuth(String exchange, GatewayFilterChain chain,
      ServerWebExchange exchange1, ServerHttpRequest serverHttpRequest) {
    log.debug(exchange);
    return chain.filter(exchange1.mutate().request(serverHttpRequest).build()).doOnError(e -> {
      log.error(e.getMessage());
      throw new GatewayException(ErrorCode.SERVER_RESPONSE_ERROR);
    });
  }

  private Mono<Void> cpcTransfer(ServerWebExchange exchange, GatewayFilterChain chain,
      ServerHttpRequest serverHttpRequest) {
    return transferApiWithoutAuth("cpc_site_id ", chain, exchange, serverHttpRequest);
  }

  private AtomicReference<String> getRedirectUrl(String siteId) {
    AtomicReference<String> goUrl = new AtomicReference<>("");

    // Redirect URI
    if (siteId.equals(GlobalConstant.LRC_SITE_ID)) {
      goUrl.set(lrcAppUrl);
    } else if (siteId.equals(GlobalConstant.CPC_SITE_ID)) {
      goUrl.set(cpcAppUrl);
    } else {
      goUrl.set(smartAdminUrl);
    }
    return goUrl;
  }

  private String validateRequest(ServerHttpRequest request) {
    log.debug("validation check start");
    // ????????? ?????? ??????
    if (!request.getHeaders().containsKey(GlobalConstant.SITE_ID)) {
      log.debug(">>>>> SITE ID NOT CONTAINS <<<<<");
      log.debug(">>>>>HEADER => {}", request.getHeaders());
      log.debug(">>>>>URI => {}", request.getURI());
      throw new GatewayException(ErrorCode.INVALID_HEADER_SITE_ID);
    }
    // ????????? ????????? ?????? Authorization check
    String siteId = request.getHeaders().getFirst(GlobalConstant.SITE_ID);
    if (!StringUtils.hasLength(siteId)) {
      log.debug(">>>>> SITE ID NOT FOUND <<<<<");
      log.debug(">>>>> header => {}", request.getHeaders());
      log.debug(">>>>> URI => {}", request.getURI());
      log.debug(">>>>> siteId => {}", siteId);
      throw new GatewayException(ErrorCode.INVALID_HEADER_SITE_ID);
    }
    return siteId;
  }
}
