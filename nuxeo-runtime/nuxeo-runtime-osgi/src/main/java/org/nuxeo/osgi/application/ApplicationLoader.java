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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.osgi.application;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.nuxeo.common.utils.FileNamePattern;
import org.nuxeo.osgi.BundleFile;
import org.nuxeo.osgi.NestedJarBundleFile;
import org.nuxeo.osgi.OSGiAdapter;
import org.osgi.framework.BundleException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class ApplicationLoader {

    protected final OSGiAdapter osgi;

    protected boolean extractNestedJARs = false;

    protected boolean scanForNestedJARs = false;

    private FileNamePattern[] patterns = BundleWalker.DEFAULT_PATTERNS;

    private final File tmpDir;

    protected ApplicationLoader(OSGiAdapter osgi) {
        this.osgi = osgi;
        tmpDir = new File(osgi.getDataDir(), "nested-bundles");
        tmpDir.mkdirs();
    }

    public abstract void installBundle(BundleFile bundleFile) throws BundleException;

    public abstract void loadBundle(BundleFile bundleFile);

    public abstract void loadJAR(BundleFile bundleFile);

    public File getNestedBundleDirectory() {
        return tmpDir;
    }

    public OSGiAdapter getOSGi() {
        return osgi;
    }

    public void setExtractNestedJARs(boolean extractNestedJARs) {
        this.extractNestedJARs = extractNestedJARs;
    }

    public boolean getExtractNestedJARs() {
        return extractNestedJARs;
    }

    public void setScanForNestedJARs(boolean scanForNestedJARs) {
        this.scanForNestedJARs = scanForNestedJARs;
    }

    public boolean getScanForNestedJARs() {
        return scanForNestedJARs;
    }

    public void setPatterns(FileNamePattern[] patterns) {
        this.patterns = patterns;
    }

    public FileNamePattern[] getPatterns() {
        return patterns;
    }

    /**
     * Scans and loads the given directory for OSGi bundles and regular JARs and fills the given lists appropriately.
     * <p>
     * Loading means registering with the given shared class loader each bundle found.
     *
     * @param root the directory to recursively scan
     * @param bundles the list to fill with found bundles
     * @param jars the list to fill with found jars
     */
    public void load(File root, List<BundleFile> bundles, List<BundleFile> jars) {
        BundleFileLoader callback = new BundleFileLoader(bundles, jars);
        BundleWalker visitor = new BundleWalker(callback, patterns);
        visitor.visit(root);
    }

    /**
     * Installs all given bundle deployments.
     *
     * @param bundleFiles
     * @throws BundleException
     */
    public void installAll(Collection<BundleFile> bundleFiles) throws BundleException {
        for (BundleFile bundleFile : bundleFiles) {
            installBundle(bundleFile);
        }
    }

    /**
     * Installs all bundles found in the given directory.
     * <p>
     * The directory is recursively searched for bundles.
     *
     * @param root the tree root
     */
    public void install(File root) {
        BundleInstaller callback = new BundleInstaller();
        BundleWalker visitor = new BundleWalker(callback, patterns);
        visitor.visit(root);
    }

    /**
     * Scans the given directory for OSGi bundles and regular JARs and fills the given lists appropriately.
     *
     * @param root the directory to recursively scan
     * @param bundles the list to fill with found bundles
     * @param ljars the list to fill with found jars
     */
    public void scan(File root, List<BundleFile> bundles, List<BundleFile> ljars) {
        BundleFileScanner callback = new BundleFileScanner(bundles, ljars);
        BundleWalker visitor = new BundleWalker(callback, patterns);
        visitor.visit(root);
    }

    /**
     * Installs bundles as they are discovered by the bundle visitor.
     *
     * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
     */
    public class BundleInstaller extends DefaultCallback {

        @Override
        public void visitBundle(BundleFile bundleFile) throws IOException {
            loadBundle(bundleFile);
            visitNestedBundles(bundleFile);
        }

        @Override
        public void visitJar(BundleFile bundleFile) throws IOException {
            loadJAR(bundleFile);
            visitNestedBundles(bundleFile);
        }

    }

    public class BundleFileScanner extends DefaultCallback {

        final List<BundleFile> bundles;

        final List<BundleFile> jars;

        public BundleFileScanner(List<BundleFile> bundles, List<BundleFile> jars) {
            this.bundles = bundles;
            this.jars = jars;
        }

        @Override
        public void visitBundle(BundleFile bundleFile) throws IOException {
            bundles.add(bundleFile);
            visitNestedBundles(bundleFile);
        }

        @Override
        public void visitJar(BundleFile bundleFile) throws IOException {
            jars.add(bundleFile);
            visitNestedBundles(bundleFile);
        }

        public List<BundleFile> getBundles() {
            return bundles;
        }

        public List<BundleFile> getJARs() {
            return jars;
        }

    }

    public class BundleFileLoader extends DefaultCallback {

        final List<BundleFile> bundles;

        final List<BundleFile> jars;

        public BundleFileLoader(List<BundleFile> bundles, List<BundleFile> jars) {
            this.bundles = bundles;
            this.jars = jars;
        }

        @Override
        public void visitBundle(BundleFile bundleFile) throws IOException {
            // System.out.println(">>>> FOUND BUNDLE: "+bundleFile.getFileName());
            loadBundle(bundleFile);
            bundles.add(bundleFile);
            visitNestedBundles(bundleFile);
        }

        @Override
        public void visitJar(BundleFile bundleFile) throws IOException {
            // System.out.println(">>>> FOUND JAR: "+bundleFile.getFileName());
            loadJAR(bundleFile);
            jars.add(bundleFile);
            visitNestedBundles(bundleFile);
        }

        public List<BundleFile> getBundles() {
            return bundles;
        }

        public List<BundleFile> getJARs() {
            return jars;
        }

    }

    public abstract class DefaultCallback implements BundleWalker.Callback {

        @Override
        public void visitBundle(BundleFile bundleFile) throws IOException {
            visitNestedBundles(bundleFile);
        }

        @Override
        public void visitJar(BundleFile bundleFile) throws IOException {
            visitNestedBundles(bundleFile);
        }

        public void visitNestedBundles(BundleFile bundleFile) throws IOException {
            if (bundleFile instanceof NestedJarBundleFile) {
                return; // do not allows more than one level of nesting
            }
            if (extractNestedJARs) {
                Collection<BundleFile> bundles;
                if (scanForNestedJARs) {
                    bundles = bundleFile.findNestedBundles(tmpDir);
                } else { // use manifest to find nested jars
                    bundles = bundleFile.getNestedBundles(tmpDir);
                }
                if (bundles == null || bundles.isEmpty()) {
                    return;
                }
                for (BundleFile bundle : bundles) {
                    if (bundle.getSymbolicName() != null) {
                        visitBundle(bundle);
                    } else {
                        visitJar(bundle);
                    }
                }
            }
        }
    }

}
