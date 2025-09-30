package com.gantoniadis.cargopulse.security.filter;

import com.gantoniadis.cargopulse.security.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String AUTHORIZATION_HEADER_KEY = "Authorization";
        final String BEARER = "Bearer ";

        if (request.getServletPath().contains("/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader(AUTHORIZATION_HEADER_KEY);
        final String jwt;
        String username;
        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            filterChain.doFilter(request, response);
            return;
        }
        if (authHeader == null ||!authHeader.startsWith(BEARER)) {
            log.warn("Unauthorized request made. Method: {}, URI: {}, Remote Address: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getRemoteAddr()
            );
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(BEARER.length());

        try {
            username = jwtService.extractUsername(jwt);
        } catch (JwtException e) {
            log.warn("Could not extract username from JWT. {}", e.getLocalizedMessage());
            filterChain.doFilter(request, response);
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails;

            try {
                userDetails = this.userDetailsService.loadUserByUsername(username);
            } catch (UsernameNotFoundException e) {
                log.warn("Could not load User. {}", e.getLocalizedMessage());
                filterChain.doFilter(request, response);
                return;
            }

            if (jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        jwt,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                log.warn("Unauthorized request made. Invalid token. Username: {}, Method: {}, URI: {}, Remote Address: {}",
                        username, request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
            }
        } else {
            log.warn("Unauthorized request made. Missing or invalid token. Method: {}, URI: {}, Remote Address: {}",
                    request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
        }
        filterChain.doFilter(request, response);
    }
}
