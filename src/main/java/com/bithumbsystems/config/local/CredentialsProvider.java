package com.bithumbsystems.config.local;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;

@Slf4j
@Getter
@Configuration
@RequiredArgsConstructor
@Profile("local")
public class CredentialsProvider {

    @Value("${cloud.aws.credentials.profile-name}")
    private String profileName;

    @Bean
    public ProfileCredentialsProvider getProvider() {
        log.debug("CredentialsProvider profile name => {}", profileName);
        ProfileCredentialsProvider credentialsProvider = ProfileCredentialsProvider.builder()
            .profileName(profileName).build();

        log.debug("key id => {}", credentialsProvider.resolveCredentials().accessKeyId());
        return credentialsProvider;
    }

}