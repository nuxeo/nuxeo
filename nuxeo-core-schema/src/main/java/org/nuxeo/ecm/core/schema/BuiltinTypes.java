/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.schema;

import org.nuxeo.ecm.core.schema.types.AnyType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.SchemaImpl;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public final class BuiltinTypes {

    public static DocumentTypeImpl T_DOCUMENT;

    private static SchemaImpl systemSchema;

    // This is an utility class.
    private BuiltinTypes() { }

    public static void registerBuiltinTypes(SchemaManager typeMgr) {
//        ComplexTypeImpl acp = new ComplexTypeImpl(null, ACP_TYPE);
//
//        systemSchema = new SchemaImpl(SECURITY_SCHEMA_NAME, new Namespace(SECURITY_SCHEMA_URI, SECURITY_SCHEMA_PREFIX));
//        systemSchema.addField(ACP, acp);
        //typeMgr.registerSchema(systemSchema);

//        Schema[] defSchemas = new Schema[] {systemSchema};
        String[] defSchemas = null;
        T_DOCUMENT = new DocumentTypeImpl(null,
                TypeConstants.DOCUMENT,
                defSchemas, null, DocumentTypeImpl.T_DOCUMENT);
        typeMgr.registerDocumentType(T_DOCUMENT);
        typeMgr.registerType(AnyType.INSTANCE);
    }

    public static Schema getSystemSchema() {
        return systemSchema;
    }

}
