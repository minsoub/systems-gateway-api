package com.bithumbsystems.filter;

import com.bithumbsystems.config.constant.GlobalConstant;
import com.bithumbsystems.filter.sender.AwsSQSSender;
import com.bithumbsystems.model.request.AuditLogRequest;
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
            log.info("requestBody:" + requestBody);
            log.info(String.valueOf(exchange.getRequest().getHeaders().get(GlobalConstant.USER_IP)));
            log.info(String.valueOf(exchange.getRequest().getHeaders().get(GlobalConstant.SITE_ID)));
            log.info(String.valueOf(exchange.getRequest().getHeaders().get(GlobalConstant.TOKEN_HEADER)));
            log.info(String.valueOf(exchange.getRequest().getURI()));
            log.info(String.valueOf(exchange.getRequest().getPath()));
            log.info(String.valueOf(exchange.getRequest().getMethod()));
            log.info(String.valueOf(exchange.getRequest().getQueryParams()));
            log.info(String.valueOf(exchange.getRequest().getHeaders().get("referer")));
            log.info(String.valueOf(exchange.getRequest().getHeaders().get("User-Agent")));
            log.info(String.valueOf(requestBody));
            var auditRequest = AuditLogRequest.builder()
                .userIp(String.valueOf(exchange.getRequest().getHeaders().get(GlobalConstant.USER_IP)))
                .siteId(String.valueOf(exchange.getRequest().getHeaders().get(GlobalConstant.SITE_ID)))
                .token(String.valueOf(exchange.getRequest().getHeaders().get(GlobalConstant.TOKEN_HEADER)))
                .uri(URLEncoder.encode(String.valueOf(exchange.getRequest().getURI()), StandardCharsets.UTF_8.name()))
                .path(String.valueOf(exchange.getRequest().getPath()))
                .method(String.valueOf(exchange.getRequest().getMethod()))
                .queryParams(URLEncoder.encode(String.valueOf(exchange.getRequest().getQueryParams()), StandardCharsets.UTF_8.name()))
                .referer(String.valueOf(exchange.getRequest().getHeaders().get("referer")))
                .userAgent(String.valueOf(exchange.getRequest().getHeaders().get("User-Agent")))
                .message(String.valueOf(exchange.getRequest().getHeaders()))
                .requestBody(requestBody)
                .build();
            sqsSender.sendMessage(auditRequest, auditRequest.getPath());
          } catch (IOException e) {
            log.error(e.getLocalizedMessage());
          }
        });
      }
    };

//    ServerHttpResponseDecorator loggingServerHttpResponseDecorator = new ServerHttpResponseDecorator(exchange.getResponse()) {
//      String responseBody = "";
//      @Override
//      public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
//        Mono<DataBuffer> buffer = Mono.from(body);
//        return super.writeWith(buffer.doOnNext(dataBuffer -> {
//          try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
//            Channels.newChannel(byteArrayOutputStream).write(dataBuffer.asByteBuffer().asReadOnlyBuffer());
//            responseBody = IOUtils.toString(byteArrayOutputStream.toByteArray(), "UTF-8");
//            log.info("responseBody:" + responseBody);
//          } catch (Exception e) {
//
//          }
//        }));
//      }
//    };

    return chain.filter(exchange.mutate().request(loggingServerHttpRequestDecorator).build());
  }
}
