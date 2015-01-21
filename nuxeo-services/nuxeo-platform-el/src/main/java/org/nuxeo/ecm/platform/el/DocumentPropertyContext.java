/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
