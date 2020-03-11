/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.rendering.fm.adapters;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * A template for a document's schema, that can still take advantage of the document's prefetched values.
 */
public class SchemaTemplate extends PropertyWrapper implements TemplateHashModel {

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
            if (doc.isPrefetched(schemaName, name)) {
                // simple value already available, don't load DocumentPart
                return wrapper.wrap(doc.getProperty(schemaName, name));
            } else {
                // use normal Property lookup in Part
                return wrap(doc.getPropertyObject(schemaName, name));
            }
        } catch (PropertyException e) {
            throw new TemplateModelException(e);
        }
    }

    @Override
    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

}
