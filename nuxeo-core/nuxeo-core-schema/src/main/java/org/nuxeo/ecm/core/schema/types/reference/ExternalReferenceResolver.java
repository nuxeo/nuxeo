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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.schema.types.reference;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;

/**
 * External references are document field with a simple type whose value refers to an external business entity. Objects
 * implementing this interface are able to resolve the entity using the reference.
 *
 * @since 7.1
 */
public interface ExternalReferenceResolver {

    /**
     * Configure this resolver.
     *
     * @param parameters A map of parameter whose keys are parameter names and map value are corresponding values.
     * @throws IllegalArgumentException If some parameter are not compatible with this resolver.
     * @since 7.1
     */
    void configure(Map<String, String> parameters) throws IllegalArgumentException;

    /**
     * Provides this resolver name.
     *
     * @return The resolver name.
     * @since 7.1
     */
    String getName();

    /**
     * Provides this resolver parameters.
     *
     * @return A map containing <parameter name , parameter value>
     * @since 7.1
     */
    Map<String, Serializable> getParameters();

    /**
     * Validates some value references an existing entity.
     *
     * @param value The reference.
     * @return true if value could be resolved as an existing external reference, false otherwise.
     * @throws IllegalStateException If this resolver has not been configured.
     * @since 7.1
     */
    boolean validate(Object value) throws IllegalStateException;

    /**
     * Provides the entity referenced by a value.
     *
     * @param value The reference.
     * @return The referenced entity, null if no entity matches the value.
     * @throws IllegalStateException If this resolver has not been configured.
     * @since 7.1
     */
    Object fetch(Object value) throws IllegalStateException;

    /**
     * Provides the entity referenced by a value, return the entity as expected type.
     *
     * @param value The reference.
     * @return The referenced entity, null if no entity matches the value or if this entity cannot be converted as type.
     * @throws IllegalStateException If this resolver has not been configured.
     * @since 7.1
     */
    <T> T fetch(Class<T> type, Object value) throws IllegalStateException;

    /**
     * Generates a reference to an entity.
     *
     * @param value The entity.
     * @return A reference to the entity.
     * @throws IllegalStateException If this resolver has not been configured.
     * @throws IllegalArgumentException If the entity cannot be referenced. For example if it does not exists anymore.
     * @throws NullPointerException If entity is null
     * @since 7.1
     */
    Serializable getReference(Object entity) throws IllegalStateException, IllegalArgumentException;

    /**
     * Provides an error message to display when some invalid value does not match existing entity.
     *
     * @param invalidValue The invalid value that don't match any entity.
     * @param locale The language in which the message should be generated.
     * @return A message in the specified language or
     * @since 7.1
     */
    String getConstraintErrorMessage(Object invalidValue, Locale locale);

}
