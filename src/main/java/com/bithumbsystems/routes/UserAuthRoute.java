package com.bithumbsystems.routes;

import com.bithumbsystems.config.Config;
import com.bithumbsystems.filter.ApiFilter;
import com.bithumbsystems.filter.AuthFilter;
import com.bithumbsystems.filter.UserFilter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Log4j2
@Configuration
public class UserAuthRoute {

  @Value("${spring.client.authUrl}")
  private String authUrl;
  @Value("${spring.client.smartAdminUrl}")
  private String smartAdminUrl;
  @Value("${spring.client.lrcAppUrl}")
  private String lrcAppUrl;
  @Value("${spring.client.cpcAppUrl}")
  private String cpcAppUrl;

  private final WebClient webClient;

  public UserAuthRoute(WebClient webClient) {
    this.webClient = webClient;
  }

  @Bean
  public RouteLocator routes(RouteLocatorBuilder builder) {
    log.debug(authUrl);
    return builder.routes()
        .route("user-service",   // 운영자 로그인 처리
            route -> route.path("/user/**")
                .filters(filter -> filter.rewritePath("/user/(?<path>.*)", "/api/v1/user/${path}")
                    .filter(new UserFilter().apply(
                        new Config("UserFilter apply", true, true))))
                .uri(authUrl)
        )
        .route("adm-service",   // 운영자 로그인 처리
            route -> route.path("/adm/**")
                .filters(filter -> filter.rewritePath("/adm/(?<path>.*)", "/api/v1/adm/${path}")
                    .filter(new UserFilter().apply(
                        new Config("UserFilter apply", true, true))))
                .uri(authUrl)
        )
        .route("auth-service",
            route -> route.path("/auth/**")
                .filters(filter -> filter.filter(
                    new AuthFilter().apply(new Config("AuthFilter apply", true, true))))
                .uri(authUrl)
        )
        .route("api-service",   // API 서비스 호출
            route -> route.path("/api/**")
                .filters(filter -> filter.filter(new ApiFilter(webClient).apply(
                    new Config("ApiFilter apply", true, true))))
                .uri(smartAdminUrl)
        ).build();
  }
}
