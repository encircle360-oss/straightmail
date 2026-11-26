package com.encircle360.oss.straightmail.dto.status;

/**
 * Accessibility status of the configured file-template base directory.
 *
 * @param accessible {@code true} if the base directory exists and is readable
 */
public record FileStatusDTO(boolean accessible) {
}
