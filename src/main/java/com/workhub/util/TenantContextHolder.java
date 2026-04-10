package com.workhub.util;

import com.workhub.security.TenantContext;
import lombok.extern.slf4j.Slf4j;

/**
 * Tenant Context Holder Utility
 * 
 * Provides convenient methods to access tenant context from anywhere in the application.
 * This is a facade over TenantContext for easier usage in services and repositories.
 * 
 * Usage Examples:
 * 
 * 1. Get current tenant ID (returns null if not set):
 *    Long tenantId = TenantContextHolder.getCurrentTenantId();
 * 
 * 2. Get current tenant ID (throws exception if not set):
 *    Long tenantId = TenantContextHolder.requireCurrentTenantId();
 * 
 * 3. Check if tenant context is available:
 *    if (TenantContextHolder.hasTenantContext()) {
 *        // Do something tenant-specific
 *    }
 * 
 * 4. Execute code with specific tenant context:
 *    TenantContextHolder.executeInTenantContext(tenantId, () -> {
 *        // Your code here
 *    });
 */
@Slf4j
public final class TenantContextHolder {

    private TenantContextHolder() {
        // Utility class - prevent instantiation
    }

    /**
     * Get the current tenant ID
     * 
     * @return Current tenant ID, or null if not set
     */
    public static Long getCurrentTenantId() {
        return TenantContext.getTenantId();
    }

    /**
     * Get the current tenant ID, throwing exception if not set
     * 
     * @return Current tenant ID
     * @throws IllegalStateException if tenant context is not set
     */
    public static Long requireCurrentTenantId() {
        return TenantContext.getRequiredTenantId();
    }

    /**
     * Check if tenant context is available
     * 
     * @return true if tenant ID is set, false otherwise
     */
    public static boolean hasTenantContext() {
        return TenantContext.hasTenantId();
    }

    /**
     * Execute a runnable within a specific tenant context
     * 
     * Useful for background jobs or async operations that need tenant context.
     * The original tenant context is restored after execution.
     * 
     * @param tenantId Tenant ID to set
     * @param runnable Code to execute
     */
    public static void executeInTenantContext(Long tenantId, Runnable runnable) {
        Long originalTenantId = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(tenantId);
            log.debug("Executing in tenant context: {}", tenantId);
            runnable.run();
        } finally {
            if (originalTenantId != null) {
                TenantContext.setTenantId(originalTenantId);
            } else {
                TenantContext.clear();
            }
        }
    }

    /**
     * Execute a callable within a specific tenant context and return result
     * 
     * @param tenantId Tenant ID to set
     * @param callable Code to execute
     * @param <T> Return type
     * @return Result from callable
     * @throws Exception if callable throws exception
     */
    public static <T> T executeInTenantContext(Long tenantId, java.util.concurrent.Callable<T> callable) throws Exception {
        Long originalTenantId = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(tenantId);
            log.debug("Executing in tenant context: {}", tenantId);
            return callable.call();
        } finally {
            if (originalTenantId != null) {
                TenantContext.setTenantId(originalTenantId);
            } else {
                TenantContext.clear();
            }
        }
    }

    /**
     * Get tenant ID as string (useful for logging)
     * 
     * @return Tenant ID as string, or "NONE" if not set
     */
    public static String getTenantIdAsString() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId != null ? tenantId.toString() : "NONE";
    }
}
