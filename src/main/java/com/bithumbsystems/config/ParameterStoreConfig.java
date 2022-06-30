package com.bithumbsystems.config;

import static com.bithumbsystems.config.constant.GlobalConstant.SQS_URL;

import com.bithumbsystems.config.constant.GlobalConstant;
import com.bithumbsystems.config.properties.AwsProperties;
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
@Configuration
@Profile("dev|prod|eks-dev")
public class ParameterStoreConfig {

    private SsmClient ssmClient;

    private final AwsProperties awsProperties;

    @Value("${cloud.aws.credentials.profile-name}")
    private String profileName;

    @PostConstruct
    public void init() {

        log.debug("config store [prefix] => {}", awsProperties.getPrefix());

        this.ssmClient = SsmClient.builder()
            .region(Region.of(awsProperties.getRegion()))
            .build();

        // KMS Parameter Key
        this.awsProperties.setKmsKey(getParameterValue(awsProperties.getParamStoreKmsName().trim(), GlobalConstant.KMS_ALIAS_NAME));
        this.awsProperties.setSqlUrl(getParameterValue(awsProperties.getParamStoreMessageName().trim(), SQS_URL));
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
