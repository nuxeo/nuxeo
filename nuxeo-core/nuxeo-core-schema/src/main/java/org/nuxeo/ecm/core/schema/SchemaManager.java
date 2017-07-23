/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
     * Whether or not to ignore any previous values when setting complex properties.
     *
     * @return {@code true} if setting a complex property ignores any previous values
     * @since 9.3
     */
    boolean getClearComplexPropertyBeforeSet();

}
