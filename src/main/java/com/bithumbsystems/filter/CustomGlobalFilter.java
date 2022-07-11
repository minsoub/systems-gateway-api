package com.bithumbsystems.filter;

import com.amazonaws.services.sqs.model.InvalidMessageContentsException;
import com.bithumbsystems.config.constant.GlobalConstant;
import com.bithumbsystems.filter.sender.AwsSQSSender;
import com.bithumbsystems.model.request.AuditLogRequest;
import com.bithumbsystems.utils.CommonUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
    ServerHttpRequest serverHttpRequest = exchange.getRequest();
    HttpHeaders httpHeaders = serverHttpRequest.getHeaders();
    if (serverHttpRequest.getMethod() == HttpMethod.GET) {
      AuditLogRequest auditRequest = getAuditLogRequest(httpHeaders, serverHttpRequest);
      log.debug(auditRequest.toString());
      sqsSender.sendMessage(auditRequest, auditRequest.getPath());
      return chain.filter(exchange.mutate().build());
    } else {
      ServerHttpRequestDecorator loggingServerHttpRequestDecorator = new ServerHttpRequestDecorator(
          exchange.getRequest()) {

        @Override
        public Flux<DataBuffer> getBody() {
          return super.getBody().publishOn(Schedulers.boundedElastic()).doOnNext(dataBuffer -> {
            try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
              Channels.newChannel(byteArrayOutputStream)
                  .write(dataBuffer.asByteBuffer().asReadOnlyBuffer());
              var requestBody = IOUtils.toString(byteArrayOutputStream.toByteArray(),
                  StandardCharsets.UTF_8.name());

              AuditLogRequest auditRequest = getAuditLogRequest(httpHeaders, serverHttpRequest);
              auditRequest.setRequestBody(requestBody);
              log.debug(auditRequest.toString());
              sqsSender.sendMessage(auditRequest, auditRequest.getPath());
            } catch (IOException e) {
              log.error(e.getLocalizedMessage());
            } catch (InvalidMessageContentsException e) {
              log.warn("InvalidMessageContentsException: {}", e.getLocalizedMessage());
              AuditLogRequest auditRequest = getAuditLogRequest(httpHeaders, serverHttpRequest);
              sqsSender.sendMessage(auditRequest, auditRequest.getPath());
            }
          });
        }
      };
      return chain.filter(exchange.mutate().request(loggingServerHttpRequestDecorator).build());
    }
  }


  private AuditLogRequest getAuditLogRequest(HttpHeaders httpHeaders,
      ServerHttpRequest serverHttpRequest) {
    String userIp = httpHeaders.get(GlobalConstant.USER_IP) != null ?
        Objects.requireNonNull(httpHeaders.get(GlobalConstant.USER_IP)).get(0)
        : CommonUtil.getUserIp(serverHttpRequest);

    String token = httpHeaders.get(GlobalConstant.TOKEN_HEADER) != null ?
        Objects.requireNonNull(httpHeaders.get(GlobalConstant.TOKEN_HEADER)).get(0) : "";

    String siteId = httpHeaders.get(GlobalConstant.SITE_ID) != null ?
        Objects.requireNonNull(httpHeaders.get(GlobalConstant.SITE_ID)).get(0) : "";

    String mySiteId = httpHeaders.get(GlobalConstant.MY_SITE_ID) != null ?
        Objects.requireNonNull(httpHeaders.get(GlobalConstant.MY_SITE_ID)).get(0) : "";

    return AuditLogRequest.builder()
        .userIp(userIp)
        .mySiteId(mySiteId)
        .siteId(siteId)
        .token(token)
        .path(String.valueOf(serverHttpRequest.getPath()))
        .queryParams(URLEncoder.encode(String.valueOf(serverHttpRequest.getQueryParams()),
            StandardCharsets.UTF_8))
        .uri(URLEncoder.encode(String.valueOf(serverHttpRequest.getURI()), StandardCharsets.UTF_8))
        .path(String.valueOf(serverHttpRequest.getPath()))
        .method(String.valueOf(serverHttpRequest.getMethod()))
        .referer(String.valueOf(httpHeaders.get("referer") == null ? "" : Objects.requireNonNull(
            httpHeaders.get("referer")).get(0)))
        .userAgent(
            String.valueOf(httpHeaders.get("User-Agent") == null ? "" : Objects.requireNonNull(
                httpHeaders.get("User-Agent")).get(0)))
        .message(String.valueOf(httpHeaders))
        .build();
  }
}

