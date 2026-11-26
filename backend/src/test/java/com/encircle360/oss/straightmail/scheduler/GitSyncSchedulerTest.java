package com.encircle360.oss.straightmail.scheduler;

import com.encircle360.oss.straightmail.config.TenantProperties;
import com.encircle360.oss.straightmail.model.Tenant;
import com.encircle360.oss.straightmail.repository.TenantRepository;
import com.encircle360.oss.straightmail.service.GitSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitSyncSchedulerTest {

    @Mock
    private GitSyncService gitSyncService;

    @Mock
    private ObjectProvider<TenantRepository> tenantRepositoryProvider;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private TenantProperties tenantProperties;

    private GitSyncScheduler scheduler;

    @BeforeEach
    void setUp() {
        when(tenantRepositoryProvider.getIfAvailable()).thenReturn(tenantRepository);
        lenient().when(tenantProperties.getConfig()).thenReturn(List.of());
        scheduler = new GitSyncScheduler(gitSyncService, tenantRepositoryProvider, tenantProperties);
    }

    @Test
    void onApplicationReady_triggersDoSync() {
        when(tenantRepository.findAllByGitRepoUrlNotNullAndActiveTrue()).thenReturn(List.of());

        scheduler.onApplicationReady();

        verify(tenantRepository, times(1)).findAllByGitRepoUrlNotNullAndActiveTrue();
    }

    @Test
    void sync_triggersDoSync() {
        when(tenantRepository.findAllByGitRepoUrlNotNullAndActiveTrue()).thenReturn(List.of());

        scheduler.sync();

        verify(tenantRepository, times(1)).findAllByGitRepoUrlNotNullAndActiveTrue();
    }

    @Test
    void doSync_callsSyncTenantForEachTenant() {
        Tenant tenant1 = new Tenant();
        Tenant tenant2 = new Tenant();
        when(tenantRepository.findAllByGitRepoUrlNotNullAndActiveTrue()).thenReturn(List.of(tenant1, tenant2));

        scheduler.doSync();

        verify(gitSyncService, times(1)).syncTenant(tenant1);
        verify(gitSyncService, times(1)).syncTenant(tenant2);
    }
}
