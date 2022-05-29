package com.bithumbsystems.exception;

import com.bithumbsystems.model.enums.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Builder
@Data
@AllArgsConstructor
public class ErrorData {
    private final int code;
    private final String message;

    public ErrorData(ErrorCode errorCode) {
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

}
