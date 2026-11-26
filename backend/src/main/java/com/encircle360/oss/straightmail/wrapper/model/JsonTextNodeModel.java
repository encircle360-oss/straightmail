package com.encircle360.oss.straightmail.wrapper.model;

import tools.jackson.databind.node.StringNode;
import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.util.ModelFactory;
import freemarker.template.TemplateScalarModel;

/**
 * FreeMarker template model for Jackson {@link tools.jackson.databind.node.StringNode}.
 * Exposes the node's string value via {@link TemplateScalarModel#getAsString()}.
 */
public class JsonTextNodeModel extends BeanModel implements TemplateScalarModel {
    public static final ModelFactory FACTORY = (object, wrapper) -> new JsonTextNodeModel(object, (BeansWrapper) wrapper);

    JsonTextNodeModel(Object object, BeansWrapper wrapper) {
        super(object, wrapper);
    }

    @Override
    public String getAsString() {
        StringNode stringNode = ((StringNode) object);
        return stringNode.asString();
    }
}
