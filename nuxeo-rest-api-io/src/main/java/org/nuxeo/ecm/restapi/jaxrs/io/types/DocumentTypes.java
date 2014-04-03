/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     tdelprat
 */
package org.nuxeo.ecm.restapi.jaxrs.io.types;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.types.Schema;

public class DocumentTypes {

    protected final Schema[] schemas;

    protected final DocumentType[] docTypes;

    public DocumentTypes(Schema[] schemas, DocumentType[] docTypes) {
        this.schemas = schemas;
        this.docTypes = docTypes;
    }

    public List<Schema> getUsedSchemas() {
        List<Schema> result = new ArrayList<Schema>();
        for (DocumentType type : docTypes) {
            for (Schema schema : type.getSchemas()) {
                if (!result.contains(schema)) {
                    result.add(schema);
                }
            }
        }
        return result;
    }

    public Schema[] getSchemas() {
        List<Schema> schemas = getUsedSchemas();
        return schemas.toArray(new Schema[schemas.size()]);
    }

    public DocumentType[] getDocTypes() {
        return docTypes;
    }

}
