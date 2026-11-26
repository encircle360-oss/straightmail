package com.encircle360.oss.straightmail.dto;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the pagination metadata of {@link PageContainer}.
 *
 * <p>The all-args constructor takes {@code (content, size, page)} while callers think in
 * {@code (page, size)}, so the two were silently swapped in the responses of every list endpoint.
 */
class PageContainerTest {

    @Test
    void of_explicitParameters_keepsPageAndSizeApart() {
        PageContainer<String> container = PageContainer.of(List.of("a"), 2, 10, 42L, "name: ASC");

        assertEquals(2, container.getPage());
        assertEquals(10, container.getSize());
        assertEquals(42L, container.getTotalElements());
        assertEquals("name: ASC", container.getSort());
        assertEquals(List.of("a"), container.getContent());
    }

    @Test
    void of_explicitParameters_defaultsNullsToZero() {
        PageContainer<String> container = PageContainer.of(List.of(), null, null, 0L, "");

        assertEquals(0, container.getPage());
        assertEquals(0, container.getSize());
    }

    @Test
    void of_springPage_keepsPageAndSizeApart() {
        PageImpl<String> page = new PageImpl<>(
                List.of("a", "b"),
                PageRequest.of(3, 25, Sort.by("name")),
                200L);

        PageContainer<String> container = PageContainer.of(page.getContent(), page);

        assertEquals(3, container.getPage());
        assertEquals(25, container.getSize());
        assertEquals(200L, container.getTotalElements());
    }
}
