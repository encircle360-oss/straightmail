package com.encircle360.oss.straightmail.service.template;

import com.encircle360.oss.straightmail.model.GitTemplateCache;
import com.encircle360.oss.straightmail.model.Template;
import com.encircle360.oss.straightmail.repository.GitTemplateCacheRepository;
import com.encircle360.oss.straightmail.service.template.provider.DatabaseGitTemplateRegistry;
import com.encircle360.oss.straightmail.service.template.provider.GitTemplateRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseGitTemplateRegistryTest {

    @Mock
    private GitTemplateCacheRepository cacheRepository;

    @InjectMocks
    private DatabaseGitTemplateRegistry registry;

    private Template template(String name, String html) {
        Template t = new Template();
        t.setName(name);
        t.setHtml(html);
        return t;
    }

    @Test
    void updateBranch_deletesExistingAndSavesNew() {
        Map<String, Template> templates = Map.of("welcome", template("welcome", "<h1>Hi</h1>"));

        registry.updateBranch("acme", "main", 0, templates);

        verify(cacheRepository).deleteAllByTenantIdAndBranch("acme", "main");
        verify(cacheRepository).flush();
        ArgumentCaptor<GitTemplateCache> captor = ArgumentCaptor.forClass(GitTemplateCache.class);
        verify(cacheRepository).save(captor.capture());
        GitTemplateCache saved = captor.getValue();
        assertThat(saved.getTenantId()).isEqualTo("acme");
        assertThat(saved.getBranch()).isEqualTo("main");
        assertThat(saved.getBranchOrder()).isEqualTo(0);
        assertThat(saved.getTemplateName()).isEqualTo("welcome");
        assertThat(saved.getHtml()).isEqualTo("<h1>Hi</h1>");
    }

    @Test
    void updateBranch_emptyTemplates_onlyDeletes() {
        registry.updateBranch("acme", "main", 0, Map.of());

        verify(cacheRepository).deleteAllByTenantIdAndBranch("acme", "main");
        verify(cacheRepository).flush();
        verify(cacheRepository, never()).save(any());
    }

    @Test
    void getBranchTemplates_groupsByBranch() {
        List<GitTemplateCache> rows = List.of(
                row("acme", "main", 0, "welcome"),
                row("acme", "develop", 1, "newsletter")
        );
        when(cacheRepository.findAllByTenantIdOrderByBranchOrderAscTemplateNameAsc("acme")).thenReturn(rows);

        Map<String, Map<String, Template>> result = registry.getBranchTemplates("acme");

        assertThat(result).containsKeys("main", "develop");
        assertThat(result.get("main")).containsKey("welcome");
        assertThat(result.get("develop")).containsKey("newsletter");
    }

    @Test
    void getBranchTemplates_unknownTenant_returnsEmptyMap() {
        when(cacheRepository.findAllByTenantIdOrderByBranchOrderAscTemplateNameAsc("unknown"))
                .thenReturn(List.of());

        assertThat(registry.getBranchTemplates("unknown")).isEmpty();
    }

    @Test
    void findFirst_returnsLowestBranchOrderMatch() {
        List<GitTemplateCache> rows = List.of(
                row("acme", "main", 0, "welcome"),
                row("acme", "develop", 1, "welcome")
        );
        when(cacheRepository.findAllByTenantIdOrderByBranchOrderAscTemplateNameAsc("acme")).thenReturn(rows);

        Optional<GitTemplateRegistry.GitEntry> entry = registry.findFirst("acme", "welcome");

        assertThat(entry).isPresent();
        assertThat(entry.get().branch()).isEqualTo("main");
    }

    @Test
    void findFirst_unknownTemplate_returnsEmpty() {
        when(cacheRepository.findAllByTenantIdOrderByBranchOrderAscTemplateNameAsc("acme"))
                .thenReturn(List.of(row("acme", "main", 0, "other")));

        assertThat(registry.findFirst("acme", "missing")).isEmpty();
    }

    private GitTemplateCache row(String tenantId, String branch, int branchOrder, String templateName) {
        return GitTemplateCache.builder()
                .id(java.util.UUID.randomUUID().toString())
                .tenantId(tenantId)
                .branch(branch)
                .branchOrder(branchOrder)
                .templateName(templateName)
                .html("<p>" + templateName + "</p>")
                .build();
    }
}
