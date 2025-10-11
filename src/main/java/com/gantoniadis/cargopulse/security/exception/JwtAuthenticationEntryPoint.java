package com.gantoniadis.cargopulse.security.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gantoniadis.cargopulse.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        HttpStatus status = HttpStatus.UNAUTHORIZED;
        String authErrorMessage = authException.getMessage();

        // 1. Log the error
        log.warn("Authentication failure for '{}' endpoint: {} - {} and message: {}",
                request.getRequestURI(),
                status.value(),
                status.getReasonPhrase(),
                authErrorMessage);

        // 2. Build the ErrorResponse object to match the GlobalExceptionHandler format
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                authErrorMessage,
                request.getRequestURI()
        );

        // 3. Set the response headers and status
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // 4. Write the JSON response body
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
