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

package org.nuxeo.ecm.core.schema.ejb;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.TypeProvider;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@Stateless
@Remote(TypeProvider.class)
@Local(TypeProviderLocal.class)
public class TypeProviderBean implements TypeProviderLocal {

    private final SchemaManager typeProvider;

    public TypeProviderBean() {
        // use the local runtime service as the backend
        typeProvider = Framework.getLocalService(SchemaManager.class);
    }

    public Type getType(String name) {
        return typeProvider.getType(name);
    }

    public Type getType(String schema, String name) {
        return typeProvider.getType(schema, name);
    }

    public Schema getSchema(String schema) {
        return typeProvider.getSchema(schema);
    }

    public DocumentType getDocumentType(String docType) {
        return typeProvider.getDocumentType(docType);
    }

    public Schema[] getSchemas() {
        return typeProvider.getSchemas();
    }

    public Type[] getTypes() {
        return typeProvider.getTypes();
    }

    public DocumentType[] getDocumentTypes() {
        return typeProvider.getDocumentTypes();
    }
}
