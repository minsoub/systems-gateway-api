package com.bithumbsystems.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
@Getter
public enum ErrorCode {

    UNKNOWN_ERROR(999, "알 수 없는 에러가 발생하였습니다. 운영자에게 문의 주시기 바랍니다!!!"),
    INVALID_HEADER_USER_IP(901, "Header 정보가 유효하지 않습니다!!(Not found user_ip)"),
    INVALID_HEADER_SITE_ID(902,"Haeder 정보가 유효하지 않습니다!!(Not found site_id)"),
    INVALID_HEADER_TOKEN(903, "Token 정보가 잘 못되었습니다!!!"),

    SERVER_RESPONSE_ERROR(904, "API 서버에서 에러가 발생하였습니다!!!");

    private final int code;
    private final String message;

    public static ErrorCode findByCode(int code) {
        return Arrays.stream(ErrorCode.values())
                .filter(errorCode -> errorCode.hasCode(code))
                .findAny()
                .orElse(UNKNOWN_ERROR);
    }

    public static ErrorCode findByName(String message) {
        return Arrays.stream(ErrorCode.values())
                .filter(errorName -> errorName.hasMessage(message))
                .findAny()
                .orElse(UNKNOWN_ERROR);
    }

    public boolean hasCode(int code) {
        return Arrays.stream(ErrorCode.values()).anyMatch(errorCode -> errorCode.code == code);
    }
    public boolean hasMessage(String message) {
        return Arrays.stream(ErrorCode.values()).anyMatch(errorCode -> errorCode.message.equals(message));
    }
}
