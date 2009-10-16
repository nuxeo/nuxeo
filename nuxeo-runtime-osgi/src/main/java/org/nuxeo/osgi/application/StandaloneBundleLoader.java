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

package org.nuxeo.osgi.application;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.osgi.BundleFile;
import org.nuxeo.osgi.BundleImpl;
import org.nuxeo.osgi.OSGiAdapter;
import org.osgi.framework.BundleException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class StandaloneBundleLoader extends ApplicationLoader {

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

    public static void main(String[] args) {
        File home = new File("/tmp/test_osgi_loader");
        OSGiAdapter osgi = new OSGiAdapter(home);
        System.out.println("Starting ...");
        StandaloneBundleLoader loader = new StandaloneBundleLoader(osgi);
        Thread.currentThread().setContextClassLoader(loader.loader.getLoader());
        double s = System.currentTimeMillis();
        try {
            loader.setExtractNestedJARs(true);
            loader.setScanForNestedJARs(true);
            List<BundleFile> bundles = new ArrayList<BundleFile>();
            List<BundleFile> jars = new ArrayList<BundleFile>();
            loader.load(new File("/opt/jboss/server/default/deploy/nuxeo.ear"), bundles, jars);
            loader.installAll(bundles);
//            jars.clear(); bundles.clear();
//            loader.load(new File("/opt/jboss/server/default/deploy/nuxeo.ear/system"), bundles, jars);
//            loader.installAll(bundles);
//            jars.clear(); bundles.clear();
//            loader.load(new File("/opt/jboss/server/default/deploy/nuxeo.ear/core"), bundles, jars);
//            loader.installAll(bundles);
            System.out.println(">>>> Loading done!!!!");
        } catch (Throwable e) {
            e.printStackTrace();
            for (URL url : loader.loader.getURLs()) {
                System.err.println("url> " + url);
            }
        } finally {
            System.out.println("Shutting down");
            try {
                osgi.shutdown();
            } catch (Exception e) {
            }
        }
        double e = System.currentTimeMillis();
        System.out.println("Total time: " + ((e - s) / 1000) + " sec.");
        System.exit(0);
    }

}
