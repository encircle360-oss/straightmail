package com.encircle360.oss.straightmail.service;

import com.encircle360.oss.straightmail.dto.status.GitSyncStatusDTO;
import com.encircle360.oss.straightmail.dto.status.SyncResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryGitSyncStatusStoreTest {

    private InMemoryGitSyncStatusStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryGitSyncStatusStore();
    }

    @Test
    void record_and_get_returnsStoredStatus() {
        Instant now = Instant.now();
        store.record("acme", now, SyncResult.SUCCESS, null);

        Optional<GitSyncStatusDTO> result = store.get("acme");

        assertThat(result).isPresent();
        assertThat(result.get().tenantId()).isEqualTo("acme");
        assertThat(result.get().result()).isEqualTo(SyncResult.SUCCESS);
        assertThat(result.get().lastSyncAt()).isEqualTo(now);
        assertThat(result.get().errorMessage()).isNull();
    }

    @Test
    void record_overwrites_previous_status() {
        store.record("acme", Instant.now(), SyncResult.SUCCESS, null);
        store.record("acme", Instant.now(), SyncResult.FAILED, "network error");

        Optional<GitSyncStatusDTO> result = store.get("acme");

        assertThat(result).isPresent();
        assertThat(result.get().result()).isEqualTo(SyncResult.FAILED);
        assertThat(result.get().errorMessage()).isEqualTo("network error");
    }

    @Test
    void get_unknownTenant_returnsEmpty() {
        assertThat(store.get("unknown")).isEmpty();
    }

    @Test
    void getAll_returnsAllRecords_orderedByTenantId() {
        store.record("zebra", Instant.now(), SyncResult.SUCCESS, null);
        store.record("acme", Instant.now(), SyncResult.FAILED, "err");

        List<GitSyncStatusDTO> all = store.getAll();

        assertThat(all).hasSize(2);
        assertThat(all.get(0).tenantId()).isEqualTo("acme");
        assertThat(all.get(1).tenantId()).isEqualTo("zebra");
    }

    @Test
    void getAll_empty_returnsEmptyList() {
        assertThat(store.getAll()).isEmpty();
    }
}
