package com.encircle360.oss.straightmail.wrapper.model;

import tools.jackson.databind.node.NumericNode;
import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.util.ModelFactory;
import freemarker.template.TemplateNumberModel;

/**
 * FreeMarker template model for Jackson {@link tools.jackson.databind.node.NumericNode}.
 * Exposes the node's numeric value via {@link TemplateNumberModel#getAsNumber()}.
 */
public class JsonNumberNodeModel extends BeanModel implements TemplateNumberModel {
    public static final ModelFactory FACTORY =
            (object, wrapper) -> new JsonNumberNodeModel(object, (BeansWrapper) wrapper);

    JsonNumberNodeModel(Object object, BeansWrapper wrapper) {
        super(object, wrapper);
    }

    @Override
    public Number getAsNumber() {
        return ((NumericNode) object).numberValue();
    }
}
