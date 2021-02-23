package com.encircle360.oss.straightmail.controller;

import java.util.List;

import javax.validation.Valid;

import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.encircle360.oss.straightmail.config.MongoDbConfig;
import com.encircle360.oss.straightmail.dto.PageContainer;
import com.encircle360.oss.straightmail.dto.template.CreateUpdateTemplateDTO;
import com.encircle360.oss.straightmail.dto.template.TemplateDTO;
import com.encircle360.oss.straightmail.mapper.TemplateMapper;
import com.encircle360.oss.straightmail.model.Template;
import com.encircle360.oss.straightmail.service.TemplateService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/templates")
@Profile(MongoDbConfig.PROFILE)
public class TemplatesController {

    private final TemplateService templateService;

    private final TemplateMapper templateMapper;

    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "listTemplates", description = "Returns a pageable list of templates")
    public ResponseEntity<PageContainer<TemplateDTO>> list(@RequestParam(required = false) String sort,
                                                           @RequestParam(required = false) Integer size,
                                                           @RequestParam(required = false) Integer page) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        Page<Template> templatePage = templateService.findAll(pageable);
        List<TemplateDTO> dtos = templateMapper.toDtos(templatePage.getContent());

        PageContainer<TemplateDTO> pageContainer = PageContainer.of(dtos, templatePage);
        return ResponseEntity.status(HttpStatus.OK).body(pageContainer);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "getTemplate", description = "Returns a templates by its id, if not found 404")
    public ResponseEntity<TemplateDTO> get(@PathVariable final String id) {
        Template template = templateService.get(id);
        if (template == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        TemplateDTO templateDTO = templateMapper.toDto(template);
        return ResponseEntity.status(HttpStatus.OK).body(templateDTO);
    }

    @Operation(operationId = "createTemplate", description = "Creates a template in database with the given contents")
    @PostMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TemplateDTO> create(@Valid final CreateUpdateTemplateDTO createUpdateTemplateDTO) {
        Template template = templateMapper.createFromDtp(createUpdateTemplateDTO);
        template = templateService.save(template);
        TemplateDTO templateDTO = templateMapper.toDto(template);

        return ResponseEntity.status(HttpStatus.CREATED).body(templateDTO);
    }

    @Operation(operationId = "updateTemplate", description = "Updates a template in database with the given contents")
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TemplateDTO> update(@PathVariable final String id, @Valid final CreateUpdateTemplateDTO createUpdateTemplateDTO) {
        Template template = templateService.get(id);
        if (template == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        templateMapper.updateFromDto(createUpdateTemplateDTO, template);
        template = templateService.save(template);
        TemplateDTO templateDTO = templateMapper.toDto(template);

        return ResponseEntity.status(HttpStatus.OK).body(templateDTO);
    }

    @Operation(operationId = "deleteTemplate", description = "Deletes a template in database with the given id")
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> delete(@PathVariable final String id) {
        Template template = templateService.get(id);
        if (template == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        templateService.delete(template);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
