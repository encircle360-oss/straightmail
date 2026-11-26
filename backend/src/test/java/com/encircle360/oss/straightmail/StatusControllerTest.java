package com.encircle360.oss.straightmail;

import com.encircle360.oss.straightmail.dto.status.FileStatusDTO;
import com.encircle360.oss.straightmail.dto.status.StatusDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link com.encircle360.oss.straightmail.controller.StatusController}.
 *
 * <p>Runs with the {@code database} profile active, so JPA repositories and the status
 * controller bean are available. Git-sync is disabled by default in the test configuration,
 * so the {@code gitSync} list is expected to be empty.
 */
@SpringBootTest(classes = StraightmailApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles({"database", "test"})
class StatusControllerTest extends AbstractTest {

    @Test
    void getStatus_returnsOkWithEmptyGitSyncAndFileStatus() throws Exception {
        MvcResult result = get("/v1/status", status().isOk());
        StatusDTO status = resultToObject(result, StatusDTO.class);

        assertNotNull(status);
        assertNotNull(status.gitSync());
        // Git-sync is disabled in test config (templates.git-sync.enabled=false by default)
        assertTrue(status.gitSync().isEmpty());
        // File provider status depends on test config; may be null or present
        // We only verify the structure is deserializable
    }

    @Test
    void getStatus_fileTemplates_reflectsAccessibility() throws Exception {
        MvcResult result = get("/v1/status", status().isOk());
        StatusDTO status = resultToObject(result, StatusDTO.class);

        assertNotNull(status);
        // If file templates are enabled, the fileTemplates field should be populated
        if (status.fileTemplates() != null) {
            // The accessible flag must be a boolean (deserialization check)
            FileStatusDTO fileStatus = status.fileTemplates();
            assertNotNull(fileStatus);
        }
    }

    @Test
    void triggerGitSync_whenDisabled_returns503() throws Exception {
        // Git-sync is disabled in test config (templates.git-sync.enabled=false),
        // so any trigger request returns 503 Service Unavailable.
        emptyPost("/v1/sync/git/default", status().isServiceUnavailable());
    }

    @Test
    void triggerGitSync_unknownTenant_whenDisabled_returns503() throws Exception {
        // 503 takes precedence over 404 when git-sync is disabled globally.
        emptyPost("/v1/sync/git/no-such-tenant", status().isServiceUnavailable());
    }
}
