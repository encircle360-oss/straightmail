package com.encircle360.oss.straightmail.dto.template;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class CreateUpdateTemplateDTO {

    private String name;

    private String content;
}
