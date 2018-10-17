/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.Set;

import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;

/**
 * The Schema Manager manages core document types, schemas, facets and field types.
 */
public interface SchemaManager extends TypeProvider {

    /**
     * Returns the field with given xpath, or null if not found.
     */
    Field getField(String xpath);

    /**
     * Returns the field with given parent field and sub name, or null if not found.
     *
     * @since 7.2
     */
    Field getField(Field field, String subFieldName);

    Schema getSchemaFromPrefix(String schemaPrefix);

    Schema getSchemaFromURI(String schemaURI);

    /**
     * Returns the names of all document types that have given facet.
     *
     * @param facet
     * @return null or the names as a guaranteed non-empty set.
     */
    Set<String> getDocumentTypeNamesForFacet(String facet);

    /**
     * Return the names of all document types extending the given one, which is included.
     *
     * @param docType
     * @return null or the set of names.
     */
    Set<String> getDocumentTypeNamesExtending(String docType);

    int getDocumentTypesCount();

    /**
     * Returns true if {@code docType} is or extends {@code superType}, false otherwise.
     *
     * @since 5.9.4
     */
    boolean hasSuperType(String docType, String superType);

    /**
     * Returns the types of the children that can be created inside a given {@code type} type.
     *
     * @since 8.4
     */
    Set<String> getAllowedSubTypes(String type);

    /**
     * @return the deprecated properties handler
     * @since 9.2
     */
    PropertyDeprecationHandler getDeprecatedProperties();

    /**
     * @return the removed properties handler
     * @since 9.2
     */
    PropertyDeprecationHandler getRemovedProperties();

    /**
     * Whether or not to ignore any previous values when setting complex properties.
     *
     * @return {@code true} if setting a complex property ignores any previous values
     * @since 9.3
     */
    boolean getClearComplexPropertyBeforeSet();

    /**
     * Whether we allow to write the dublincore schema on a version.
     *
     * @return {@code true} if write to the dublincore schema of a version is allowed
     * @since 10.3
     */
    boolean getAllowVersionWriteForDublinCore();

}
