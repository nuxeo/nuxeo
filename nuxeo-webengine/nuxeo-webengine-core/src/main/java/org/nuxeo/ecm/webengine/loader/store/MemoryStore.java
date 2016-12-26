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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class MemoryStore implements ResourceStore {

    protected final Map<String, byte[]> store;

    protected String location;

    public MemoryStore() {
        this(new HashMap<>());
    }

    public MemoryStore(Map<String, byte[]> store) {
        this.store = store;
        this.location = "java:" + System.identityHashCode(this);
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

    public URL getURL(String name) { // TODO not yet implemented
        return null;
    }

    public long lastModified(String name) {
        return 0;
    }

    public void put(String name, InputStream data) throws IOException {
        store.put(name, IOUtils.toByteArray(data));
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
