package com.encircle360.oss.straightmail.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "templates")
public class Template {

    @Id
    private String id;

    private String subject;

    private String name;

    private String html;

    private String plain;

    private String locale;

    private List<String> tags;
}
