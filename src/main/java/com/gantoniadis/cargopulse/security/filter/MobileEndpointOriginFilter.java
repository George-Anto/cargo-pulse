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

    @Value("${security.mobile-secret-header-key}")
    private String mobileSecretHeaderKey;

    @Value("${security.mobile-secret-header-value}")
    private String mobileSecretHeaderValue;

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

                sendErrorResponse(response, requestPath, "This endpoint is only accessible from the mobile application.");
                return;
            }

            // If the custom header does not include the secret key, block access
            if (!mobileSecretHeaderValue.equals(request.getHeader(mobileSecretHeaderKey))) {
                log.warn("Blocked access to mobile endpoint {} due to missing/invalid custom secret header.", requestPath);
                sendErrorResponse(response, requestPath, "Missing or invalid mobile client secret.");
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
        // If a request has a standard HTTP/HTTPS origin, it must be the whitelisted mobile URL
        if (origin != null && (origin.startsWith("http://") || origin.startsWith("https://"))) {
            // We only allow if it exactly matches the mobile app URL
            return mobileAppUrl.equals(origin);
        }

        // If the request has no Origin header (null/empty), we assume it's the mobile app.
        // This is the WEAKEST point, but sometimes necessary for native mobile apps.
        if (origin == null || origin.isEmpty()) {
            return true;
        }

        // We allow custom non-web schemes only if they match the mobile app's scheme
        return mobileAppUrl.startsWith(origin);
    }

    private void sendErrorResponse(HttpServletResponse response, String requestPath, String message) throws IOException {
        HttpStatus status = HttpStatus.FORBIDDEN;

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                requestPath
        );

        // Set the response headers and status
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // Write the JSON response body
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}