package com.encircle360.oss.straightmail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic paginated response wrapper.
 *
 * <p>Wraps a list of items from a single page together with pagination metadata
 * (page index, page size, total element count, sort descriptor). Used by all paginated
 * list endpoints to provide a consistent response structure.
 *
 * @param <T> the type of items contained in this page
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PageContainer")
public class PageContainer<T> {

    @Schema(description = "contains the list with items from this page")
    private List<T> content;

    @Schema(description = "The size of the current page")
    private int size;

    @Schema(description = "The current page")
    private int page;

    @Schema(description = "Total elements in collection")
    private long totalElements;

    @Schema(description = "Sort string with desc and asc param")
    private String sort;

    /**
     * Creates a {@link PageContainer} from explicit pagination parameters.
     *
     * @param elements      the page content
     * @param page          zero-based page index ({@code null} defaults to 0)
     * @param size          page size ({@code null} defaults to 0)
     * @param totalElements total number of elements across all pages
     * @param sort          sort descriptor string
     * @param <T>           the item type
     * @return a populated {@link PageContainer}
     */
    public static <T> PageContainer<T> of(List<T> elements, Integer page, Integer size, long totalElements, String sort) {
        if (page == null) {
            page = 0;
        }

        if (size == null) {
            size = 0;
        }

        // Builder, not the all-args constructor. That constructor takes (content, size, page)
        // while every caller thinks in (page, size), so a positional call silently swapped the two
        // and every list endpoint answered with page and size exchanged.
        return PageContainer.<T>builder()
                .content(elements)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .sort(sort)
                .build();
    }

    /**
     * Creates a {@link PageContainer} from a Spring Data {@link Page} object.
     *
     * @param elements the mapped page content (may differ in type from the original {@link Page})
     * @param pageable the Spring Data page providing pagination metadata
     * @param <T>      the item type
     * @return a populated {@link PageContainer}
     */
    public static <T> PageContainer<T> of(List<T> elements, Page<?> pageable) {
        return PageContainer.<T>builder()
                .content(elements)
                .page(pageable.getNumber())
                .size(pageable.getSize())
                .totalElements(pageable.getTotalElements())
                .sort(pageable.getSort().toString())
                .build();
    }
}
