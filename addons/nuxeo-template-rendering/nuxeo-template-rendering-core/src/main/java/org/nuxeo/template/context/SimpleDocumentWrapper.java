/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.template.context;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendering.api.DefaultDocumentView;
import org.nuxeo.ecm.platform.rendering.fm.adapters.SchemaTemplate;

public class SimpleDocumentWrapper {

    protected final DocumentModel doc;

    public SimpleDocumentWrapper(DocumentModel doc) {
        this.doc = doc;
    }

    public Object get(String key) {
        Object value = DefaultDocumentView.DEFAULT.get(doc, key);
        if (value != DefaultDocumentView.UNKNOWN) {
            return wrap(value);
        }
        return null;
    }

    protected Object wrap(Object obj) {
        if (obj instanceof SchemaTemplate.DocumentSchema) {
            return new SimpleSchemaWrapper((SchemaTemplate.DocumentSchema) obj);
        }
        return obj;
    }
}
