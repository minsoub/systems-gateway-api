package com.bithumbsystems.filter.sender;

import com.amazonaws.services.sqs.model.SendMessageResult;

public interface AwsSQSSender<T> {

  SendMessageResult sendMessage(T request, String groupId);
}
