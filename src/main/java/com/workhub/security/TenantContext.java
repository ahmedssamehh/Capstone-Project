package com.workhub.security;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-local storage for current tenant ID
 * 
 * This class provides a way to store and access the tenant ID throughout
 * the request lifecycle without passing it as a parameter to every method.
 * 
 * The tenant ID is extracted from JWT token by JwtAuthenticationFilter
 * and stored in ThreadLocal, making it accessible anywhere in the application.
 * 
 * Usage:
 * - Set: TenantContext.setTenantId(tenantId)
 * - Get: Long tenantId = TenantContext.getTenantId()
 * - Check: boolean hasTenant = TenantContext.hasTenantId()
 * - Clear: TenantContext.clear()
 * 
 * Important: Always clear the context after request processing to prevent
 * memory leaks in thread pools. This is handled automatically by TenantContextFilter.
 */
@Slf4j
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
        // Private constructor to prevent instantiation
    }

    /**
     * Set the tenant ID for the current thread
     * 
     * @param tenantId Tenant ID to set
     */
    public static void setTenantId(Long tenantId) {
        if (tenantId == null) {
            log.warn("Attempting to set null tenant ID");
            return;
        }
        CURRENT_TENANT.set(tenantId);
        log.debug("Tenant context set: {}", tenantId);
    }

    /**
     * Get the tenant ID for the current thread
     * 
     * @return Current tenant ID, or null if not set
     */
    public static Long getTenantId() {
        return CURRENT_TENANT.get();
    }

    /**
     * Get the tenant ID for the current thread, or throw exception if not set
     * 
     * @return Current tenant ID
     * @throws IllegalStateException if tenant ID is not set
     */
    public static Long getRequiredTenantId() {
        Long tenantId = CURRENT_TENANT.get();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is not set. Ensure user is authenticated.");
        }
        return tenantId;
    }

    /**
     * Check if tenant ID is set for the current thread
     * 
     * @return true if tenant ID is set, false otherwise
     */
    public static boolean hasTenantId() {
        return CURRENT_TENANT.get() != null;
    }

    /**
     * Clear the tenant ID for the current thread
     * 
     * This should be called at the end of request processing to prevent
     * memory leaks in thread pools.
     */
    public static void clear() {
        Long tenantId = CURRENT_TENANT.get();
        if (tenantId != null) {
            log.debug("Clearing tenant context: {}", tenantId);
        }
        CURRENT_TENANT.remove();
    }
}
