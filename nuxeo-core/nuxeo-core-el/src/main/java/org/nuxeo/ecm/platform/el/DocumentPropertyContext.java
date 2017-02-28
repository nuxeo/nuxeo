/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.el;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DocumentPropertyContext {

    final DocumentModel doc;

    final String schema;

    public DocumentPropertyContext(DocumentModel doc, String schema) {
        this.doc = doc;
        this.schema = schema;
    }

    /**
     * @since 7.2
     */
    public DocumentModel getDoc() {
        return doc;
    }

    /**
     * @since 7.2
     */
    public String getSchema() {
        return schema;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append("DocumentPropertyContext");
        buf.append(" {");
        buf.append(" doc=");
        buf.append(doc);
        buf.append(", schema=");
        buf.append(schema);
        buf.append('}');
        return buf.toString();
    }

}
