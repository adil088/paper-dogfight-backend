package com.paperdogfight.service;

/** Machine-readable reasons a request is rejected, each with the HTTP status it maps to. */
public enum ErrorCode {
    GAME_NOT_FOUND(404),
    INVALID_TOKEN(401),
    NOT_YOUR_TURN(409),
    ALREADY_ROLLED(409),
    ROLL_REQUIRED(409),
    GAME_NOT_ACTIVE(409),
    GAME_NOT_FINISHED(409),
    GAME_FULL(409),
    INVALID_JET(400),
    JET_NOT_OWNED(403),
    JET_DESTROYED(400),
    JET_CANNOT_ADVANCE(400),
    ROLL_TOO_HIGH(400);

    private final int httpStatus;

    ErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
