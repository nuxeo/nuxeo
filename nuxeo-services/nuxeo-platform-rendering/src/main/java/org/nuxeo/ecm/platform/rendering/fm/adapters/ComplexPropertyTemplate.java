/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.fm.adapters;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;

import freemarker.template.AdapterTemplateModel;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ComplexPropertyTemplate extends PropertyWrapper implements
        TemplateHashModelEx, AdapterTemplateModel {

    protected final Property property;

    public ComplexPropertyTemplate(DocumentObjectWrapper wrapper,
            Property property) {
        super(wrapper);
        this.property = property;
    }

    @SuppressWarnings("unchecked")
    public Object getAdaptedObject(Class hint) {
        return property;
    }

    public TemplateCollectionModel keys() throws TemplateModelException {
        List<String> list = new ArrayList<String>(property.size());
        for (Property p : property.getChildren()) {
            list.add(p.getName());
        }
        return (TemplateCollectionModel) wrapper.wrap(list);
    }

    public int size() throws TemplateModelException {
        return property.size();
    }

    public TemplateCollectionModel values() throws TemplateModelException {
        try {
            List<Object> list = new ArrayList<Object>(property.size());
            for (Property p : property.getChildren()) {
                Object value = p.getValue();
                list.add(value == null ? "" : value);
            }
            return (TemplateCollectionModel) wrapper.wrap(list);
        } catch (PropertyException e) {
            throw new TemplateModelException(
                    "Failed to adapt complex property values", e);
        }
    }

    public TemplateModel get(String name) throws TemplateModelException {
        try {
            Property p = property.get(name);
            return wrap(p);
        } catch (PropertyException e) {
            throw new TemplateModelException(e);
        }
    }

    public boolean isEmpty() throws TemplateModelException {
        return property.size() == 0;
    }

}
