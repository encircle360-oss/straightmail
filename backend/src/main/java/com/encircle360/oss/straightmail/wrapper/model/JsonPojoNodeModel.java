package com.encircle360.oss.straightmail.wrapper.model;

import tools.jackson.databind.node.POJONode;
import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.util.ModelFactory;

/**
 * FreeMarker template model for Jackson {@link tools.jackson.databind.node.POJONode}.
 *
 * <p>Unwraps the embedded Java object from the {@link tools.jackson.databind.node.POJONode}
 * and delegates to the standard {@link freemarker.ext.beans.BeanModel} for property access.
 */
public class JsonPojoNodeModel extends BeanModel {
    public static final ModelFactory FACTORY =
            (object, wrapper) -> new JsonPojoNodeModel(((POJONode) object).getPojo(), (BeansWrapper) wrapper);

    JsonPojoNodeModel(Object object, BeansWrapper wrapper) {
        super(object, wrapper);
    }
}
