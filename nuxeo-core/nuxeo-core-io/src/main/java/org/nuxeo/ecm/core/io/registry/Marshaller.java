/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.core.io.registry;

import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.io.registry.reflect.Supports;

/**
 * Interface of Java type converter.
 * <p>
 * You must add {@link Setup} annotation to every class implementing this interface. You should add {@link Supports}
 * annotation to define supported mimetype. You can add {@link Inject} annotation to your properties to get current
 * {@link RenderingContext} or any Nuxeo service.
 * </p>
 * <p>
 * To get an instance of this class with injected properties, call
 * {@link MarshallerRegistry#getInstance(RenderingContext, Class)}.
 * </p>
 *
 * @param <EntityType> The managed Java type.
 * @since 7.2
 */
public interface Marshaller<EntityType> {

    /**
     * Checks if this marshaller can handle the marshalling request.
     * <p>
     * Please note it's useless to check that clazz is an instance of EntityType or if generic type and entity type are
     * compatible (unlike JAX-RS which just checks the clazz, not the generic type). It's also useless to check
     * {@link Supports} is compatible with mediatype. This is already done by the {@link MarshallerRegistry}
     * </p>
     * <p>
     * This method implementation can use injected properties. So you can check the current {@link RenderingContext} to
     * accept or reject a marshalling request.
     * </p>
     *
     * @param clazz The type to marshall.
     * @param genericType The generic type to marshall.
     * @param mediatype The managed mimetype.
     * @return true if this converter handle the request, false otherwise.
     * @since 7.2
     */
    boolean accept(Class<?> clazz, Type genericType, MediaType mediatype);

}
