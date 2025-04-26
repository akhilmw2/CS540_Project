package com.example.authservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component  // ✅ THIS MAKES IT A SPRING BEAN
public class JwtTokenFilter extends OncePerRequestFilter {

    private final com.example.authservice.security.JwtUtils jwtUtils;

    public JwtTokenFilter(com.example.authservice.security.JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/login") ||
                path.startsWith("/api/auth/register") ||
                path.startsWith("/api/auth/confirm")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = jwtUtils.parseJwt(request);
        if (token != null && jwtUtils.validateJwtToken(token)) {
            String username = jwtUtils.getUserDetailsFromJwt(token);
            var authentication = jwtUtils.getAuthentication(username);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
