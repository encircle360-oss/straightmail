package com.encircle360.oss.straightmail.dto.template;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateDTO {

    @Schema(name = "id", description = "Id of the template in database")
    private String id;

    @Schema(name = "name", description = "Name of the template in database")
    private String name;

    @Schema(name = "content", description = "Content of the template in database")
    private String content;

    @Schema(name = "locale", description = "Locale of the template in database")
    private String locale;

}
