package com.bithumbsystems.exception;

import com.bithumbsystems.model.enums.ErrorCode;

public class GatewayStatusException extends RuntimeException {
    public GatewayStatusException(String errorCode) {
        super(errorCode);
    }
}
