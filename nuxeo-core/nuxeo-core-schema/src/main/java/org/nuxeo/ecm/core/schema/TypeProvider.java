/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.Schema;

/**
 * A provider of types (schemas, document types, facets).
 */
public interface TypeProvider {

    /** Gets a schema. */
    Schema getSchema(String schema);

    /** Gets the list of schemas. */
    Schema[] getSchemas();

    /** Gets a document type. */
    DocumentType getDocumentType(String docType);

    /** Gets the list of document types. */
    DocumentType[] getDocumentTypes();
    
    /**
     * Gets the list of document types excluded from copy.
     * @since 11.1
     */
    default List<DocumentType> getSpecialDocumentTypes() {
        return new ArrayList<>();
    }

    /** Gets a facet. */
    CompositeType getFacet(String name);

    /** Gets the list of facets. */
    CompositeType[] getFacets();

    /** Finds which facets are configured as no-per-instance-query. */
    Set<String> getNoPerDocumentQueryFacets();

    /** Gets the schemas on a proxy for a document of the given type. */
    List<Schema> getProxySchemas(String docType);

    /** Checks if a schema is on a proxy for a document of the given type. */
    boolean isProxySchema(String schema, String docType);

}
