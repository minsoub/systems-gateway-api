package com.bithumbsystems.config;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.bithumbsystems.config.properties.AwsProperties;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsAsyncClient;

@Slf4j
@Getter
@Setter
@Configuration
@Profile("dev|prod|eks-dev")
public class AwsConfig {

  private final AwsProperties awsProperties;
  @Value("${cloud.aws.credentials.profile-name}")
  private String profileName;
  private KmsAsyncClient kmsAsyncClient;
  @Value("${spring.profiles.active:}")
  private String activeProfiles;

  public AwsConfig(AwsProperties awsProperties) {
    this.awsProperties = awsProperties;
  }

  @Bean
  public AmazonSQSAsync amazonSQS() {
    return AmazonSQSAsyncClientBuilder.standard()
        .withRegion(awsProperties.getRegion())
        .build();
  }

  @PostConstruct
  public void init() {
    kmsAsyncClient = KmsAsyncClient.builder()
        .region(Region.of(awsProperties.getRegion()))
        .build();
  }
}

