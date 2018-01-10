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
import java.util.Collection;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.platform.rendering.api.DefaultDocumentView;

import freemarker.core.CollectionAndSequence;
import freemarker.template.AdapterTemplateModel;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * TODO document template should not be aware of rendering context ?
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DocumentTemplate implements TemplateHashModelEx, AdapterTemplateModel {

    protected final ObjectWrapper wrapper;

    protected final DocumentModel doc;

    public DocumentTemplate(ObjectWrapper wrapper, DocumentModel doc) {
        this.doc = doc;
        this.wrapper = wrapper;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Object getAdaptedObject(Class hint) {
        return doc;
    }

    public DocumentModel getDocument() {
        return doc;
    }

    @Override
    public TemplateModel get(String key) throws TemplateModelException {
        try {
            Object value = DefaultDocumentView.DEFAULT.get(doc, key);
            if (value != DefaultDocumentView.UNKNOWN) {
                return wrapper.wrap(value);
            }
        } catch (PropertyException e) {
            throw new TemplateModelException("Failed to get document field: " + key, e);
        }
        return null;
    }

    public CoreSession getSession() {
        return doc.getCoreSession();
    }

    /**
     * A doc model is never empty.
     */
    @Override
    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public Collection<String> getRawKeys() {
        return DefaultDocumentView.DEFAULT.getFields().keySet();
    }

    @Override
    public TemplateCollectionModel keys() throws TemplateModelException {
        return new CollectionAndSequence(new SimpleSequence(getRawKeys(), wrapper));
    }

    public Collection<Object> getRawValues() throws TemplateModelException {
        List<Object> values = new ArrayList<Object>();
        Collection<DefaultDocumentView.Field> fields = DefaultDocumentView.DEFAULT.getFields().values();
        for (DefaultDocumentView.Field field : fields) {
            values.add(field.getValue(doc));
        }
        return values;
    }

    @Override
    public TemplateCollectionModel values() throws TemplateModelException {
        return new CollectionAndSequence(new SimpleSequence(getRawValues(), wrapper));
    }

    @Override
    public int size() throws TemplateModelException {
        return DefaultDocumentView.DEFAULT.size(doc);
    }

}
