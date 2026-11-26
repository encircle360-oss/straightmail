package com.encircle360.oss.straightmail.wrapper.model;

import freemarker.ext.util.ModelFactory;

/**
 * FreeMarker template model factory for Jackson {@link tools.jackson.databind.node.NullNode}.
 *
 * <p>JSON {@code null} values are mapped to FreeMarker's {@code null} (i.e. the factory returns
 * {@code null} from {@code createModel}), which FreeMarker treats as a missing/null value in templates.
 *
 * <p>Not instantiable; only the static {@link #FACTORY} is used.
 */
public class JsonNullNodeModel {

    private JsonNullNodeModel() {
    }

    /**
     * Factory that maps a JSON null node to FreeMarker {@code null}.
     */
    public static final ModelFactory FACTORY = (object, wrapper) -> null;
}
