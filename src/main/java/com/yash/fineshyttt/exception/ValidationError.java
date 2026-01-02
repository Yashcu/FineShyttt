package com.yash.fineshyttt.exception;

public record ValidationError(
        String field,
        String message,
        Object rejectedValue
) {}
