package com.paperdogfight.controller;

import com.paperdogfight.dto.ErrorResponse;
import com.paperdogfight.service.GameRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns rule violations into clear JSON 4xx errors: {@code {status, code, message}}. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(GameRuleException.class)
    public ResponseEntity<ErrorResponse> handleRule(GameRuleException ex) {
        int status = ex.getCode().httpStatus();
        return ResponseEntity.status(status).body(new ErrorResponse(status, ex.getCode().name(), ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "BAD_REQUEST", "The request body is not valid JSON."));
    }
}
