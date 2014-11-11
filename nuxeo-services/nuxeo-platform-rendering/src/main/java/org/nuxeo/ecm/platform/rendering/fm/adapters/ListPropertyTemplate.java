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

import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;

import freemarker.template.AdapterTemplateModel;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateSequenceModel;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ListPropertyTemplate extends PropertyWrapper implements
        TemplateCollectionModel, TemplateSequenceModel, AdapterTemplateModel {

    protected final ListProperty property;

    public ListPropertyTemplate(DocumentObjectWrapper wrapper,
            ListProperty property) {
        super(wrapper);
        this.property = property;
    }

    @SuppressWarnings("unchecked")
    public Object getAdaptedObject(Class hint) {
        return property;
    }

    public TemplateModelIterator iterator() throws TemplateModelException {
        return new PropertyIteratorTemplate(wrapper, property.iterator());
    }

    public TemplateModel get(int arg0) throws TemplateModelException {
        Property p = property.get(arg0);
        return wrap(p);
    }

    public int size() throws TemplateModelException {
        return property.size();
    }

}
