package com.bithumbsystems.utils;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;

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
        if (ip.indexOf(",") != -1) {
            return ip.split(",")[0];
        } else {
            return ip;
        }
    }
}
