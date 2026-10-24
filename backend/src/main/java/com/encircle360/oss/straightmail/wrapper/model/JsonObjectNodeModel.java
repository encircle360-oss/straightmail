package com.encircle360.oss.straightmail.wrapper.model;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import freemarker.core.CollectionAndSequence;
import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.IteratorModel;
import freemarker.ext.util.ModelFactory;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * FreeMarker template model for Jackson {@link tools.jackson.databind.node.ObjectNode}.
 *
 * <p>Supports key-based property access (e.g. {@code ${obj.fieldName}} in templates),
 * iteration over keys and values, and size/empty checks.
 */
public class JsonObjectNodeModel extends BeanModel {
    public static final ModelFactory FACTORY =
            (object, wrapper) -> new JsonObjectNodeModel(object, (BeansWrapper) wrapper);

    JsonObjectNodeModel(Object object, BeansWrapper wrapper) {
        super(object, wrapper);
    }

    @Override
    public TemplateModel get(String key) throws TemplateModelException {
        ObjectNode objectNode = (ObjectNode) object;
        final JsonNode jsonNode = objectNode.get(key);

        if (jsonNode == null) {
            return null;
        }
        return wrap(jsonNode);
    }

    @Override
    public boolean isEmpty() {
        ObjectNode objectNode = (ObjectNode) object;
        return objectNode.isEmpty();
    }

    @Override
    public int size() {
        ObjectNode objectNode = (ObjectNode) object;
        return objectNode.size();
    }

    @Override
    public TemplateCollectionModel keys() {
        ObjectNode objectNode = (ObjectNode) object;
        return new IteratorModel(objectNode.propertyNames().iterator(), wrapper);
    }

    @Override
    public TemplateCollectionModel values() {
        ObjectNode objectNode = (ObjectNode) object;

        List<JsonNode> values = new ArrayList<>(size());
        final Iterator<Map.Entry<String, JsonNode>> it = objectNode.properties().iterator();
        it.forEachRemaining(next -> values.add(next.getValue()));

        SimpleSequence simpleSequence = new SimpleSequence(values, wrapper);

        return new CollectionAndSequence(simpleSequence);
    }
}
