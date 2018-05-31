/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
     * {@link ObjectResolver#validate(Object,Object)}
     *
     * @since 10.2
     */
    default boolean validate(Object context) {
        return validate();
    }

    /**
     * {@link ObjectResolver#fetch(Object)}
     *
     * @since 7.1
     */
    Object fetch();

    /**
     * {@link ObjectResolver#fetch(Object,Object)}
     *
     * @since 10.2
     */
    default Object fetch(Object context) {
        return fetch();
    }

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
