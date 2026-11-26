package com.encircle360.oss.straightmail.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the segment boundary that separates API paths from SPA routes.
 *
 * <p>A plain {@code startsWith("/api")} also matches {@code /api-key-login}, which made the
 * tenant-resolution filter answer that SPA route with "Missing X-Tenant-ID header".
 */
class ApiPathsTest {

    @Test
    void matchesThePrefixItself() {
        assertTrue(ApiPaths.isApiPath("/api", "/api"));
    }

    @Test
    void matchesPathsBelowThePrefix() {
        assertTrue(ApiPaths.isApiPath("/api/v1/templates", "/api"));
    }

    @Test
    void doesNotMatchASiblingRouteSharingThePrefixAsSubstring() {
        assertFalse(ApiPaths.isApiPath("/api-key-login", "/api"));
        assertFalse(ApiPaths.isApiPath("/apidocs", "/api"));
    }

    @Test
    void doesNotMatchUnrelatedRoutes() {
        assertFalse(ApiPaths.isApiPath("/templates", "/api"));
    }

    @Test
    void blankPrefixMeansEverythingIsApi() {
        assertTrue(ApiPaths.isApiPath("/templates", ""));
        assertTrue(ApiPaths.isApiPath("/templates", null));
    }
}
