package com.bithumbsystems.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRequest {
    private String token;
    private String user_ip;
    private String site_id;
}
