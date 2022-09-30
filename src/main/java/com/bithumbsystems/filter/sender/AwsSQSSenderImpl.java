package com.bithumbsystems.filter.sender;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.bithumbsystems.config.properties.AwsProperties;
import com.google.gson.Gson;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AwsSQSSenderImpl<T> implements AwsSQSSender<T> {

  private final AmazonSQSAsync amazonSQS;
  private final AwsProperties awsProperties;

  @Override
  public SendMessageResult sendMessage(T auditLogRequest, String groupId) {
    log.debug("AwsSQSSender Thread {}" , Thread.currentThread().getName());
    return amazonSQS.sendMessage(
        new SendMessageRequest(awsProperties.getSqsEndPoint() + "/" + awsProperties.getSqsQueueName(), new Gson().toJson(auditLogRequest))
            .withMessageGroupId(groupId)
            .withMessageDeduplicationId(UUID.randomUUID().toString())
        );
  }
}
