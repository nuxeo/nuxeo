/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.osgi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.StringUtils;
import org.osgi.framework.Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class JarBundleFile implements BundleFile {

    private static final Log log = LogFactory.getLog(JarBundleFile.class);

    protected final JarFile jarFile;


    public JarBundleFile(File file) throws IOException {
        this(new JarFile(file));
    }

    public JarBundleFile(JarFile jarFile) {
        this.jarFile = jarFile;
    }

    @Override
    public Enumeration<URL> findEntries(String name, String pattern,
            boolean recurse) {
        throw new UnsupportedOperationException(
                "The operation BundleFile.findEntries() was not yet implemented");
    }

    @Override
    public URL getEntry(String name) {
        ZipEntry entry = jarFile.getEntry(name);
        if (entry == null) {
            return null;
        }
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        try {
            return new URL("jar:" + new File(jarFile.getName()).toURI().toURL() + "!/" + name);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Enumeration<String> getEntryPaths(String path) {
        throw new UnsupportedOperationException(
                "The operation BundleFile.geEntryPaths() was not yet implemented");
    }

    @Override
    public File getFile() {
        return new File(jarFile.getName());
    }

    @Override
    public String getFileName() {
        String path = jarFile.getName();
        int punix = path.lastIndexOf('/');
        int pwin = path.lastIndexOf('\\');
        int p = punix > pwin ? punix : pwin;
        if (p == -1) {
            return path;
        }
        if (p == 0) {
            return "";
        }
        return path.substring(p + 1);
    }

    @Override
    public String getLocation() {
        return jarFile.getName();
    }

    @Override
    public Manifest getManifest() {
        try {
            return jarFile.getManifest();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public Collection<BundleFile> getNestedBundles(File tmpDir) throws IOException {
        Attributes attrs = jarFile.getManifest().getMainAttributes();
        String cp = attrs.getValue(Constants.BUNDLE_CLASSPATH);
        if (cp == null) {
            cp = attrs.getValue("Class-Path");
        }
        if (cp == null) {
            return null;
        }
        String[] paths = StringUtils.split(cp, ',', true);
        URL base = new URL("jar:"
                + new File(jarFile.getName()).toURI().toURL().toExternalForm() + "!/");
        String fileName = getFileName();
        List<BundleFile> nested = new ArrayList<BundleFile>();
        for (String path : paths) {
            if (path.equals(".")) {
                continue; // TODO
            }
            String location = base + path;
            String name = path.replace('/', '_');
            File dest = new File(tmpDir, fileName + '-' + name);
            try {
                extractNestedJar(jarFile, path, dest);
                nested.add(new NestedJarBundleFile(location, dest));
            } catch (FileNotFoundException e) {
                log.error("A nested jar is referenced in manifest but not found: "
                        + location);
            } catch (IOException e) {
                log.error(e);
            }
        }
        return nested;
    }

    public static void extractNestedJar(JarFile file, String path, File dest) throws IOException {
        InputStream in = null;
        ZipEntry entry = file.getEntry(path);
        try {
            in = file.getInputStream(entry);
            FileUtils.copyToFile(in, dest);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    public static void extractNestedJar(JarFile file, ZipEntry entry, File dest) throws IOException {
        InputStream in = null;
        try {
            in = file.getInputStream(entry);
            FileUtils.copyToFile(in, dest);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    @Override
    public Collection<BundleFile> findNestedBundles(File tmpDir) throws IOException {
        URL base = new URL("jar:"
                + new File(jarFile.getName()).toURI().toURL().toExternalForm() + "!/");
        String fileName = getFileName();
        Enumeration<JarEntry> entries = jarFile.entries();
        List<BundleFile> nested = new ArrayList<BundleFile>();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String path = entry.getName();
            if (entry.getName().endsWith(".jar")) {
                String location = base + path;
                String name = path.replace('/', '_');
                File dest = new File(tmpDir, fileName + '-' + name);
                extractNestedJar(jarFile, entry, dest);
                nested.add(new NestedJarBundleFile(location, dest));
            }
        }
        return nested;
    }

    @Override
    public String getSymbolicName() {
        try {
            String value = jarFile.getManifest().getMainAttributes().getValue(
                    Constants.BUNDLE_SYMBOLICNAME);
            return value != null ? BundleManifestReader.removePropertiesFromHeaderValue(value)
                    : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public URL getURL() {
        try {
            return new File(jarFile.getName()).toURI().toURL();
        } catch (Exception e) {
            return null;
        }
    }

    public URL getJarURL() {
        try {
            String url = new File(jarFile.getName()).toURI().toURL().toExternalForm();
            return new URL("jar:" + url + "!/");
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public boolean isJar() {
        return true;
    }

    @Override
    public String toString() {
        return getLocation();
    }

    @Override
    protected void finalize() throws IOException {
        if (jarFile != null) {
            jarFile.close();
        }
    }

}
