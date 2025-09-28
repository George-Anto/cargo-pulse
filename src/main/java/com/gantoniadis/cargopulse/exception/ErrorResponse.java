package com.gantoniadis.cargopulse.exception;

import java.time.LocalDateTime;

// Standard error response format
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {}
