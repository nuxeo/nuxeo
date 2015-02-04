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

package org.nuxeo.ecm.core.api.model.resolver;

import java.util.List;

import org.nuxeo.ecm.core.schema.types.resolver.ObjectResolver;

/**
 * Object to resolve entities referenced by a property. Works only on properties whose type has an object resolver.
 *
 * @since 7.1
 */
public interface PropertyObjectResolver {

    /**
     * {@link ObjectResolver#getManagedClasses()}
     *
     * @since 7.2
     */
    List<Class<?>> getManagedClasses();

    /**
     * {@link ObjectResolver#validate(Object)}
     *
     * @since 7.1
     */
    boolean validate();

    /**
     * {@link ObjectResolver#fetch(Object)}
     *
     * @since 7.1
     */
    Object fetch();

    /**
     * {@link ObjectResolver#fetch(Class, Object)}
     *
     * @since 7.1
     */
    <T> T fetch(Class<T> type);

    /**
     * Gets a reference to the object and set the corresponding value to this property.
     * {@link ObjectResolver#fetch(Class, Object)}
     *
     * @since 7.1
     */
    void setObject(Object object);

    /**
     * Returns the underlying {@link ObjectResolver}.
     *
     * @since 7.1
     */
    ObjectResolver getObjectResolver();

}