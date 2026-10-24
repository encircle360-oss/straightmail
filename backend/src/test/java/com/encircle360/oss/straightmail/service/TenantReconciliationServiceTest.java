package com.encircle360.oss.straightmail.service;

import com.encircle360.oss.straightmail.config.TenantProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantReconciliationServiceTest {

    @Mock
    private TenantService tenantService;

    @Mock
    private TenantProperties tenantProperties;

    @InjectMocks
    private TenantReconciliationService reconciliationService;

    @Test
    void reconcile_does_nothing_when_config_is_empty() {
        when(tenantProperties.getConfig()).thenReturn(List.of());

        reconciliationService.reconcile();

        verifyNoInteractions(tenantService);
    }

    @Test
    void reconcile_imports_new_config_tenants() {
        TenantProperties.TenantConfig cfg1 = new TenantProperties.TenantConfig();
        cfg1.setId("acme");
        TenantProperties.TenantConfig cfg2 = new TenantProperties.TenantConfig();
        cfg2.setId("beta");

        when(tenantProperties.getConfig()).thenReturn(List.of(cfg1, cfg2));

        reconciliationService.reconcile();

        verify(tenantService).importFromConfig(cfg1);
        verify(tenantService).importFromConfig(cfg2);
    }

    @Test
    void reconcile_never_deletes_tenants() {
        TenantProperties.TenantConfig cfg = new TenantProperties.TenantConfig();
        cfg.setId("acme");

        when(tenantProperties.getConfig()).thenReturn(List.of(cfg));

        reconciliationService.reconcile();

        verify(tenantService, never()).delete(any());
    }
}
