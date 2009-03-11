/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.cmis.client.app;

import java.io.InputStream;
import java.io.OutputStream;

import org.nuxeo.ecm.cmis.ContentManagerException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface SerializationManager {

    void writeEntity(Object object, OutputStream out) throws ContentManagerException;

    <T> T readEntity(Object context, Class<T> type, InputStream in) throws ContentManagerException;

    <T> Feed<T> readFeed(Object context, Class<T> type, InputStream in) throws ContentManagerException;

    <T> SerializationHandler<T> getHandler(Class<T> clazz);

    <T> SerializationHandler<T> getHandler(String contentType);

    void registerHandler(SerializationHandler<?> handler);

    void unregisterHandler(Class<?> clazz);

}
