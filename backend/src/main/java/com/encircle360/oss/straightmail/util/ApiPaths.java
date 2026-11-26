package com.encircle360.oss.straightmail.util;

/**
 * Decides whether a request path belongs to the REST API rather than to the admin SPA.
 *
 * <p>Shared by the authentication filters, the tenant-resolution filters and the SPA fallback so
 * they all draw the line in the same place. A plain {@code startsWith} is not enough: with the
 * default prefix {@code /api} it also matches the SPA route {@code /api-key-login}, which then
 * fails with a missing-header error instead of rendering the login page.
 */
public final class ApiPaths {

    private ApiPaths() {
    }

    /**
     * Returns whether {@code path} lies under {@code apiPrefix}, respecting path-segment boundaries.
     *
     * <p>A blank prefix means the API is served from the root, so every path counts as an API path.
     *
     * @param path      the request URI
     * @param apiPrefix the configured API prefix, e.g. {@code /api}
     * @return {@code true} when the path is an API path
     */
    public static boolean isApiPath(String path, String apiPrefix) {
        if (apiPrefix == null || apiPrefix.isBlank()) {
            return true;
        }
        return path.equals(apiPrefix) || path.startsWith(apiPrefix + "/");
    }
}
