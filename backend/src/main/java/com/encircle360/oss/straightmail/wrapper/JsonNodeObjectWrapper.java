package com.encircle360.oss.straightmail.wrapper;

import com.encircle360.oss.straightmail.wrapper.model.*;
import tools.jackson.databind.node.*;
import freemarker.ext.beans.DefaultMemberAccessPolicy;
import freemarker.ext.util.ModelFactory;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Version;

/**
 * Custom FreeMarker {@link DefaultObjectWrapper} that maps Jackson {@link tools.jackson.databind.JsonNode}
 * types to their corresponding FreeMarker template model implementations.
 *
 * <p>Without this wrapper, FreeMarker cannot natively traverse Jackson's JSON node graph in templates.
 * Registered on the FreeMarker {@link freemarker.template.Configuration} in
 * {@link com.encircle360.oss.straightmail.service.FreemarkerService} before each rendering pass.
 *
 * <p>The following Jackson node types are mapped:
 * <ul>
 *   <li>{@link tools.jackson.databind.node.StringNode} → {@link com.encircle360.oss.straightmail.wrapper.model.JsonTextNodeModel}</li>
 *   <li>{@link tools.jackson.databind.node.NumericNode} → {@link com.encircle360.oss.straightmail.wrapper.model.JsonNumberNodeModel}</li>
 *   <li>{@link tools.jackson.databind.node.BooleanNode} → {@link com.encircle360.oss.straightmail.wrapper.model.JsonBooleanNodeModel}</li>
 *   <li>{@link tools.jackson.databind.node.POJONode} → {@link com.encircle360.oss.straightmail.wrapper.model.JsonPojoNodeModel}</li>
 *   <li>{@link tools.jackson.databind.node.ArrayNode} → {@link com.encircle360.oss.straightmail.wrapper.model.JsonArrayNodeModel}</li>
 *   <li>{@link tools.jackson.databind.node.ObjectNode} → {@link com.encircle360.oss.straightmail.wrapper.model.JsonObjectNodeModel}</li>
 *   <li>{@link tools.jackson.databind.node.NullNode} → {@link com.encircle360.oss.straightmail.wrapper.model.JsonNullNodeModel}</li>
 * </ul>
 */
public class JsonNodeObjectWrapper extends DefaultObjectWrapper {

    /**
     * Constructs the wrapper with the given FreeMarker compatibility version.
     *
     * <p>The member access policy is pinned to {@link DefaultMemberAccessPolicy} whatever the
     * compatibility version: below 2.3.30 FreeMarker falls back to its legacy policy, which lets a
     * template call {@code getClass()}, reach class loaders and reflect its way out of the sandbox.
     *
     * @param incompatibleImprovements the minimum FreeMarker version to enable improvements for
     */
    public JsonNodeObjectWrapper(Version incompatibleImprovements) {
        super(incompatibleImprovements);
        setMemberAccessPolicy(DefaultMemberAccessPolicy.getInstance(Configuration.VERSION_2_3_30));
    }

    @Override
    protected ModelFactory getModelFactory(Class clazz) {
        if (StringNode.class.isAssignableFrom(clazz)) {
            return JsonTextNodeModel.FACTORY;
        } else if (NumericNode.class.isAssignableFrom(clazz)) {
            return JsonNumberNodeModel.FACTORY;
        } else if (BooleanNode.class.isAssignableFrom(clazz)) {
            return JsonBooleanNodeModel.FACTORY;
        } else if (POJONode.class.isAssignableFrom(clazz)) {
            return JsonPojoNodeModel.FACTORY;
        } else if (ArrayNode.class.isAssignableFrom(clazz)) {
            return JsonArrayNodeModel.FACTORY;
        } else if (ObjectNode.class.isAssignableFrom(clazz)) {
            return JsonObjectNodeModel.FACTORY;
        } else if (NullNode.class.isAssignableFrom(clazz)) {
            return JsonNullNodeModel.FACTORY;
        }
        return super.getModelFactory(clazz);
    }
}
