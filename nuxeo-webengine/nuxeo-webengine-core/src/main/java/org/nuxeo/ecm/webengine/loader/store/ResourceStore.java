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
package org.nuxeo.ecm.webengine.loader.store;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * When implementing a resource store you should implement equals and hashCode method.
 * A store is equals to another if the store location is the same.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface ResourceStore {

    void put(final String name, final InputStream data) throws IOException;

    void put(final String name, final byte[] data) throws IOException;

    InputStream getStream(final String name);

    byte[] getBytes(final String name);

    void remove(final String name);

    boolean exists(final String name);

    long lastModified(String name);

    URL getURL(final String name);

    /**
     * A string that uniquely identify the location of that store.
     * Two stores are considered equals if their locations are the same.
     */
    String getLocation();

}
