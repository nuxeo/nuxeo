/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.schema.types;

import java.util.Collection;

import org.nuxeo.ecm.core.schema.Namespace;
import org.nuxeo.ecm.core.schema.TypeRef;

/**
 * A complex type is tree-like structure of named elements which can be of any
 * type.
 * <p>
 * Complex types can describe and validate java <code>Map objects</code>.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface ComplexType extends Type {

    /**
     * Tests whether this type is structured or not.
     * <p>
     * An unstructured complex type accepts any field name and type.
     * <p>
     * By default, complex types inherit their unstructured property. If a type
     * has no super-type then it is considered unstructured if it is not
     * specifying any field.
     *
     * @return true if unstructured, false otherwise
     */
    boolean isUnstructured();

    /**
     * Gets the namespace used by this complex type.
     *
     * @return the namespace or {@link Namespace#DEFAULT_NS} if none was
     *         specified
     */
    Namespace getNamespace();

    /**
     * Gets the field with the given name.
     * <p>
     * If the name is non-prefixed the first matching field is returned if any
     * is found. If the name is prefixed then the right field is returned if any
     * is found.
     *
     * @param name the field name
     * @return the field
     */
    Field getField(String name);

    /**
     * Gets the field having the given name.
     *
     * @param name the name
     * @return the field or null if no field with that name was found
     */
    Field getField(QName name);

    /**
     * Adds a field to this complex type.
     * <p>
     * If the given name is not prefixed it will be prefixed with the type
     * prefix. If one was specified otherwise the default prefix will be used
     * (e.g. "" - no prefix). If the given name is prefixed it will be stored as
     * is (using the specified prefix).
     *
     * @param name the field name
     * @param type the field type
     * @return the created field
     */
    Field addField(String name, TypeRef<? extends Type> type);

    /**
     * Adds a field to this complex type.
     * <p>
     * If the given name is not prefixed it will be prefixed with the type
     * prefix. If one was specified otherwise the default prefix will be used
     * (e.g. "" - no prefix). If the given name is prefixed it will be stored as
     * is (using the specified prefix).
     *
     * @param name the field name
     * @param type the field type
     * @param defaultValue an optional default value (null if none)
     * @param flags optional flags
     * @return the created field
     */
    Field addField(String name, TypeRef<? extends Type> type, String defaultValue, int flags);

    /**
     * Adds a field to this complex type.
     *
     * @param name the field name
     * @param type the field type
     * @return the created field
     */
    Field addField(QName name, TypeRef<? extends Type> type);

    /**
     * Adds a field to this complex type.
     *
     * @param name the field name
     * @param type the field type
     * @param defaultValue an optional default value (null if none)
     * @param flags optional flags
     *
     * Possible values are:
     * <ul>
     * <li>{@link Field#NILLABLE}
     * <li> {@link Field#CONSTANT}
     * </ul>
     * @return the created field
     */
    Field addField(QName name, TypeRef<? extends Type> type, String defaultValue, int flags);

    /**
     * Tests whether this type defines the given field name.
     * <p>
     * The name is supposed to be non prefixed.
     *
     * @param name the field name
     * @return true if the field exists, false otherwise
     */
    boolean hasField(String name);

    /**
     * Tests whether this type defines the given field name.
     *
     * @param name the field name
     * @return true if the field exists, false otherwise
     */
    boolean hasField(QName name);

    /**
     * Tests whether this type has any field defined.
     * <p>
     * If a complex type has no fields, it is considered as unstructured and it
     * accepts any field with any type and name.
     *
     * @return true if the at least one field exists, false otherwise
     */
    boolean hasFields();

    /**
     * Gets all fields as a (field name, field type) map.
     *
     * @return the fields map
     */
    Collection<Field> getFields();

    /**
     * Gets the number of fields defined for this complex type.
     *
     * @return the fields count
     */
    int getFieldsCount();

    @Override
    TypeRef<? extends ComplexType> getRef();

}
