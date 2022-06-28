package com.bithumbsystems.routes;

import com.bithumbsystems.config.Config;
import com.bithumbsystems.config.properties.UrlProperties;
import com.bithumbsystems.filter.ApiFilter;
import com.bithumbsystems.filter.AuthFilter;
import com.bithumbsystems.filter.UserFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Log4j2
@Configuration
@RequiredArgsConstructor
public class UserAuthRoute {

//    @Value("${sites.auth-url}")
//    private String authUrl;
//    @Value("${sites.smart-admin-url}")
//    private String smartAdminUrl;
//    @Value("${sites.lrc-app-url}")
//    private String lrcAppUrl;
//    @Value("${sites.cpc-app-url}")
//    private String cpcAppUrl;

    private final UrlProperties urlProperties;
    private final UserFilter userFilter;
    private final AuthFilter authFilter;
    private final ApiFilter apiFilter;

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        log.debug(urlProperties.getAuthUrl());
        return builder.routes()
                .route("user-service",   // 운영자 로그인 처리
                        route -> route.path("/user/**")
                                .filters(filter -> filter.rewritePath("/user/(?<path>.*)", "/api/v1/user/${path}")
                                        //.filter(new UserFilter().apply(new Config("UserFilter apply", true, true))))
                                        .filter(userFilter.apply(new Config("UserFilter apply", true, true))))
                                .uri(urlProperties.getAuthUrl())
                )
                .route("adm-service",   // 운영자 로그인 처리
                        route -> route.path("/adm/**")
                                .filters(filter -> filter.rewritePath("/adm/(?<path>.*)", "/api/v1/adm/${path}")
                                        .filter(userFilter.apply(new Config("UserFilter apply", true, true))))
                                .uri(urlProperties.getAuthUrl())
                )
                .route("auth-service",
                        route ->route.path("/auth/**")
                                .filters(filter -> filter.filter(authFilter.apply(new Config("AuthFilter apply", true, true))))
                                .uri(urlProperties.getAuthUrl())
                )
                .route("api-service-cpc",   // 고객보호 API 서비스 호출
                        route -> route.path("/api/*/cpc/**")
                                    .filters(filter -> filter.filter(apiFilter.apply(new Config("CPC ApiFilter apply", true, true))))
                                .uri(urlProperties.getCpcAppUrl())
                )
                .route("api-service-lrc",   // 거래지원 API 서비스 호출
                        route -> route.path("/api/*/lrc/**")
                                .filters(filter -> filter.filter(apiFilter.apply(new Config("LRC ApiFilter apply", true, true))))
                                .uri(urlProperties.getLrcAppUrl())
                )
                .route("api-service-mng-lrc",   // API 서비스 호출 (LRC Smart Admin)
                        route -> route.path("/api/*/mng/lrc/**")
                                .filters(filter -> filter.filter(apiFilter.apply(new Config("LRC Smart Admin ApiFilter apply", true, true))))
                                .uri(urlProperties.getSmartAdminLrcUrl())

                )
                .route("api-service-mng-cpc",   // API 서비스 호출 (CP Smart Admin)
                        route -> route.path("/api/*/mng/cpc/**")
                                .filters(filter -> filter.filter(apiFilter.apply(new Config("LRC Smart Admin ApiFilter apply", true, true))))
                                .uri(urlProperties.getSmartAdminCpcUrl())

                )
                .route("api-service",   // API 서비스 호출 (Smart Admin)
                        route -> route.path("/api/**")
                                .filters(filter -> filter.filter(apiFilter.apply(new Config("ApiFilter apply", true, true))))
                                .uri(urlProperties.getSmartAdminUrl())

                )
                .build();
    }
}
