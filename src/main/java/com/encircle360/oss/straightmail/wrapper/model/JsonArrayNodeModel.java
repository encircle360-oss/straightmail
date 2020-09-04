package com.encircle360.oss.straightmail.wrapper.model;

import com.fasterxml.jackson.databind.node.ArrayNode;

import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.util.ModelFactory;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateSequenceModel;

public class JsonArrayNodeModel extends BeanModel
    implements
    TemplateCollectionModel,
    TemplateSequenceModel {
    public static final ModelFactory FACTORY =
        (object, wrapper) -> new JsonArrayNodeModel(object, (BeansWrapper) wrapper);
    private final int length;

    JsonArrayNodeModel(Object array, BeansWrapper wrapper) {
        super(array, wrapper);
        ArrayNode arrayNode = (ArrayNode) array;
        length = arrayNode.size();
    }

    @Override
    public TemplateModelIterator iterator() {
        return new Iterator();
    }

    @Override
    public TemplateModel get(int index) throws TemplateModelException {
        try {
            return wrap(((ArrayNode) object).get(index));
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    @Override
    public int size() {
        return length;
    }

    @Override
    public boolean isEmpty() {
        return length == 0;
    }

    private class Iterator
        implements
        TemplateSequenceModel,
        TemplateModelIterator {
        private int position = 0;

        public boolean hasNext() {
            return position < length;
        }

        public TemplateModel get(int index) throws TemplateModelException {
            return JsonArrayNodeModel.this.get(index);
        }

        public TemplateModel next() throws TemplateModelException {
            return position < length ? get(position++) : null;
        }

        public int size() {
            return JsonArrayNodeModel.this.size();
        }
    }
}
