package com.encircle360.oss.straightmail.controller;

import javax.validation.Valid;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.encircle360.oss.straightmail.config.MongoDbConfig;
import com.encircle360.oss.straightmail.dto.TemplateRenderRequestDTO;
import com.encircle360.oss.straightmail.model.Template;
import com.encircle360.oss.straightmail.service.TemplateService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/render")
@Profile(MongoDbConfig.PROFILE)
public class RenderController {

    private final TemplateService templateService;

    @Operation(operationId = "renderTemplate", description = "Returns the rendered template as HTML.")
    @PostMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> render(@RequestBody @Valid TemplateRenderRequestDTO templateRenderRequestDTO) {
        Template template = templateService.get(templateRenderRequestDTO.getTemplateId());
        if (template == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
