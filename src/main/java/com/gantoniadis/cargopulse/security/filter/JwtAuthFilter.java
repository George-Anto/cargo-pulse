package com.gantoniadis.cargopulse.security.filter;

import com.gantoniadis.cargopulse.security.service.JwtBlocklistService;
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
    private final JwtBlocklistService blocklistService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String AUTHORIZATION_HEADER_KEY = "Authorization";
        final String BEARER = "Bearer ";

        // 1. Check for OPTION calls early (CORS preflight)
        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader(AUTHORIZATION_HEADER_KEY);
        final String jwt;
        String username;

        // 2. If no header or bad prefix, let the chain continue unauthenticated
        if (authHeader == null ||!authHeader.startsWith(BEARER)) {
            log.debug("No JWT found in request. Allowing chain to continue unauthenticated.");
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(BEARER.length());

        // 3. Check for Blocklisted Token
        if (blocklistService.isTokenBlocklisted(jwt)) {
            log.warn("Unauthorized request made. Token is blocklisted.");
            filterChain.doFilter(request, response);
            return;
        }

        // 4. Extract Username and handle exceptions
        try {
            username = jwtService.extractUsername(jwt);
        } catch (JwtException e) {
            log.warn("JWT parsing failed: {}", e.getLocalizedMessage());
            // DO NOT set authentication. Let chain continue unauthenticated.
            filterChain.doFilter(request, response);
            return;
        }

        // 5. Authentication attempt (only runs if token is not blocked and username exists)
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails;

            try {
                userDetails = this.userDetailsService.loadUserByUsername(username);
            } catch (UsernameNotFoundException e) {
                log.warn("User not found: {}", username);
                filterChain.doFilter(request, response); // Continue unauthenticated
                return;
            }

            if (jwtService.isTokenValid(jwt, userDetails)) {
                // SUCCESS: Set the authentication context
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        jwt,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                log.warn("Token validation failed for user: {}", username);
            }
        }

        // 6. Continue the filter chain. If context was set (SUCCESS), the request proceeds.
        // If context was NOT set (FAILURE), the request proceeds to fail at FilterSecurityInterceptor,
        // which triggers the AuthenticationEntryPoint.
        filterChain.doFilter(request, response);
    }
}
