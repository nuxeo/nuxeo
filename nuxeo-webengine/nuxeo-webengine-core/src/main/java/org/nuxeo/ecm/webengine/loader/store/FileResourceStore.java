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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
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
            log.error("Failed to transform file to URL: "+name, e);
        }
        return null;
    }

    public byte[] getBytes(String name) {
        InputStream in = getStream(name);
        if (in != null) {
            try {
                return FileUtils.readBytes(in);
            } catch (IOException e) {
                log.error("Failed to read file: "+name, e);
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
        FileUtils.writeFile(getFile(name), data);
    }

    public void put(String name, InputStream data) throws IOException {
        FileUtils.copyToFile(data, getFile(name));
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
            FileResourceStore store = (FileResourceStore)obj;
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
        return "FileResourceStore: "+root;
    }

}
