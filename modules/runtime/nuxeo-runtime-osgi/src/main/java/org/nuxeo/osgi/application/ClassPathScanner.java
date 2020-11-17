/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.osgi.application;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarFile;

import org.nuxeo.osgi.BundleFile;
import org.nuxeo.osgi.DirectoryBundleFile;
import org.nuxeo.osgi.JarBundleFile;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ClassPathScanner {

    protected boolean scanForNestedJARs = true;

    protected final Callback callback;

    /**
     * If set points to a set of path prefixes to be excluded form bundle processing
     */
    protected String[] blackList;

    public ClassPathScanner(Callback callback) {
        this.callback = callback;
    }

    public ClassPathScanner(Callback callback, boolean scanForNestedJars, String[] blackList) {
        this.callback = callback;
        scanForNestedJARs = scanForNestedJars;
        this.blackList = blackList;
    }

    public void setScanForNestedJARs(boolean scanForNestedJars) {
        scanForNestedJARs = scanForNestedJars;
    }

    public void scan(List<File> classPath) {
        for (File file : classPath) {
            scan(file);
        }
    }

    public void scan(File file) {
        String path = file.getAbsolutePath();
        if (!(path.endsWith(".jar") || path.endsWith(".rar") || path.endsWith(".sar") || path.endsWith("_jar")
                || path.endsWith("_rar") || path.endsWith("_sar"))) {
            return;
        }
        if (blackList != null) {
            for (String prefix : blackList) {
                if (path.startsWith(prefix)) {
                    return;
                }
            }
        }
        try {
            BundleFile bf;
            if (file.isFile()) {
                JarFile jar = new JarFile(file);
                bf = new JarBundleFile(jar);
            } else if (file.isDirectory()) {
                bf = new DirectoryBundleFile(file);
            } else {
                return;
            }
            File nestedJARsDir;
            if (bf.getSymbolicName() == null) { // a regular jar
                nestedJARsDir = callback.handleJar(bf);
            } else { // an osgi bundle
                nestedJARsDir = callback.handleBundle(bf);
            }
            if (nestedJARsDir != null) {
                Collection<BundleFile> nested = extractNestedJars(bf, nestedJARsDir);
                if (nested != null) {
                    for (BundleFile nestedJar : nested) {
                        callback.handleNestedJar(nestedJar);
                    }
                }
            }
        } catch (IOException e) {
            // ignore exception since some manifest may be invalid (invalid ClassPath entries) etc.
        }
    }

    public Collection<BundleFile> extractNestedJars(BundleFile bf, File nestedBundlesDir) throws IOException {
        Collection<BundleFile> bundles = null;
        if (scanForNestedJARs) {
            bundles = bf.findNestedBundles(nestedBundlesDir);
        } else { // use manifest to find nested jars
            bundles = bf.getNestedBundles(nestedBundlesDir);
        }
        if (bundles != null && bundles.isEmpty()) {
            bundles = null;
        }
        return bundles;
    }

    public interface Callback {

        /**
         * A nested JAR was found on the class path. Usually a callback should handle this by adding the JAR to a class
         * loader
         * <p>
         * The callback should return a directory to be used to extract nested JARs from this JAR.
         * <p>
         * The callback may return null to skip nested JAR extraction
         *
         * @param bf the JAR found
         */
        void handleNestedJar(BundleFile bf);

        /**
         * A JAR was found on the class path. Usually a callback should handle this by adding the JAR to a class loader.
         * <p>
         * The callback should return a directory to be used to extract nested JARs from this JAR.
         * <p>
         * The callback may return null to skip nested JAR extraction.
         *
         * @param bf the JAR found
         * @return the folder to be used to extract JARs or null to skip extraction
         */
        File handleJar(BundleFile bf);

        /**
         * A Bundle was found on the class path. Usually a callback should handle this by adding the Bundle to a class
         * loader and installing it in an OSGi framework
         * <p>
         * The callback should return a directory to be used to extract nested JARs from this JAR.
         * <p>
         * The callback may return null to skip nested JAR extraction.
         *
         * @param bf the JAR found
         * @return the folder to be used to extract JARs or null to skip extraction
         */
        File handleBundle(BundleFile bf);

    }

}
