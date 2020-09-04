package com.encircle360.oss.straightmail.wrapper.model;

import com.fasterxml.jackson.databind.node.TextNode;

import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.util.ModelFactory;
import freemarker.template.TemplateScalarModel;

public class JsonTextNodeModel extends BeanModel implements TemplateScalarModel {
    public static final ModelFactory FACTORY = (object, wrapper) -> new JsonTextNodeModel(object, (BeansWrapper) wrapper);

    JsonTextNodeModel(Object object, BeansWrapper wrapper) {
        super(object, wrapper);
    }

    public String getAsString() {
        return ((TextNode) object).asText();
    }
}
