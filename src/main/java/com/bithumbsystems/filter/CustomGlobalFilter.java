package com.bithumbsystems.filter;

import com.bithumbsystems.config.constant.GlobalConstant;
import com.bithumbsystems.filter.sender.AwsSQSSender;
import com.bithumbsystems.model.request.AuditLogRequest;
import com.bithumbsystems.utils.CommonUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
public class CustomGlobalFilter implements GlobalFilter, Ordered {

  private final AwsSQSSender<AuditLogRequest> sqsSender;

  public CustomGlobalFilter(AwsSQSSender<AuditLogRequest> sqsSender) {
    this.sqsSender = sqsSender;
  }

  @Override
  public int getOrder() {
    return -1;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    log.debug("GlobalFilter START: {}", exchange.getResponse());
    log.debug("GlobalFilter Thread: {}", Thread.currentThread().getName());
    ServerHttpRequestDecorator loggingServerHttpRequestDecorator = new ServerHttpRequestDecorator(exchange.getRequest()) {
      String requestBody = "";
      @Override
      public Flux<DataBuffer> getBody() {
        return super.getBody().publishOn(Schedulers.boundedElastic()).doOnNext(dataBuffer -> {
          try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            Channels.newChannel(byteArrayOutputStream).write(dataBuffer.asByteBuffer().asReadOnlyBuffer());
            requestBody = IOUtils.toString(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8.name());
            ServerHttpRequest serverHttpRequest = exchange.getRequest();
            HttpHeaders httpHeaders = serverHttpRequest.getHeaders();
            String userIp = CommonUtil.getUserIp(serverHttpRequest);

            var auditRequest = AuditLogRequest.builder()
                .userIp(userIp)
                .siteId(String.valueOf(httpHeaders.get(GlobalConstant.SITE_ID)))
                .token(String.valueOf(httpHeaders.get(GlobalConstant.TOKEN_HEADER)))
                .uri(URLEncoder.encode(String.valueOf(serverHttpRequest.getURI()), StandardCharsets.UTF_8.name()))
                .path(String.valueOf(serverHttpRequest.getPath()))
                .method(String.valueOf(serverHttpRequest.getMethod()))
                .queryParams(URLEncoder.encode(String.valueOf(serverHttpRequest.getQueryParams()), StandardCharsets.UTF_8.name()))
                .referer(String.valueOf(httpHeaders.get("referer")))
                .userAgent(String.valueOf(httpHeaders.get("User-Agent")))
                .message(String.valueOf(httpHeaders))
                .requestBody(requestBody)
                .build();

            log.debug(auditRequest.toString());
            sqsSender.sendMessage(auditRequest, auditRequest.getPath());
          } catch (IOException e) {
            log.error(e.getLocalizedMessage());
          }
        });
      }
    };
    return chain.filter(exchange.mutate().request(loggingServerHttpRequestDecorator).build());
  }
}
