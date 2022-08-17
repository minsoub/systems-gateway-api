package com.bithumbsystems.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
public class AwsProperties {

  @Value("${cloud.aws.region.static}")
  private String region;

  @Value("${cloud.aws.ssm.endpoint}")
  private String ssmEndPoint;

  @Value("${cloud.aws.param-store.prefix}")
  private String prefix;

  @Value("${cloud.aws.param-store.kms-name}")
  private String paramStoreKmsName;

  @Value("${cloud.aws.param-store.message-name}")
  private String paramStoreMessageName;

  private String sqlUrl;
  private String kmsKey;
}
