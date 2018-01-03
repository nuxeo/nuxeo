/*
 * (C) Copyright 2016-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.storage.marklogic;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.runtime.api.Framework;

/**
 * Schema manager extension.
 *
 * @since 8.10.
 */
// TODO merge this code in SchemaManagerImpl
class MarkLogicSchemaManager {

    private final SchemaManager schemaManager;

    public MarkLogicSchemaManager() {
        this.schemaManager = Framework.getService(SchemaManager.class);
    }

    public Field computeField(String fullName, String element) {
        Field field = schemaManager.getField(element);
        if (field == null) {
            if (element.indexOf(':') > -1) {
                throw new QueryParseException("No such property: " + fullName);
            }
            // check without prefix
            // TODO precompute this in SchemaManagerImpl
            for (Schema schema : schemaManager.getSchemas()) {
                if (schema == null || !StringUtils.isBlank(schema.getNamespace().prefix)) {
                    // schema with prefix, do not consider as candidate
                    continue;
                }
                field = schema.getField(element);
                if (field != null) {
                    break;
                }
            }
            if (field == null) {
                throw new QueryParseException("No such property: " + fullName);
            }
        }
        return field;
    }

    public List<String> getDocumentTypes() {
        // TODO precompute in SchemaManager
        return Stream.of(schemaManager.getDocumentTypes()).map(DocumentType::getName).collect(Collectors.toList());
    }

    public Set<String> getNoPerDocumentQueryFacets() {
        return schemaManager.getNoPerDocumentQueryFacets();
    }

    public Set<String> getMixinDocumentTypes(String mixin) {
        Set<String> types = schemaManager.getDocumentTypeNamesForFacet(mixin);
        return types == null ? Collections.emptySet() : types;
    }

}
