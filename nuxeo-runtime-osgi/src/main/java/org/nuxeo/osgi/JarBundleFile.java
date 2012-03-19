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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.osgi.util.EntryFilter;
import org.osgi.framework.Constants;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class JarBundleFile implements BundleFile {

    private static final Log log = LogFactory.getLog(JarBundleFile.class);

    protected JarFile jarFile;

    protected String urlBase;

    public JarBundleFile(File file) throws IOException {
        this(new JarFile(file));
    }

    public JarBundleFile(JarFile jarFile) {
        this.jarFile = jarFile;
        try {
            urlBase = "jar:" + new File(jarFile.getName()).toURI().toURL() + "!/";
        } catch (MalformedURLException e) {
            log.error("Failed to convert bundle location to an URL: "+jarFile.getName()+". Bundle getEntry will not work.", e);
        }
    }

    @Override
    public Enumeration<URL> findEntries(String name, String pattern,
            boolean recurse) {
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        String prefix;
        if (name.length() == 0) {
            name = null;
            prefix = "";
        } else if (!name.endsWith("/") ){
            prefix = name+"/";
        } else {
            prefix = name;
        }
        int len = prefix.length();
        EntryFilter filter = EntryFilter.newFilter(pattern);
        Enumeration<JarEntry> entries = jarFile.entries();
        ArrayList<URL> result = new ArrayList<URL>();
        try {
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String ename = entry.getName();
                if (name != null && !ename.startsWith(prefix)) {
                    continue;
                }
                int i = ename.lastIndexOf('/');
                if (!recurse) {
                    if (i > -1) {
                        if (ename.indexOf('/', len) > -1) {
                            continue;
                        }
                    }
                }
                String n = i > -1 ? ename.substring(i+1) : ename;
                if (filter.match(n)) {
                    result.add(getEntryUrl(ename));
                }
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return Collections.enumeration(result);
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
            return new URL(urlBase + name);
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

    protected final URL getEntryUrl(String name) throws MalformedURLException {
        return new URL(urlBase + name);
    }

    static class UrlEntryEnum implements Enumeration<URL> {
        Enumeration<? extends ZipEntry> e;
        ZipFile zf;
        String prefix;
        UrlEntryEnum(ZipFile zf, Enumeration<? extends ZipEntry> e) throws MalformedURLException {
            prefix = "jar:" + new File(zf.getName()).toURI().toURL() + "!/";
            this.e = e;
        }
        @Override
        public boolean hasMoreElements() {
            return e.hasMoreElements();
        }
        @Override
        public URL nextElement() {
            try {
                return new URL(prefix+e.nextElement().getName());
            } catch (MalformedURLException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void close(OSGiAdapter osgi) throws IOException {
        if (jarFile == null) {
            return;
        }
        try {
            osgi.getJarFileCloser().close(jarFile);
        } finally {
            jarFile = null;
        }
    }

}
