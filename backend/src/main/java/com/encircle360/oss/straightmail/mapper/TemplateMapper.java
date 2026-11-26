package com.encircle360.oss.straightmail.mapper;

import com.encircle360.oss.straightmail.dto.template.CreateUpdateTemplateDTO;
import com.encircle360.oss.straightmail.dto.template.RenderedTemplateDTO;
import com.encircle360.oss.straightmail.dto.template.TemplateDTO;
import com.encircle360.oss.straightmail.dto.template.TemplateView;
import com.encircle360.oss.straightmail.model.Template;
import com.encircle360.oss.straightmail.service.template.provider.TemplateSource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * MapStruct mapper for converting between {@link Template} entities and their DTO representations.
 *
 * <p>Provides mappings for the three common use cases:
 * <ul>
 *   <li>Entity → {@link TemplateDTO} (read response)</li>
 *   <li>{@link TemplateView} → {@link TemplateDTO} (composite provider response with source info)</li>
 *   <li>{@link CreateUpdateTemplateDTO} → {@link Template} (create and update)</li>
 *   <li>{@link Template} + rendered strings → {@link RenderedTemplateDTO} (render response)</li>
 * </ul>
 *
 * <p>The {@link #INSTANCE} singleton is used by controllers that cannot inject the mapper as a Spring bean
 * (e.g. {@link com.encircle360.oss.straightmail.controller.TemplatesController}).
 */
@Mapper(imports = TemplateSource.class)
public interface TemplateMapper {

    TemplateMapper INSTANCE = Mappers.getMapper(TemplateMapper.class);

    /**
     * Maps a {@link Template} entity to a {@link TemplateDTO}, defaulting source to DATABASE and editable to {@code true}.
     *
     * @param template the template entity
     * @return the mapped DTO
     */
    @Mapping(target = "id", expression = "java(template.getId() != null ? template.getId().toString() : null)")
    @Mapping(target = "source", expression = "java(TemplateSource.DATABASE)")
    @Mapping(target = "editable", expression = "java(true)")
    @Mapping(target = "gitBranch", ignore = true)
    TemplateDTO toDto(Template template);

    List<TemplateDTO> toDtos(List<Template> template);

    /**
     * Maps a {@link TemplateView} (source + editability annotated) to a {@link TemplateDTO},
     * overriding the source and editable fields from the view.
     *
     * <p>GIT and FILE templates automatically receive a lowercase source tag (e.g. {@code "git"},
     * {@code "file"}) via {@link TemplateView#effectiveTags()}, consistent with how providers filter them.
     *
     * @param view the view wrapper
     * @return the mapped DTO with source, editability, and effective tags from the view
     */
    default TemplateDTO toDto(TemplateView view) {
        TemplateDTO dto = toDto(view.template());
        dto.setSource(view.source());
        dto.setEditable(view.editable());
        dto.setTags(view.effectiveTags());
        dto.setGitBranch(view.gitBranch());
        // Non-database templates (GIT, FILE) have no UUID — use the name as a stable identifier.
        // Forward slashes in file-path names are encoded as "::" so the ID is URL-safe when
        // used as a path segment (e.g. "templates/default" → "templates::default").
        if (dto.getId() == null) {
            dto.setId(view.template().getName().replace("/", "::"));
        }
        return dto;
    }

    /**
     * Creates a new {@link Template} entity from a create/update DTO. ID and tenant ID are excluded.
     *
     * @param createUpdateTemplateDTO the DTO with template content
     * @return a new (unsaved) {@link Template} entity
     */
    @Mapping(ignore = true, target = "id")
    @Mapping(ignore = true, target = "tenantId")
    Template createFromDto(CreateUpdateTemplateDTO createUpdateTemplateDTO);

    /**
     * Updates an existing {@link Template} entity in-place from a create/update DTO.
     *
     * @param createUpdateTemplateDTO the DTO with new template content
     * @param template                the existing entity to update
     */
    @Mapping(ignore = true, target = "id")
    @Mapping(ignore = true, target = "tenantId")
    void updateFromDto(CreateUpdateTemplateDTO createUpdateTemplateDTO, @MappingTarget Template template);

    /**
     * Maps a {@link Template} entity along with pre-rendered content strings to a {@link RenderedTemplateDTO}.
     *
     * @param template      the template entity providing metadata (id, name, locale, tags)
     * @param renderedHtml  the fully rendered HTML body
     * @param renderedPlain the fully rendered plain-text body
     * @return the rendered template DTO
     */
    @Mapping(target = "html", source = "renderedHtml")
    @Mapping(target = "plain", source = "renderedPlain")
    RenderedTemplateDTO toRendered(Template template, String renderedHtml, String renderedPlain);
}
