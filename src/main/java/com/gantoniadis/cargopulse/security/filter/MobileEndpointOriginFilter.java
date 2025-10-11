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
 * Filter to restrict access to mobile-specific endpoints based on origin.
 * This filter ensures that mobile endpoints are only accessible from the configured mobile app origin,
 * preventing access from web browsers, Swagger UI, or other unauthorized sources.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MobileEndpointOriginFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Value("${host.url.mobile-app}")
    private String mobileAppUrl;

    private static final String MOBILE_ENDPOINT_PATTERN = "/api/auth/";
    private static final String MOBILE_SUFFIX = "/mobile";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        
        // Check if this is a mobile-specific endpoint
        if (isMobileEndpoint(requestPath)) {
            String origin = request.getHeader("Origin");
            String referer = request.getHeader("Referer");
            
            log.debug("Mobile endpoint access attempt - Path: {}, Origin: {}, Referer: {}", 
                     requestPath, origin, referer);
            
            // Block access if origin is not from mobile app
            if (!isValidMobileOrigin(origin)) {
                log.warn("Blocked access to mobile endpoint {} from unauthorized origin: {} (referer: {})", 
                        requestPath, origin, referer);

                HttpStatus status = HttpStatus.FORBIDDEN;

                // 2. Build the ErrorResponse object to match the GlobalExceptionHandler format
                ErrorResponse errorResponse = new ErrorResponse(
                        LocalDateTime.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        "This endpoint is only accessible from the mobile application",
                        request.getRequestURI()
                );

                // 3. Set the response headers and status
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                // 4. Write the JSON response body
                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Check if the request path is for a mobile-specific endpoint
     */
    private boolean isMobileEndpoint(String requestPath) {
        return requestPath != null && 
               requestPath.contains(MOBILE_ENDPOINT_PATTERN) && 
               requestPath.endsWith(MOBILE_SUFFIX);
    }

    /**
     * Validate if the origin is from the authorized mobile app
     */
    private boolean isValidMobileOrigin(String origin) {
        // For mobile apps, the origin might be null or the mobile app URL
        // Mobile apps typically don't send Origin header, or send the app scheme
        
        // If origin is null (typical for mobile apps), allow it
        if (origin == null || origin.isEmpty()) {
            return true;
        }
        
        // If origin matches the configured mobile app URL, allow it
        if (mobileAppUrl.equals(origin)) {
            return true;
        }
        
        // Block if origin is from web browsers (http/https schemes from different domains)
        // This is likely a web browser request, block it
        return !origin.startsWith("http://") && !origin.startsWith("https://");
        
        // For custom app schemes (like app://), allow them
    }
}