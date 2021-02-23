package com.encircle360.oss.straightmail.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageContainer<T> {

    private List<T> content;

    private int size;

    private int page;

    private long totalElements;

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
