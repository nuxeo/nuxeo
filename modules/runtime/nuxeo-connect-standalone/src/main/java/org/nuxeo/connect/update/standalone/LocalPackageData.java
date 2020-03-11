/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.connect.update.standalone;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageData;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.runtime.api.SharedResourceLoader;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class LocalPackageData implements PackageData {

    protected File root;

    // SharedResourceLoader is a URLClassLoader with a public addURL
    protected SharedResourceLoader loader;

    public LocalPackageData(ClassLoader parent, File file) throws IOException {
        this.root = file.getCanonicalFile();
        if (parent == null) {
            parent = Thread.currentThread().getContextClassLoader();
            if (parent == null) {
                parent = getClass().getClassLoader();
            }
        }
        try {
            this.loader = new SharedResourceLoader(new URL[] {}, parent);
            loader.addURL(root.toURI().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to create package class loader. Invalid package root: " + root, e);
        }
    }

    public void setRoot(File file) {
        this.root = file;
    }

    @Override
    public ClassLoader getLoader() {
        return loader;
    }

    @Override
    public File getEntry(String path) {
        return new File(root, path);
    }

    @Override
    public InputStream getEntryAsStream(String path) throws IOException {
        return new FileInputStream(getEntry(path));
    }

    @Override
    public File getManifest() {
        return getEntry(LocalPackage.MANIFEST);
    }

    @Override
    public File getRoot() {
        return root;
    }

    @Override
    public Class<?> loadClass(String name) throws PackageException {
        try {
            return loader.loadClass(name);
        } catch (ClassNotFoundException e) {
            throw new PackageException("Failed to load class " + name + " from package: " + root.getName());
        }
    }

}
