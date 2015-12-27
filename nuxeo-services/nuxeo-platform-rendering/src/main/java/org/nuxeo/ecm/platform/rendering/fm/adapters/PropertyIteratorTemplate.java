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

import java.util.Iterator;

import org.nuxeo.ecm.core.api.model.Property;

import freemarker.template.AdapterTemplateModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class PropertyIteratorTemplate extends PropertyWrapper implements TemplateModelIterator, AdapterTemplateModel {

    protected final Iterator<Property> iterator;

    public PropertyIteratorTemplate(DocumentObjectWrapper wrapper, Iterator<Property> iterator) {
        super(wrapper);
        this.iterator = iterator;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Object getAdaptedObject(Class hint) {
        return iterator;
    }

    @Override
    public boolean hasNext() throws TemplateModelException {
        return iterator.hasNext();
    }

    @Override
    public TemplateModel next() throws TemplateModelException {
        return wrap(iterator.next());
    }

}
