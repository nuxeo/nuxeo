/*
 * (C) Copyright 2007-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.schema;

import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;

/**
 * A provider of types (property types, schemas, document types, facets).
 */
public interface TypeProvider {

    /** Low level generic type lookup. */
    Type getType(String schema, String name);

    /** Gets a property type. */
    Type getType(String name);

    /** Gets the list of property types. */
    Type[] getTypes();

    /** Gets a schema. */
    Schema getSchema(String schema);

    /** Gets the list of schemas. */
    Schema[] getSchemas();

    /** Gets a document type. */
    DocumentType getDocumentType(String docType);

    /** Gets the list of document types. */
    DocumentType[] getDocumentTypes();

    /** Gets a facet. */
    CompositeType getFacet(String name);

    /** Gets the list of facets. */
    CompositeType[] getFacets();

}
