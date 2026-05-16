package com.workhub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Request tracing filter that enriches MDC and emits start/end request logs.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@Slf4j
public class RequestTracingFilter extends OncePerRequestFilter {

    private static final String HTTP_METHOD_KEY = "httpMethod";
    private static final String HTTP_PATH_KEY = "httpPath";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startNanos = System.nanoTime();
        MDC.put(HTTP_METHOD_KEY, request.getMethod());
        MDC.put(HTTP_PATH_KEY, request.getRequestURI());
        try {
            log.info("request-start");
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            log.info("request-end status={} durationMs={}", response.getStatus(), durationMs);
            MDC.remove(HTTP_METHOD_KEY);
            MDC.remove(HTTP_PATH_KEY);
        }
    }
}
