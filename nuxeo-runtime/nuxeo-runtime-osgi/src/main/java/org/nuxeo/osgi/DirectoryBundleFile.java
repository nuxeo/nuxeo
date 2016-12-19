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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.osgi;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.osgi.util.CompoundEnumeration;
import org.nuxeo.osgi.util.EntryFilter;
import org.nuxeo.osgi.util.FileIterator;
import org.osgi.framework.Constants;

/**
 * A {@link BundleFile} that is backed by a filesystem directory, for use in test settings from Eclipse or maven.
 */
public class DirectoryBundleFile implements BundleFile {

    public static final String MANIFEST_PATH = "META-INF/MANIFEST.MF";

    protected final File file;

    protected final List<File> files;

    protected final Manifest mf;

    public DirectoryBundleFile(File file) throws IOException {
        this(file, null);
    }

    public DirectoryBundleFile(File file, Manifest mf) throws IOException {
        this.file = file;
        this.files = findFiles(file);
        this.mf = mf == null ? findManifest() : mf;
    }

    protected List<File> findFiles(File file) {
        List<File> files = new ArrayList<>(2);
        files.add(file);
        if (file.getPath().endsWith("/bin")) {
            // hack for Eclipse PDE development
            files.add(file.getParentFile());
        } else if (file.getPath().endsWith("/target/classes")) {
            // hack for maven/tycho development
            files.add(file.getParentFile().getParentFile());
        }
        return files;
    }

    private Enumeration<URL> createEnumeration(File root, final EntryFilter efilter, final boolean recurse) {
        FileIterator it = new FileIterator(root, pathname -> {
            if (pathname.isDirectory()) {
                return recurse;
            }
            return efilter.match(pathname.getName());
        });
        it.setSkipDirs(true);
        return FileIterator.asUrlEnumeration(it);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration<URL> findEntries(String name, String pattern, boolean recurse) {
        EntryFilter efilter = EntryFilter.newFilter(pattern);
        if (files.size() == 1) {
            return createEnumeration(new File(file, name), efilter, recurse);
        } else {
            Enumeration<URL>[] enums = new Enumeration[files.size()];
            int i = 0;
            for (File f : files) {
                enums[i++] = createEnumeration(new File(f, name), efilter, recurse);
            }
            return new CompoundEnumeration<>(enums);
        }
    }

    @Override
    public URL getEntry(String name) {
        for (File file : files) {
            File entry = new File(file, name);
            if (entry.exists()) {
                try {
                    return entry.toURI().toURL();
                } catch (MalformedURLException e) {
                    // ignore
                }
            }
        }
        return null;
    }

    @Override
    public Enumeration<String> getEntryPaths(String path) {
        throw new UnsupportedOperationException("The operation BundleFile.geEntryPaths() is not yet implemented");
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

    protected Manifest findManifest() throws IOException {
        for (File file : files) {
            File entry = new File(file, MANIFEST_PATH);
            if (entry.exists()) {
                try (FileInputStream fis = new FileInputStream(entry)) {
                    return new Manifest(fis);
                }
            }
        }
        String paths = files.stream().map(File::toString).collect(Collectors.joining(", "));
        throw new IOException(String.format("Could not find a file '%s' in paths: %s", MANIFEST_PATH, paths));
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
        List<BundleFile> nested = new ArrayList<>();
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
    public Collection<BundleFile> findNestedBundles(File tmpDir) throws IOException {
        List<BundleFile> nested = new ArrayList<>();
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
        String value = mf.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
        return value == null ? null : BundleManifestReader.removePropertiesFromHeaderValue(value);
    }

    @Override
    public URL getURL() {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
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

    public static void main(String[] args) throws Exception {
        DirectoryBundleFile bf = new DirectoryBundleFile(
                new File("/Users/bstefanescu/work/org.eclipse.ecr/plugins/org.eclipse.ecr.application/bin"));
        Enumeration<URL> urls = bf.findEntries("META-INF", "*.txt", false);
        while (urls.hasMoreElements()) {
            System.out.println(urls.nextElement());
        }
    }

    @Override
    public void close(OSGiAdapter osgi) throws IOException {
    }

}
