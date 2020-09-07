package com.encircle360.oss.straightmail.wrapper.model;

import com.fasterxml.jackson.databind.node.BooleanNode;

import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.util.ModelFactory;
import freemarker.template.TemplateBooleanModel;

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
