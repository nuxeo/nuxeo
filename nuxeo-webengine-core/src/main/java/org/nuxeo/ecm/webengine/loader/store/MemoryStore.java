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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.utils.FileUtils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class MemoryStore implements ResourceStore {

    protected final Map<String, byte[]> store;
    protected String location;

    public MemoryStore() {
        this(new HashMap<String, byte[]>());
    }

    public MemoryStore(Map<String, byte[]> store) {
        this.store = store;
        this.location = "java:"+System.identityHashCode(this);
    }

    public boolean exists(String name) {
        return store.containsKey(name);
    }

    public byte[] getBytes(String name) {
        return store.get(name);
    }

    public InputStream getStream(String name) {
        byte[] data = store.get(name);
        return data == null ? null : new ByteArrayInputStream(data);
    }

    public URL getURL(String name) { //TODO not yet implemented
        return null;
    }

    public long lastModified(String name) {
        return 0;
    }

    public void put(String name, InputStream data) throws IOException {
        store.put(name, FileUtils.readBytes(data));
    }

    public void put(String name, byte[] data) throws IOException {
        store.put(name, data);
    }

    public void remove(String name) {
        store.remove(name);
    }

    public String getLocation() {
        return "java";
    }

    @Override
    public String toString() {
        return getLocation();
    }
}
