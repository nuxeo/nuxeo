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

package org.nuxeo.runtime.launcher;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.nuxeo.common.utils.FileNamePattern;
import org.nuxeo.osgi.OSGiAdapter;
import org.osgi.framework.BundleException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class ApplicationLoader {

    protected final OSGiAdapter osgi;
    protected boolean extractNestedJARs = false;
    protected boolean scanForNestedJARs = false;

    private FileNamePattern[] patterns = BundleVisitor.DEFAULT_PATTERNS;

    private final File tmpDir;

    protected ApplicationLoader(OSGiAdapter osgi) {
        this.osgi = osgi;
        tmpDir = new File(osgi.getWorkingDir(), "nested-bundles");
        tmpDir.mkdirs();
    }


    public abstract void installBundle(BundleFile bundleFile) throws BundleException;

    public abstract void loadBundle(BundleFile bundleFile);

    public abstract void loadJAR(BundleFile bundleFile);


    /**
     * @param extractNestedJARs The extractNestedJARs to set.
     */
    public void setExtractNestedJARs(boolean extractNestedJARs) {
        this.extractNestedJARs = extractNestedJARs;
    }

    /**
     * @return the extractNestedJARs.
     */
    public boolean getExtractNestedJARs() {
        return extractNestedJARs;
    }

    /**
     * @param scanForNestedJARs The scanForNestedJARs to set.
     */
    public void setScanForNestedJARs(boolean scanForNestedJARs) {
        this.scanForNestedJARs = scanForNestedJARs;
    }

    /**
     * @return the scanForNestedJARs.
     */
    public boolean getScanForNestedJARs() {
        return scanForNestedJARs;
    }

    /**
     * @param patterns the patterns to set.
     */
    public void setPatterns(FileNamePattern[] patterns) {
        this.patterns = patterns;
    }

    /**
     * @return the patterns.
     */
    public FileNamePattern[] getPatterns() {
        return patterns;
    }

    /**
     * Scans and loads the given directory for OSGi bundles and regular JARs and
     * fills the given lists appropriately.
     * <p>
     * Loading means registering with the given shared class loader each bundle found
     *
     * @param root the directory to recursively scan
     * @param bundles the list to fill with found bundles
     * @param jars the list to fill with found jars
     */
    public void load(File root, List<BundleFile> bundles, List<BundleFile> jars) {
        BundleFileLoader callback = new BundleFileLoader(bundles, jars);
        BundleVisitor visitor = new BundleVisitor(callback, patterns);
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
        BundleVisitor visitor = new BundleVisitor(callback, patterns);
        visitor.visit(root);
    }

    /**
     * Scans the given directory for OSGi bundles and regular JARs and fills
     * the given lists appropriately.
     *
     * @param root the directory to recursively scan
     * @param bundles the list to fill with found bundles
     * @param ljars the list to fill with found jars
     */
    public void scan(File root, List<BundleFile> bundles, List<BundleFile> ljars) {
        BundleFileScanner callback = new BundleFileScanner(bundles, ljars);
        BundleVisitor visitor = new BundleVisitor(callback, patterns);
        visitor.visit(root);
    }

    /**
     * Installs bundles as they are discovered by the bundle visitor.
     *
     * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
     */
    public class BundleInstaller  extends DefaultCallback {

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
            //System.out.println(">>>> FOUND BUNDLE: "+bundleFile.getFileName());
            loadBundle(bundleFile);
            bundles.add(bundleFile);
            visitNestedBundles(bundleFile);
        }

        @Override
        public void visitJar(BundleFile bundleFile) throws IOException {
            //System.out.println(">>>> FOUND JAR: "+bundleFile.getFileName());
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


    public abstract class DefaultCallback implements BundleVisitor.Callback {

        public void visitBundle(BundleFile bundleFile) throws IOException {
            visitNestedBundles(bundleFile);
        }

        public void visitJar(BundleFile bundleFile) throws IOException {
            visitNestedBundles(bundleFile);
        }

        public void visitNestedBundles(BundleFile bundleFile) throws IOException {
            if (bundleFile instanceof NestedJarBundleFile) {
                return; //do not allows more than one level of nesting
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
                //System.out.println(">>> FOUND NESTED JARS: "+bundles);
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
