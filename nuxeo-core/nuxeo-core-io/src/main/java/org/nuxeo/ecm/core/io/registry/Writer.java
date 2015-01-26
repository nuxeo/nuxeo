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
import java.io.OutputStream;
import java.lang.reflect.Type;

import javax.ws.rs.core.MediaType;

/**
 * Interface of Java type to mimetype converter.
 * <p>
 * see {@link Marshaller} for more details.
 * </p>
 *
 * @param <EntityType> The managed Java type.
 * @since 7.2
 */
public interface Writer<EntityType> extends Marshaller<EntityType> {

    /**
     * Writes the entity to out {@link OutputStream} using mediatype format.
     * <p>
     * This method implementation can use injected properties.
     * </p>
     *
     * @param entity The entity to marshall.
     * @param clazz The requested marshalled class.
     * @param genericType The requested marshalled generic type.
     * @param mediatype The target mediatype.
     * @param out The output of this marshaller.
     * @throws IOException If some error append while writing entity to out.
     * @since 7.2
     */
    public void write(EntityType entity, Class<?> clazz, Type genericType, MediaType mediatype, OutputStream out)
            throws IOException;

}
