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
package org.nuxeo.ecm.client;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ContentHandlerRegistry {

    protected final Set<ContentHandler<?>> registry =
        new HashSet<ContentHandler<?>>();

    public void addContentHandler(ContentHandler<?> handler) {
        throw new UnsupportedOperationException("not yet");
    }

    public void removeContentHandler(ContentHandler<?> handler) {
        throw new UnsupportedOperationException("not yet");
    }

    List<ContentHandler<?>>[] getContentHandlers() {
        throw new UnsupportedOperationException("not yet");
    }

    List<ContentHandler<?>> getContentHandler(String contentType) {
        throw new UnsupportedOperationException("not yet");
    }

    <T> List<ContentHandler<T>> getContentHandler(Class<T> objectType) {
        throw new UnsupportedOperationException("not yet");
    }

    <T> ContentHandler<T> getContentHandler(String contentType, Class<T> objectType) {
        throw new UnsupportedOperationException("not yet");
    }

}
