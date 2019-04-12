/*
 * (C) Copyright 2015-2019 Nuxeo (http://nuxeo.com/) and others.
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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

import javax.ws.rs.core.MediaType;

/**
 * Interface of mimetype to Java type converter.
 * <p>
 * see {@link Marshaller} for more details.
 * </p>
 *
 * @param <EntityType> The managed Java type.
 * @since 7.2
 */
public interface Reader<EntityType> extends Marshaller<EntityType> {

    /**
     * Read the entity from in {@link InputStream} using mediatype format.
     * <p>
     * This method implementation can use injected properties.
     * </p>
     *
     * @param clazz The requested marshalled class.
     * @param genericType The requested marshalled generic type.
     * @param mediaType The input media type.
     * @param in The input of this marshaller.
     * @throws IOException If some error append while reading entity from in.
     * @since 7.2
     */
    EntityType read(Class<?> clazz, Type genericType, MediaType mediaType, InputStream in) throws IOException;

}
