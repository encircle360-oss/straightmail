package com.encircle360.oss.straightmail.service.template;

import com.encircle360.oss.straightmail.model.Template;
import com.encircle360.oss.straightmail.repository.TemplateRepository;
import com.encircle360.oss.straightmail.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private TenantContext tenantContext;

    @InjectMocks
    private TemplateService templateService;

    @Test
    void findAll_withoutTags_usesFindAllByTenantId() {
        String tenantId = "acme";
        Template t = Template.builder().name("welcome").tenantId(tenantId).build();
        when(tenantContext.getTenantId()).thenReturn(tenantId);
        when(templateRepository.findAllByTenantId(tenantId, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(t)));

        List<Template> result = templateService.findAll(null);

        assertEquals(1, result.size());
        assertEquals("welcome", result.get(0).getName());
        verify(templateRepository).findAllByTenantId(tenantId, Pageable.unpaged());
        verifyNoMoreInteractions(templateRepository);
    }

    @Test
    void findAll_withEmptyTags_usesFindAllByTenantId() {
        String tenantId = "acme";
        when(tenantContext.getTenantId()).thenReturn(tenantId);
        when(templateRepository.findAllByTenantId(tenantId, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of()));

        templateService.findAll(List.of());

        verify(templateRepository).findAllByTenantId(tenantId, Pageable.unpaged());
        verify(templateRepository, never()).findAllByTenantIdAndTagsContains(any(), any(), any());
    }

    @Test
    void findAll_withTags_usesFindAllByTenantIdAndTagsContains() {
        String tenantId = "acme";
        List<String> tags = List.of("promo");
        Template t = Template.builder().name("promo-mail").tenantId(tenantId).tags(tags).build();
        when(tenantContext.getTenantId()).thenReturn(tenantId);
        when(templateRepository.findAllByTenantIdAndTagsContains(tenantId, tags, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(t)));

        List<Template> result = templateService.findAll(tags);

        assertEquals(1, result.size());
        verify(templateRepository).findAllByTenantIdAndTagsContains(tenantId, tags, Pageable.unpaged());
        verify(templateRepository, never()).findAllByTenantId(any(), any());
    }

    @Test
    void findById_delegatesToRepository() {
        String tenantId = "acme";
        UUID id = UUID.randomUUID();
        Template t = Template.builder().name("welcome").tenantId(tenantId).build();
        when(tenantContext.getTenantId()).thenReturn(tenantId);
        when(templateRepository.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.of(t));

        Optional<Template> result = templateService.findById(id);

        assertTrue(result.isPresent());
        assertEquals("welcome", result.get().getName());
        verify(templateRepository).findByIdAndTenantId(id, tenantId);
    }

    @Test
    void findById_returnsEmptyWhenNotFound() {
        String tenantId = "acme";
        UUID id = UUID.randomUUID();
        when(tenantContext.getTenantId()).thenReturn(tenantId);
        when(templateRepository.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.empty());

        Optional<Template> result = templateService.findById(id);

        assertTrue(result.isEmpty());
    }

    @Test
    void findByName_delegatesToRepository() {
        String tenantId = "acme";
        String name = "welcome";
        Template t = Template.builder().name(name).tenantId(tenantId).build();
        when(tenantContext.getTenantId()).thenReturn(tenantId);
        when(templateRepository.findByTenantIdAndName(tenantId, name)).thenReturn(Optional.of(t));

        Optional<Template> result = templateService.findByName(name);

        assertTrue(result.isPresent());
        assertEquals(name, result.get().getName());
        verify(templateRepository).findByTenantIdAndName(tenantId, name);
    }

    @Test
    void findByName_returnsEmptyWhenNotFound() {
        String tenantId = "acme";
        when(tenantContext.getTenantId()).thenReturn(tenantId);
        when(templateRepository.findByTenantIdAndName(tenantId, "missing")).thenReturn(Optional.empty());

        Optional<Template> result = templateService.findByName("missing");

        assertTrue(result.isEmpty());
    }
}
