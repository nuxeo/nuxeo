/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.connect.update.standalone;

import groovy.lang.GroovyClassLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageData;
import org.nuxeo.connect.update.PackageException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class LocalPackageData implements PackageData {

    protected File root;

    protected GroovyClassLoader loader;

    public LocalPackageData(ClassLoader parent, File file) throws IOException {
        this.root = file.getCanonicalFile();
        if (parent == null) {
            parent = Thread.currentThread().getContextClassLoader();
            if (parent == null) {
                parent = getClass().getClassLoader();
            }
        }
        try {
            this.loader = new GroovyClassLoader(parent);
            loader.addURL(root.toURI().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(
                    "Failed to create package class loader. Invalid package root: "
                            + root, e);
        }
    }

    public void setRoot(File file) {
        this.root = file;
    }

    public ClassLoader getLoader() {
        return loader;
    }

    public File getEntry(String path) {
        return new File(root, path);
    }

    public InputStream getEntryAsStream(String path) throws IOException {
        return new FileInputStream(getEntry(path));
    }

    public File getManifest() {
        return getEntry(LocalPackage.MANIFEST);
    }

    public File getRoot() {
        return root;
    }

    public Class<?> loadClass(String name) throws PackageException {
        try {
            return loader.loadClass(name);
        } catch (Exception e) {
            throw new PackageException("Failed to load class " + name
                    + " from package: " + root.getName());
        }
    }

}
