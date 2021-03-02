package com.encircle360.oss.straightmail.service.template;

import com.encircle360.oss.straightmail.model.Template;

public interface TemplateLoader {

    Template loadTemplate(String templateId);
}
