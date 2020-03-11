/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.loader.store;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * When implementing a resource store you should implement equals and hashCode method. A store is equals to another if
 * the store location is the same.
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
     * A string that uniquely identify the location of that store. Two stores are considered equals if their locations
     * are the same.
     */
    String getLocation();

}
