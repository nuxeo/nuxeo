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

import java.util.Set;

import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.TypeHelper;


/**
 * A Type Manager manages ECM document types, schemas and field types.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface SchemaManager extends TypeProvider {

    void registerType(Type type);

    Type unregisterType(String name);

    /**
     * Gets the types defined by the given schema.
     *
     * @param schema
     * @return
     */
    Type[] getTypes(String schema);

    int getTypesCount();

    void registerSchema(Schema schema);

    Schema unregisterSchema(String name);

    /**
     * Get the field given a prefixed name.
     *
     * @param prefixedName
     * @return the field or null if none
     */
    Field getField(String prefixedName);

    Schema getSchemaFromPrefix(String schemaPrefix);

    Schema getSchemaFromURI(String schemaURI);

    int getSchemasCount();

    void registerDocumentType(DocumentType docType);

    //void registerDocumentType(DocumentTypeDescriptor docTypeDesc);

    DocumentType unregisterDocumentType(String name);

    /**
     * Returns the names of all document types that have given facet.
     *
     * @param facet
     * @return null or the names as a guaranteed non-empty set.
     */
    Set<String> getDocumentTypeNamesForFacet(String facet);

    /**
     * Return the names of all document types extending the given one, which
     * is included.
     *
     * @param docType
     * @return null or the set of names.
     */
    Set<String> getDocumentTypeNamesExtending(String docType);

    int getDocumentTypesCount();

    /**
     * Unregisters all types.
     * Useful for testing.
     */
    void clear();

    /**
     * Get the schema definition.
     *
     * @return
     */
    // XXX: This should be refactored to get a serializable InputStream.
    String getXmlSchemaDefinition(String name);


    void registerHelper(String schema, String type, TypeHelper helper);

    void unregisterHelper(String schema, String type);

    TypeHelper getHelper(String schema, String type);

}
