package com.encircle360.oss.straightmail.wrapper;

import com.encircle360.oss.straightmail.wrapper.model.JsonArrayNodeModel;
import com.encircle360.oss.straightmail.wrapper.model.JsonBooleanNodeModel;
import com.encircle360.oss.straightmail.wrapper.model.JsonNullNodeModel;
import com.encircle360.oss.straightmail.wrapper.model.JsonNumberNodeModel;
import com.encircle360.oss.straightmail.wrapper.model.JsonObjectNodeModel;
import com.encircle360.oss.straightmail.wrapper.model.JsonPojoNodeModel;
import com.encircle360.oss.straightmail.wrapper.model.JsonTextNodeModel;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.fasterxml.jackson.databind.node.TextNode;

import freemarker.ext.util.ModelFactory;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Version;

public class JsonNodeObjectWrapper extends DefaultObjectWrapper {
    public JsonNodeObjectWrapper(Version incompatibleImprovements) {
        super(incompatibleImprovements);
    }

    @Override
    protected ModelFactory getModelFactory(Class clazz) {
        if (TextNode.class.isAssignableFrom(clazz)) {
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