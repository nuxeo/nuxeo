/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     tdelprat
 */
package org.nuxeo.ecm.restapi.jaxrs.io.types;

import java.util.HashSet;
import java.util.Set;

import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.types.Schema;

public class DocumentTypes {

    protected final DocumentType[] docTypes;

    protected Set<Schema> usedSchemas = null;

    public DocumentTypes(DocumentType[] docTypes) {
        this.docTypes = docTypes;
    }

    protected Set<Schema> getUsedSchemas() {
        if (usedSchemas == null) {
            usedSchemas = new HashSet<>();
            for (DocumentType type : docTypes) {
                for (Schema schema : type.getSchemas()) {
                    usedSchemas.add(schema);
                }
            }
        }
        return usedSchemas;
    }

    public Schema[] getSchemas() {
        Set<Schema> schemas = getUsedSchemas();
        return schemas.toArray(new Schema[schemas.size()]);
    }

    public DocumentType[] getDocTypes() {
        return docTypes;
    }

}
