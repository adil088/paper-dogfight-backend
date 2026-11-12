package com.paperdogfight.dto;

/** Body of every 4xx response. */
public record ErrorResponse(int status, String code, String message) {
}
