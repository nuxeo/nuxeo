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
package org.nuxeo.ecm.core.schema.types;

import java.util.Collection;

import org.nuxeo.ecm.core.schema.Namespace;
import org.nuxeo.ecm.core.schema.types.constraints.Constraint;

/**
 * A complex type is tree-like structure of named elements which can be of any type.
 * <p>
 * Complex types can describe and validate java <code>Map objects</code>.
 */
public interface ComplexType extends Type {

    /**
     * Gets the namespace used by this complex type.
     *
     * @return the namespace or {@link Namespace#DEFAULT_NS} if none was specified
     */
    Namespace getNamespace();

    /**
     * Gets the field with the given name.
     * <p>
     * If the name is non-prefixed the first matching field is returned if any is found. If the name is prefixed then
     * the right field is returned if any is found.
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
     * If the given name is not prefixed it will be prefixed with the type prefix. If one was specified otherwise the
     * default prefix will be used (e.g. "" - no prefix). If the given name is prefixed it will be stored as is (using
     * the specified prefix).
     *
     * @param name the field name
     * @param type the field type
     * @param defaultValue an optional default value (null if none)
     * @param flags optional flags
     * @return the created field
     */
    Field addField(String name, Type type, String defaultValue, int flags, Collection<Constraint> constraints);

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
     * Tests whether this type has any field defined.
     * <p>
     * If a complex type has no fields, it is considered as unstructured and it accepts any field with any type and
     * name.
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

}
