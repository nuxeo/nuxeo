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
import java.util.Collection;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendering.api.DefaultDocumentView;

import freemarker.template.AdapterTemplateModel;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * TODO document template should not be aware of rendering context ?
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DocumentTemplate implements TemplateHashModelEx,
        AdapterTemplateModel {

    protected final ObjectWrapper wrapper;

    protected final DocumentModel doc;

    public DocumentTemplate(ObjectWrapper wrapper, DocumentModel doc) {
        this.doc = doc;
        this.wrapper = wrapper;
    }

    @SuppressWarnings("unchecked")
    public Object getAdaptedObject(Class hint) {
        return doc;
    }

    public DocumentModel getDocument() {
        return doc;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        try {
            Object value = DefaultDocumentView.DEFAULT.get(doc, key);
            if (value != DefaultDocumentView.UNKNOWN) {
                return wrapper.wrap(value);
            }
        } catch (Exception e) {
            throw new TemplateModelException("Failed to get document field: "
                    + key, e);
        }
        return null;
    }

    public CoreSession getSession() {
        return CoreInstance.getInstance().getSession(doc.getSessionId());
    }

    /**
     * A doc model is never empty.
     */
    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public Collection<String> getRawKeys() {
        return DefaultDocumentView.DEFAULT.getFields().keySet();
    }

    public TemplateCollectionModel keys() throws TemplateModelException {
        return (TemplateCollectionModel) wrapper.wrap(getRawKeys());
    }

    public Collection<Object> getRawValues() throws TemplateModelException {
        List<Object> values = new ArrayList<Object>();
        try {
            Collection<DefaultDocumentView.Field> fields = DefaultDocumentView.DEFAULT.getFields().values();
            for (DefaultDocumentView.Field field : fields) {
                values.add(field.getValue(doc));
            }
        } catch (Exception e) {
            throw new TemplateModelException("failed to fetch field values", e);
        }
        return values;
    }

    public TemplateCollectionModel values() throws TemplateModelException {
        return (TemplateCollectionModel) wrapper.wrap(getRawValues());
    }

    public int size() throws TemplateModelException {
        return DefaultDocumentView.DEFAULT.size(null);
    }

}
