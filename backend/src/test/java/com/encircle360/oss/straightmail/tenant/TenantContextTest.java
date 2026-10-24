package com.encircle360.oss.straightmail.tenant;

import com.encircle360.oss.straightmail.model.Tenant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantContextTest {

    @Test
    void isDefault_true_when_tenantId_is_default() {
        TenantContext ctx = new TenantContext();
        ctx.setTenantId(Tenant.DEFAULT_ID);
        assertTrue(ctx.isDefault());
    }

    @Test
    void isDefault_false_for_custom_tenant() {
        TenantContext ctx = new TenantContext();
        ctx.setTenantId("acme-corp");
        assertFalse(ctx.isDefault());
    }

    @Test
    void isDefault_false_when_tenantId_is_null() {
        TenantContext ctx = new TenantContext();
        assertFalse(ctx.isDefault());
    }

    @Test
    void tenantId_getter_returns_set_value() {
        TenantContext ctx = new TenantContext();
        ctx.setTenantId("my-tenant");
        assertEquals("my-tenant", ctx.getTenantId());
    }
}
