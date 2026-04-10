package com.workhub.aspect;

import com.workhub.util.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Tenant Aspect
 * 
 * AOP aspect for logging tenant context in method executions
 * Useful for debugging and monitoring tenant isolation
 */
@Aspect
@Component
@Slf4j
public class TenantAspect {

    /**
     * Log tenant context for all service methods
     */
    @Around("execution(* com.workhub.service..*(..))")
    public Object logTenantContext(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        
        if (TenantContextHolder.hasTenantContext()) {
            Long tenantId = TenantContextHolder.getCurrentTenantId();
            log.trace("Executing {} in tenant context: {}", methodName, tenantId);
        } else {
            log.trace("Executing {} without tenant context", methodName);
        }
        
        return joinPoint.proceed();
    }
}
