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

package org.nuxeo.osgi.application;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.nuxeo.osgi.OSGiAdapter;
import org.nuxeo.osgi.nio.BundleWalker;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class ApplicationLoader {

    protected final OSGiAdapter osgi;

    protected boolean extractNestedJARs = false;

    protected boolean scanForNestedJARs = false;

    private String[] patterns = BundleWalker.DEFAULT_PATTERNS;

    private final File tmpDir;

    protected ApplicationLoader(OSGiAdapter osgi) {
        this.osgi = osgi;
        tmpDir = new File(osgi.getDataDir(), "nested-bundles");
        tmpDir.mkdirs();
    }

    public void installBundle(URI location) throws BundleException {
        osgi.install(location);
    }

    public File getNestedBundleDirectory() {
        return tmpDir;
    }

    public OSGiAdapter getOSGi() {
        return osgi;
    }

    public void setScanForNestedJARs(boolean scanForNestedJARs) {
        osgi.setProperty(OSGiAdapter.SCAN_FOR_NESTED_JARS,
                Boolean.toString(scanForNestedJARs));
    }

    public boolean getScanForNestedJARs() {
        return Boolean.valueOf(osgi.getProperty(OSGiAdapter.SCAN_FOR_NESTED_JARS, "false")).booleanValue();
    }

    public void setPatterns(String[] patterns) {
        this.patterns = patterns;
    }

    public String[] getPatterns() {
        return patterns;
    }

    /**
     * Scans and install the given directory for OSGi bundles and regular JARs
     * and fills the given lists appropriately.
     * <p>
     * *
     *
     * @param root the directory to recursively scan
     * @param bundles the list to fill with found bundles
     * @param jars the list to fill with found jars
     * @throws IOException
     */
    public void install(File root, List<Bundle> bundles) throws IOException {
        BundleInstaller callback = new BundleInstaller(bundles);
        BundleWalker visitor = new BundleWalker(osgi, callback, patterns);
        visitor.visit(root);
    }

    public class BundleInstaller implements BundleWalker.Callback {

        final List<Bundle> bundles;

        public BundleInstaller(List<Bundle> bundles) {
            this.bundles = bundles;
        }

        @Override
        public void visitBundle(Bundle bundle) {
            bundles.add(bundle);
        }

        public List<Bundle> getBundles() {
            return bundles;
        }

    }

}
