package com.encircle360.oss.straightmail.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    public static <T> PageContainer<T> of(List<T> elements, Integer page, Integer size, long totalElements, String sort) {
        if (page == null) {
            page = 0;
        }

        if (size == null) {
            size = 0;
        }

        return new PageContainer<T>(elements, page, size, totalElements, sort);
    }

    public static <T> PageContainer<T> of(List<T> elements, Page<?> pageable) {
        return new PageContainer<T>(elements, pageable.getNumber(), pageable.getSize(), pageable.getTotalElements(), pageable.getSort().toString());
    }
}
