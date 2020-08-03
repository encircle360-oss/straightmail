package com.encircle360.oss.straightmail.dto;

import java.util.HashMap;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequestDTO {

    private String recipient;

    private String subject;

    private String sender;

    // variables defined in template
    private HashMap<String, String> model;

    // if null use default
    private EmailTemplateDTO emailTemplate;
}
