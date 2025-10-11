package com.gantoniadis.cargopulse.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gantoniadis.cargopulse.exception.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Filter to restrict access to web-specific endpoints based on origin.
 * This filter ensures that web endpoints are only accessible from web browsers (http/https origins),
 * preventing access from mobile applications or other unauthorized sources.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebEndpointOriginFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Value("${host.url.frontend}")
    private String frontendUrl;

    private static final String WEB_ENDPOINT_PATTERN = "/api/auth/";
    private static final String WEB_SUFFIX = "/web";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        
        // Check if this is a web-specific endpoint
        if (isWebEndpoint(requestPath)) {
            String origin = request.getHeader("Origin");
            String referer = request.getHeader("Referer");
            
            log.debug("Web endpoint access attempt - Path: {}, Origin: {}, Referer: {}", 
                     requestPath, origin, referer);
            
            // Block access if origin is not from a web browser
            if (!isValidWebOrigin(origin)) {
                log.warn("Blocked access to web endpoint {} from unauthorized origin: {} (referer: {})", 
                        requestPath, origin, referer);

                HttpStatus status = HttpStatus.FORBIDDEN;

                // Build the ErrorResponse object to match the GlobalExceptionHandler format
                ErrorResponse errorResponse = new ErrorResponse(
                        LocalDateTime.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        "This endpoint is only accessible from web browsers",
                        request.getRequestURI()
                );

                // Set the response headers and status
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                // Write the JSON response body
                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Check if the request path is for a web-specific endpoint
     */
    private boolean isWebEndpoint(String requestPath) {
        return requestPath != null && 
               requestPath.contains(WEB_ENDPOINT_PATTERN) && 
               requestPath.endsWith(WEB_SUFFIX);
    }

    /**
     * Validate if the origin is from an authorized web browser
     */
    private boolean isValidWebOrigin(String origin) {
        // Web endpoints should only be accessible from web browsers
        
        // If origin is null (typical for mobile apps or direct API calls), block it
        if (origin == null || origin.isEmpty()) {
            return false;
        }
        
        // If origin matches the configured web apps URL, allow it
        if (frontendUrl.equals(origin)) {
            return true;
        }
        
        // Allow if origin is from web browsers (http/https schemes)
        return origin.startsWith("http://") || origin.startsWith("https://");
        
        // Block custom app schemes (like app://) - these are likely mobile apps
    }
}