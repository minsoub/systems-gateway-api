package com.bithumbsystems.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
public class AuditLogRequest {

  private String userIp;
  private String siteId;
  private String token;
  private String uri;
  private String path;
  private String method;
  private String queryParams;
  private String referer;
  private String userAgent;
  private String requestBody;
  private String message;
}
