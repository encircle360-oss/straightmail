package com.encircle360.oss.straightmail.tenant;

/**
 * Utility class holding validation constants for tenant identifiers.
 *
 * <p>Not instantiable.
 */
public final class TenantValidation {

    /**
     * Regular expression pattern that a valid tenant slug must match.
     * Slugs must be 2–63 characters long, start and end with a lowercase letter or digit,
     * and may contain hyphens in between.
     */
    public static final String SLUG_PATTERN = "^[a-z0-9][a-z0-9\\-]{0,61}[a-z0-9]$";

    private TenantValidation() {
    }
}
