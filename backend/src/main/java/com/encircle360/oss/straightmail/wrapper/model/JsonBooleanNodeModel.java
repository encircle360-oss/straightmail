package com.encircle360.oss.straightmail.wrapper.model;

import tools.jackson.databind.node.BooleanNode;
import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.util.ModelFactory;
import freemarker.template.TemplateBooleanModel;

/**
 * FreeMarker template model for Jackson {@link tools.jackson.databind.node.BooleanNode}.
 * Exposes the node's boolean value via {@link TemplateBooleanModel#getAsBoolean()}.
 */
public class JsonBooleanNodeModel extends BeanModel implements TemplateBooleanModel {
    public static final ModelFactory FACTORY =
            (object, wrapper) -> new JsonBooleanNodeModel(object, (BeansWrapper) wrapper);

    JsonBooleanNodeModel(Object object, BeansWrapper wrapper) {
        super(object, wrapper);
    }

    @Override
    public boolean getAsBoolean() {
        return ((BooleanNode) object).asBoolean();
    }
}
