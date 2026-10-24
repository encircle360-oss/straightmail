package com.encircle360.oss.straightmail.service.template;

import com.encircle360.oss.straightmail.model.Template;
import com.encircle360.oss.straightmail.service.template.provider.GitTemplateRegistry;
import com.encircle360.oss.straightmail.service.template.provider.InMemoryGitTemplateRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GitTemplateRegistryTest {

    private GitTemplateRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new InMemoryGitTemplateRegistry();
    }

    private Template template(String name) {
        Template t = new Template();
        t.setName(name);
        return t;
    }

    @Test
    void updateBranch_storesBranchTemplates() {
        registry.updateBranch("acme", "main", 0, Map.of("welcome", template("welcome")));

        Map<String, Map<String, Template>> branches = registry.getBranchTemplates("acme");
        assertThat(branches).containsKey("main");
        assertThat(branches.get("main")).containsKey("welcome");
    }

    @Test
    void updateBranch_differentBranches_bothSurvive() {
        registry.updateBranch("acme", "main", 0, Map.of("welcome", template("welcome")));
        registry.updateBranch("acme", "develop", 1, Map.of("newsletter", template("newsletter")));

        Map<String, Map<String, Template>> branches = registry.getBranchTemplates("acme");
        assertThat(branches).containsKeys("main", "develop");
        assertThat(branches.get("main")).containsKey("welcome");
        assertThat(branches.get("develop")).containsKey("newsletter");
    }

    @Test
    void updateBranch_sameBranch_replacesOnlyThatBranch() {
        registry.updateBranch("acme", "main", 0, Map.of("old", template("old")));
        registry.updateBranch("acme", "develop", 1, Map.of("newsletter", template("newsletter")));
        registry.updateBranch("acme", "main", 0, Map.of("welcome", template("welcome")));

        Map<String, Map<String, Template>> branches = registry.getBranchTemplates("acme");
        assertThat(branches.get("main")).containsOnlyKeys("welcome");
        assertThat(branches.get("develop")).containsKey("newsletter");
    }

    @Test
    void findFirst_returnsFirstBranchMatch() {
        registry.updateBranch("acme", "main", 0, Map.of("welcome", template("welcome")));
        registry.updateBranch("acme", "develop", 1, Map.of("welcome", template("welcome")));

        Optional<GitTemplateRegistry.GitEntry> result = registry.findFirst("acme", "welcome");

        assertThat(result).isPresent();
        assertThat(result.get().branch()).isEqualTo("main");
    }

    @Test
    void findFirst_templateOnlyInSecondBranch_returnsSecond() {
        registry.updateBranch("acme", "main", 0, Map.of("other", template("other")));
        registry.updateBranch("acme", "develop", 1, Map.of("welcome", template("welcome")));

        Optional<GitTemplateRegistry.GitEntry> result = registry.findFirst("acme", "welcome");

        assertThat(result).isPresent();
        assertThat(result.get().branch()).isEqualTo("develop");
    }

    @Test
    void findFirst_unknownTemplate_returnsEmpty() {
        registry.updateBranch("acme", "main", 0, Map.of("welcome", template("welcome")));

        Optional<GitTemplateRegistry.GitEntry> result = registry.findFirst("acme", "nonexistent");

        assertThat(result).isEmpty();
    }

    @Test
    void getBranchTemplates_unknownTenant_returnsEmptyMap() {
        assertThat(registry.getBranchTemplates("unknown")).isEmpty();
    }

    @Test
    void findFirst_unknownTenant_returnsEmpty() {
        assertThat(registry.findFirst("unknown", "welcome")).isEmpty();
    }
}
