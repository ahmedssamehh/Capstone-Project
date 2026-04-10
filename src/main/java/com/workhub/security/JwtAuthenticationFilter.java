package com.workhub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter
 * 
 * Intercepts requests, extracts JWT token, validates it, and sets authentication context
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        try {
            final String authHeader = request.getHeader("Authorization");
            
            // Skip if no Authorization header or doesn't start with Bearer
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            try {
                // Extract token
                final String jwt = authHeader.substring(7);
                final String email = jwtUtil.extractEmail(jwt);
                final Long tenantId = jwtUtil.extractTenantId(jwt);

                // If email is present and no authentication exists
                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    
                    // Load user details
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                    // Validate token
                    if (jwtUtil.validateToken(jwt, userDetails)) {
                        
                        // IMPORTANT: Set tenant context from JWT
                        // This makes tenantId available throughout the request lifecycle
                        TenantContext.setTenantId(tenantId);
                        
                        // Create authentication token
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                        
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        
                        // Set authentication in security context
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        
                        log.debug("Authenticated user: {} (tenant: {})", email, tenantId);
                    }
                }
            } catch (Exception e) {
                log.error("Cannot set user authentication: {}", e.getMessage());
            }

            filterChain.doFilter(request, response);
            
        } finally {
            // CRITICAL: Always clear tenant context after request processing
            // This prevents memory leaks in thread pools
            TenantContext.clear();
        }
    }
}
