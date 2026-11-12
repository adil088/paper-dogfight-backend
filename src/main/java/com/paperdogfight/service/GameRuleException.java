package com.paperdogfight.service;

/** Thrown when a request breaks a game rule; the controller turns it into a clear 4xx response. */
public class GameRuleException extends RuntimeException {
    private final ErrorCode code;

    public GameRuleException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }
}
