package com.encircle360.oss.straightmail.service;

import com.encircle360.oss.straightmail.dto.status.GitSyncStatusDTO;
import com.encircle360.oss.straightmail.dto.status.SyncResult;
import com.encircle360.oss.straightmail.model.GitSyncStatus;
import com.encircle360.oss.straightmail.repository.GitSyncStatusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseGitSyncStatusStoreTest {

    @Mock
    private GitSyncStatusRepository repository;

    @InjectMocks
    private DatabaseGitSyncStatusStore store;

    @Test
    void record_insertsNewEntity_whenNoneExists() {
        Instant now = Instant.now();
        when(repository.findById("acme")).thenReturn(Optional.empty());

        store.record("acme", now, SyncResult.SUCCESS, null);

        ArgumentCaptor<GitSyncStatus> captor = ArgumentCaptor.forClass(GitSyncStatus.class);
        verify(repository).save(captor.capture());
        GitSyncStatus saved = captor.getValue();
        assertThat(saved.getTenantId()).isEqualTo("acme");
        assertThat(saved.getLastSyncAt()).isEqualTo(now);
        assertThat(saved.getResult()).isEqualTo(SyncResult.SUCCESS);
        assertThat(saved.getErrorMessage()).isNull();
    }

    @Test
    void record_updatesExistingEntity() {
        Instant now = Instant.now();
        GitSyncStatus existing = GitSyncStatus.builder()
                .tenantId("acme")
                .lastSyncAt(now.minusSeconds(3600))
                .result(SyncResult.SUCCESS)
                .build();
        when(repository.findById("acme")).thenReturn(Optional.of(existing));

        store.record("acme", now, SyncResult.FAILED, "timeout");

        verify(repository).save(existing);
        assertThat(existing.getResult()).isEqualTo(SyncResult.FAILED);
        assertThat(existing.getErrorMessage()).isEqualTo("timeout");
        assertThat(existing.getLastSyncAt()).isEqualTo(now);
    }

    @Test
    void record_truncatesErrorMessage_whenTooLong() {
        String longError = "x".repeat(2000);
        when(repository.findById("acme")).thenReturn(Optional.empty());

        store.record("acme", Instant.now(), SyncResult.FAILED, longError);

        ArgumentCaptor<GitSyncStatus> captor = ArgumentCaptor.forClass(GitSyncStatus.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getErrorMessage()).hasSize(1024);
    }

    @Test
    void get_returnsDtoFromRepository() {
        Instant now = Instant.now();
        GitSyncStatus entity = GitSyncStatus.builder()
                .tenantId("acme")
                .lastSyncAt(now)
                .result(SyncResult.SUCCESS)
                .errorMessage(null)
                .build();
        when(repository.findById("acme")).thenReturn(Optional.of(entity));

        Optional<GitSyncStatusDTO> result = store.get("acme");

        assertThat(result).isPresent();
        assertThat(result.get().tenantId()).isEqualTo("acme");
        assertThat(result.get().result()).isEqualTo(SyncResult.SUCCESS);
    }

    @Test
    void get_unknownTenant_returnsEmpty() {
        when(repository.findById("unknown")).thenReturn(Optional.empty());

        assertThat(store.get("unknown")).isEmpty();
    }

    @Test
    void getAll_returnsMappedDtos() {
        GitSyncStatus e1 = GitSyncStatus.builder().tenantId("acme").result(SyncResult.SUCCESS).build();
        GitSyncStatus e2 = GitSyncStatus.builder().tenantId("corp").result(SyncResult.FAILED).errorMessage("err").build();
        when(repository.findAllByOrderByTenantIdAsc()).thenReturn(List.of(e1, e2));

        List<GitSyncStatusDTO> all = store.getAll();

        assertThat(all).hasSize(2);
        assertThat(all.get(0).tenantId()).isEqualTo("acme");
        assertThat(all.get(1).errorMessage()).isEqualTo("err");
    }
}
