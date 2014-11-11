/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.osgi;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.JarUtils;
import org.nuxeo.common.utils.StringUtils;
import org.osgi.framework.Constants;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DirectoryBundleFile implements BundleFile {

    protected final File file;
    protected final Manifest mf;

    /**
     * Hack for PDE projects.
     * This method tests if the target bundle is in a PDE environment and copy the
     * bundle data to bin directory.
     * @param file
     * @return
     * @throws IOException
     */
    static Manifest getManifest(File file) throws IOException {
        if (file.getPath().endsWith("/bin")) {
            return copyBundleDataAndGetManifest(file);
        }
        return JarUtils.getDirectoryManifest(file);
    }

    static Manifest copyBundleDataAndGetManifest(File file) throws IOException {
        File project = file.getParentFile();
        File[] files = project.listFiles();
        if (files != null) {
            for (File f : files) {
                String name = f.getName();
                if (!name.equals("bin") && !name.equals("src") && !name.equals("lib")) {
                    copyFileContent(f, file);
                }
            }
        }
        return JarUtils.getDirectoryManifest(file);
    }

    static void copyFileContent(File file, File toDir) throws IOException {
        if (file.isDirectory()) {
            toDir = new File(toDir, file.getName());
            toDir.mkdirs();
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    copyFileContent(f, toDir);
                }
            }
        } else {
            FileUtils.copyFile(file, new File(toDir, file.getName()));
        }
    }

    public DirectoryBundleFile(File file) throws IOException {
        this(file, getManifest(file));
    }

    public DirectoryBundleFile(File file, Manifest mf) {
        this.file = file;
        this.mf = mf;
    }

    @Override
    public Enumeration<URL> findEntries(String name, String pattern,
            boolean recurse) {
        throw new UnsupportedOperationException(
        "The operation BundleFile.findEntries() is not yet implemented");
    }

    @Override
    public URL getEntry(String name) {
        File entry = new File(file, name);
        if (entry.exists()) {
            try {
                return entry.toURI().toURL();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    @Override
    public Enumeration<String> getEntryPaths(String path) {
        throw new UnsupportedOperationException(
        "The operation BundleFile.geEntryPaths() is not yet implemented");
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public String getFileName() {
        return file.getName();
    }

    @Override
    public String getLocation() {
        return file.getPath();
    }

    @Override
    public Manifest getManifest() {
        return mf;
    }

    @Override
    public Collection<BundleFile> getNestedBundles(File tmpDir) throws IOException {
        Attributes attrs = mf.getMainAttributes();
        String cp = attrs.getValue(Constants.BUNDLE_CLASSPATH);
        if (cp == null) {
            cp = attrs.getValue("Class-Path");
        }
        if (cp == null) {
            return null;
        }
        String[] paths = StringUtils.split(cp, ',', true);
        List<BundleFile> nested = new ArrayList<BundleFile>();
        for (String path : paths) {
            File nestedBundle = new File(file, path);
            if (nestedBundle.isDirectory()) {
                nested.add(new DirectoryBundleFile(nestedBundle));
            } else {
                nested.add(new JarBundleFile(nestedBundle));
            }
        }
        return nested;
    }

    @Override
    public Collection<BundleFile> findNestedBundles(File tmpDir)
    throws IOException {
        List<BundleFile> nested = new ArrayList<BundleFile>();
        File[] files = FileUtils.findFiles(file, "*.jar", true);
        for (File jar : files) {
            if (jar.isDirectory()) {
                nested.add(new DirectoryBundleFile(jar));
            } else {
                nested.add(new JarBundleFile(jar));
            }
        }
        return nested;
    }

    @Override
    public String getSymbolicName() {
        try {
            String value = mf.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
            return value == null ? null
                    : BundleManifestReader.removePropertiesFromHeaderValue(value);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public URL getURL() {
        try {
            return file.toURI().toURL();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public boolean isJar() {
        return false;
    }

    @Override
    public String toString() {
        return getLocation();
    }

}
