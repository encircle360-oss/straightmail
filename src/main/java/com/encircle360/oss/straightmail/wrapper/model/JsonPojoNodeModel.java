package com.encircle360.oss.straightmail.wrapper.model;

import com.fasterxml.jackson.databind.node.POJONode;

import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.util.ModelFactory;

public class JsonPojoNodeModel extends BeanModel {
    public static final ModelFactory FACTORY =
        (object, wrapper) -> new JsonPojoNodeModel(((POJONode) object).getPojo(), (BeansWrapper) wrapper);

    JsonPojoNodeModel(Object object, BeansWrapper wrapper) {
        super(object, wrapper);
    }
}
