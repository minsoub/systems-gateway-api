package com.bithumbsystems.exception;

import com.bithumbsystems.model.enums.ErrorCode;
import com.bithumbsystems.model.response.ErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Log4j2
public class GatewayExceptionHandler  implements ErrorWebExceptionHandler {
    private ErrorResponse errorResponse;
    @Autowired
    private ObjectMapper mapper;
    private int errorCode;
    private String errorMessage;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        log.warn("in GATEWAY Exeptionhandler : " + ex);
        ErrorData errorData;
        errorCode = 999;
        errorMessage = null;

        if (ex.getClass() == GatewayException.class) {
            GatewayException e = (GatewayException) ex;
            errorCode = ErrorCode.valueOf(e.getMessage()).getCode();
            errorMessage = ErrorCode.valueOf(e.getMessage()).getMessage();
            errorData = ErrorData.builder().code(errorCode).message(errorMessage).build();
        }else {
            errorData = new ErrorData(ErrorCode.UNKNOWN_ERROR);
        }

        byte[] bytes = new byte[0];  //  errorCodeMaker(errorCode, ex.getMessage().toString()).getBytes(StandardCharsets.UTF_8);
        try {
            bytes = (mapper.writeValueAsString(new ErrorResponse(errorData))).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().writeWith(Flux.just(buffer));
    }
}