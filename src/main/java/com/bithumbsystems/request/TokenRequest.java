package com.bithumbsystems.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpMethod;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(value = SnakeCaseStrategy.class)
public class TokenRequest {
    private String token;
    private HttpMethod method;
    private String requestUri;
    private String userIp;
    private String siteId;
    private String mySiteId;
    private String activeRole;
}
