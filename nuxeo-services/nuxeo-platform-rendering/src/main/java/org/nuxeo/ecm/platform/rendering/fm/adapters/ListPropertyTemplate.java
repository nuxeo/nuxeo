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
 */
public class ListPropertyTemplate extends PropertyWrapper implements TemplateCollectionModel, TemplateSequenceModel,
        AdapterTemplateModel {

    protected final ListProperty property;

    public ListPropertyTemplate(DocumentObjectWrapper wrapper, ListProperty property) {
        super(wrapper);
        this.property = property;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Object getAdaptedObject(Class hint) {
        return property;
    }

    @Override
    public TemplateModelIterator iterator() throws TemplateModelException {
        return new PropertyIteratorTemplate(wrapper, property.iterator());
    }

    @Override
    public TemplateModel get(int arg0) throws TemplateModelException {
        Property p = property.get(arg0);
        return wrap(p);
    }

    @Override
    public int size() throws TemplateModelException {
        return property.size();
    }

}
