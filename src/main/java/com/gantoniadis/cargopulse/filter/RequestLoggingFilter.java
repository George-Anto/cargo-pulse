package com.gantoniadis.cargopulse.filter;

import org.springframework.lang.NonNull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // For the swagger requests, just log their details only in debug level
        if (request.getRequestURI().toLowerCase().contains("swagger")) {

            log.debug("Incoming request: method={}, uri={}, from={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getRemoteAddr());

            log.debug("Headers: {}", Collections.list(request.getHeaderNames()));

            filterChain.doFilter(request, response);

            log.debug("Response status={} for {}", response.getStatus(), request.getRequestURI());

            return;
        }

        log.info("Incoming request: method={}, uri={}, from={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr());

         log.info("Headers: {}", Collections.list(request.getHeaderNames()));

        filterChain.doFilter(request, response);

        log.info("Response status={} for {}", response.getStatus(), request.getRequestURI());
    }
}
