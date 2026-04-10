package com.workhub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Tenant Context Filter
 * 
 * This filter ensures that the tenant context is properly cleaned up
 * after each request, preventing memory leaks in thread pools.
 * 
 * It runs with high priority to ensure cleanup happens even if exceptions occur.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class TenantContextFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        try {
            // Extract tenant ID from JWT if present
            String authHeader = request.getHeader("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String jwt = authHeader.substring(7);
                    Long tenantId = jwtUtil.extractTenantId(jwt);
                    
                    if (tenantId != null) {
                        TenantContext.setTenantId(tenantId);
                        log.debug("Tenant context initialized for request: {} (tenant: {})", 
                                request.getRequestURI(), tenantId);
                    }
                } catch (Exception e) {
                    log.warn("Failed to extract tenant ID from JWT: {}", e.getMessage());
                }
            }
            
            // Continue filter chain
            filterChain.doFilter(request, response);
            
        } finally {
            // CRITICAL: Always clear tenant context
            // This prevents memory leaks in servlet container thread pools
            TenantContext.clear();
            log.trace("Tenant context cleared for request: {}", request.getRequestURI());
        }
    }
}
