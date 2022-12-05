package com.bithumbsystems.config.local;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.bithumbsystems.config.properties.AwsProperties;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsAsyncClient;

@Slf4j
@Getter
@Setter
@Configuration
@RequiredArgsConstructor
@Profile("local|localstack")
public class LocalAwsConfig {

  @Value("${cloud.aws.credentials.profile-name}")
  private String profileName;

  private final AwsProperties awsProperties;

  private final CredentialsProvider credentialsProvider;
  private KmsAsyncClient kmsAsyncClient;
  private com.amazonaws.auth.profile.ProfileCredentialsProvider provider;

  @PostConstruct
  public void init() {
    provider = new com.amazonaws.auth.profile.ProfileCredentialsProvider(profileName);

    kmsAsyncClient = KmsAsyncClient.builder()
        .region(Region.of(awsProperties.getRegion()))
        .credentialsProvider(ProfileCredentialsProvider.create(profileName))
        .build();

  }

  @Bean
  public AmazonSQSAsync amazonSQS() {
    var endpointConfig = new AwsClientBuilder.EndpointConfiguration(
        awsProperties.getSqsEndPoint(),
        awsProperties.getRegion()
    );

    return AmazonSQSAsyncClientBuilder.standard()
//        .withRegion(awsProperties.getRegion())
        .withEndpointConfiguration(endpointConfig)
            .withCredentials(provider)
        .build();
  }
}