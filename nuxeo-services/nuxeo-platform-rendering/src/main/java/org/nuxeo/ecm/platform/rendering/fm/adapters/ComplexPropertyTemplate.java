/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.fm.adapters;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.Property;

import freemarker.core.CollectionAndSequence;
import freemarker.template.AdapterTemplateModel;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ComplexPropertyTemplate extends PropertyWrapper implements TemplateHashModelEx, AdapterTemplateModel {

    protected final Property property;

    public ComplexPropertyTemplate(DocumentObjectWrapper wrapper, Property property) {
        super(wrapper);
        this.property = property;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Object getAdaptedObject(Class hint) {
        return property;
    }

    @Override
    public TemplateCollectionModel keys() throws TemplateModelException {
        List<String> list = new ArrayList<>(property.size());
        for (Property p : property.getChildren()) {
            list.add(p.getName());
        }
        return new CollectionAndSequence(new SimpleSequence(list, wrapper));
    }

    @Override
    public int size() throws TemplateModelException {
        return property.size();
    }

    @Override
    public TemplateCollectionModel values() throws TemplateModelException {
        try {
            List<Object> list = new ArrayList<>(property.size());
            for (Property p : property.getChildren()) {
                Object value = p.getValue();
                list.add(value == null ? "" : value);
            }
            return new CollectionAndSequence(new SimpleSequence(list, wrapper));
        } catch (PropertyException e) {
            throw new TemplateModelException("Failed to adapt complex property values", e);
        }
    }

    @Override
    public TemplateModel get(String name) throws TemplateModelException {
        try {
            Property p = property.get(name);
            return wrap(p);
        } catch (PropertyException e) {
            throw new TemplateModelException(e);
        }
    }

    @Override
    public boolean isEmpty() throws TemplateModelException {
        return property.size() == 0;
    }

}
