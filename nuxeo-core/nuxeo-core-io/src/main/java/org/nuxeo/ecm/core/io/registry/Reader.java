/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
     * @param mediatype The input mediatype.
     * @param in The input of this marshaller.
     * @throws IOException If some error append while reading entity from in.
     * @since 7.2
     */
    EntityType read(Class<?> clazz, Type genericType, MediaType mediaType, InputStream in) throws IOException;

}
