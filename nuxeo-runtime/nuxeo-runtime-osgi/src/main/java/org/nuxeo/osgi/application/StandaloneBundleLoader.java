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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.osgi.BundleFile;
import org.nuxeo.osgi.BundleImpl;
import org.nuxeo.osgi.OSGiAdapter;
import org.osgi.framework.BundleException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class StandaloneBundleLoader extends ApplicationLoader {

    private static final Log log = LogFactory.getLog(StandaloneBundleLoader.class);

    protected SharedClassLoader loader;

    public StandaloneBundleLoader(OSGiAdapter osgi) {
        this(osgi, StandaloneBundleLoader.class.getClassLoader());
    }

    public StandaloneBundleLoader(OSGiAdapter osgi, ClassLoader parentLoader) {
        super(osgi);
        loader = new SharedClassLoaderImpl(parentLoader);
    }

    public StandaloneBundleLoader(OSGiAdapter osgi, SharedClassLoader scl) {
        super(osgi);
        loader = scl;
    }

    public void setSharedClassLoader(SharedClassLoader loader) {
        this.loader = loader;
    }

    public SharedClassLoader getSharedClassLoader() {
        return loader;
    }

    @Override
    public void installBundle(BundleFile bundleFile) throws BundleException {
        osgi.install(new BundleImpl(osgi, bundleFile, loader.getLoader()));
    }

    @Override
    public void loadBundle(BundleFile bundleFile) {
        loader.addURL(bundleFile.getURL());
    }

    @Override
    public void loadJAR(BundleFile bundleFile) {
        loader.addURL(bundleFile.getURL());
    }

    public static void main(String[] args) throws Exception {
        File home = new File("/tmp/test_osgi_loader");
        OSGiAdapter osgi = new OSGiAdapter(home);
        System.out.println("Starting ...");
        StandaloneBundleLoader loader = new StandaloneBundleLoader(osgi);
        Thread.currentThread().setContextClassLoader(loader.loader.getLoader());
        double s = System.currentTimeMillis();
        try {
            loader.setExtractNestedJARs(true);
            loader.setScanForNestedJARs(true);
            List<BundleFile> bundles = new ArrayList<>();
            List<BundleFile> jars = new ArrayList<>();
            loader.load(new File("/opt/jboss/server/default/deploy/nuxeo.ear"), bundles, jars);
            loader.installAll(bundles);
            System.out.println(">>>> Loading done!!!!");
        } finally {
            System.out.println("Shutting down");
            osgi.shutdown();
        }
        double e = System.currentTimeMillis();
        System.out.println("Total time: " + ((e - s) / 1000) + " sec.");
        System.exit(0);
    }

}
