package com.bithumbsystems.model.response;

import com.bithumbsystems.exception.ErrorData;
import lombok.*;

@Data
public class ErrorResponse {
    private final ErrorData error;
    private final String result;
    private final String data;

    @Builder
    public ErrorResponse(ErrorData data) {
        this.error = data;
        this.result = "FAIL";
        this.data = null;
    }

    @Override
    public String toString() {
        //    private String errorCodeMaker(int errorCode, String msg) {
//        return "{\"result\":\"FAIL\", \"error\": { \"code\":" + errorCode +", \"message\":\""+msg+"\"}, \"data\": null}";
//    }
        return "{\"result\":\""+result+"\", \"error\": { \"code\":" + error.getCode() +", \"message\":\""+error.getMessage()+"\"}, \"data\": null}";
    }
}
