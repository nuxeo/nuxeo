/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FileResourceStore implements ResourceStore {

    private static final Log log = LogFactory.getLog(FileResourceStore.class);

    protected File root;

    public FileResourceStore(File root) throws IOException {
        this.root = root.getCanonicalFile();
    }

    public File getRoot() {
        return root;
    }

    public final File getFile(String name) {
        return new File(root, name);
    }

    public boolean exists(String name) {
        return getFile(name).exists();
    }

    public long lastModified(String name) {
        return getFile(name).lastModified();
    }

    public URL getURL(String name) {
        try {
            File file = getFile(name);
            if (file.exists()) {
                return file.toURI().toURL();
            }
        } catch (IOException e) {
            log.error("Failed to transform file to URL: " + name, e);
        }
        return null;
    }

    public byte[] getBytes(String name) {
        InputStream in = getStream(name);
        if (in != null) {
            try {
                return IOUtils.toByteArray(in);
            } catch (IOException e) {
                log.error("Failed to read file: " + name, e);
            }
        }
        return null;
    }

    public InputStream getStream(String name) {
        try {
            return new FileInputStream(getFile(name));
        } catch (IOException e) {
        }
        return null;
    }

    public void remove(String name) {
        getFile(name).delete();
    }

    public void put(String name, byte[] data) throws IOException {
        FileUtils.writeByteArrayToFile(getFile(name), data);
    }

    public void put(String name, InputStream data) throws IOException {
        FileUtils.copyInputStreamToFile(data, getFile(name));
    }

    public String getLocation() {
        return root.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof FileResourceStore) {
            FileResourceStore store = (FileResourceStore) obj;
            return store.root.equals(root);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return root.hashCode();
    }

    @Override
    public String toString() {
        return "FileResourceStore: " + root;
    }

}
