package com.encircle360.oss.straightmail.controller;

import java.io.IOException;

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
import com.encircle360.oss.straightmail.dto.template.RenderedTemplateDTO;
import com.encircle360.oss.straightmail.mapper.TemplateMapper;
import com.encircle360.oss.straightmail.model.Template;
import com.encircle360.oss.straightmail.service.FreemarkerService;
import com.encircle360.oss.straightmail.service.TemplateService;

import freemarker.template.TemplateException;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/render")
@Profile(MongoDbConfig.PROFILE)
public class RenderController {

    private final TemplateMapper mapper = TemplateMapper.INSTANCE;

    private final TemplateService templateService;

    private final FreemarkerService freemarkerService;

    @Operation(operationId = "renderTemplate", description = "Returns the rendered template as HTML.")
    @PostMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RenderedTemplateDTO> render(@RequestBody @Valid TemplateRenderRequestDTO templateRenderRequestDTO) {
        Template template = templateService.get(templateRenderRequestDTO.getTemplateId());
        if (template == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            String renderedHtml = freemarkerService.parseTemplateFromString(template.getHtml(),template.getLocale(), templateRenderRequestDTO.getModel());
            String renderedPlain = freemarkerService.parseTemplateFromString(template.getPlain(),template.getLocale(), templateRenderRequestDTO.getModel());

            RenderedTemplateDTO rendered = mapper.toRendered(template, renderedHtml, renderedPlain);
            return ResponseEntity.status(HttpStatus.OK).body(rendered);
        } catch (IOException | TemplateException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
