package com.bithumbsystems.config.local;

import static com.bithumbsystems.config.constant.GlobalConstant.KMS_ALIAS_NAME;
import static com.bithumbsystems.config.constant.GlobalConstant.SQS_URL;

import com.bithumbsystems.config.properties.AwsProperties;
import java.net.URI;
import javax.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

@Log4j2
@Data
@Profile("local|default")
@Configuration
public class LocalParameterStoreConfig {

    private SsmClient ssmClient;
    private final AwsProperties awsProperties;
    private final CredentialsProvider credentialsProvider;

    @Value("${spring.profiles.active:}")
    private String profileName;

    @PostConstruct
    public void init() {

        log.debug("config store [prefix] => {}", awsProperties.getPrefix());

        this.ssmClient = SsmClient.builder()
                .credentialsProvider(credentialsProvider.getProvider()) // 로컬에서 개발로 붙을때 사용
                .region(Region.of(awsProperties.getRegion()))
                .endpointOverride(URI.create(awsProperties.getSsmEndPoint()))
                .build();

        // KMS Parameter Key
        this.awsProperties.setKmsKey(getParameterValue(awsProperties.getParamStoreKmsName().trim(), KMS_ALIAS_NAME));
        this.awsProperties.setSqsUrl(getParameterValue(awsProperties.getParamStoreMessageName().trim(), SQS_URL));
    }

    protected String getParameterValue(String storeName, String type) {
        String parameterName = String.format("%s/%s_%s/%s", awsProperties.getPrefix(), storeName, profileName, type);

        GetParameterRequest request = GetParameterRequest.builder()
                .name(parameterName)
                .withDecryption(true)
                .build();

        GetParameterResponse response = this.ssmClient.getParameter(request);

        return response.parameter().value();
    }
}
