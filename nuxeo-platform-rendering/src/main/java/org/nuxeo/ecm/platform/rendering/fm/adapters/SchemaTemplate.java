/*
 * Copyright (c) 2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.rendering.fm.adapters;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Map;

import org.nuxeo.common.utils.Null;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.platform.rendering.fm.adapters.SchemaTemplate.DocumentSchema;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * A template for a document's schema, that can still take advantage of the
 * document's prefetched values.
 */
public class SchemaTemplate extends PropertyWrapper implements
        TemplateHashModel {

    private final DocumentModel doc;

    private final String schemaName;

    public static class DocumentSchema {
        public final DocumentModel doc;

        public final String schemaName;

        public DocumentSchema(DocumentModel doc, String schemaName) {
            this.doc = doc;
            this.schemaName = schemaName;
        }
    }

    public SchemaTemplate(DocumentObjectWrapper wrapper, DocumentSchema schema) {
        super(wrapper);
        this.doc = schema.doc;
        this.schemaName = schema.schemaName;
    }

    @Override
    public TemplateModel get(String name) throws TemplateModelException {
        try {
            // check prefetch
            // TODO add doc.isPrefetched(name) or something equivalent
            Field prefetchField = DocumentModelImpl.class.getDeclaredField("prefetch");
            prefetchField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Serializable> prefetch = (Map<String, Serializable>) prefetchField.get(doc);
            if (prefetch != null && prefetch.containsKey(schemaName + '.' + name)) {
                return wrapper.wrap(doc.getProperty(schemaName, name));
            }
            // else use normal Property lookup in Part
            return wrap(doc.getPart(schemaName).get(name));
        } catch (ClientException e) {
            throw new TemplateModelException(e);
        } catch (Exception e) {
            throw new TemplateModelException(e);
        }
    }

    @Override
    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

}
