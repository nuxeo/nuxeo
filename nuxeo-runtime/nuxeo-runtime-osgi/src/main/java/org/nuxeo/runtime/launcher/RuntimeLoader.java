/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.runtime.launcher;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.nuxeo.osgi.OSGiAdapter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RuntimeLoader {

    // whether or not to extract embedded jars
    public static final String EXTRACT_NESTED_JARS = "org.nuxeo.runtime.loader.extractNestedJARs";
    // whether to scan for nested jars or to use Bundle-ClassPath when extracting nested jars
    public static final String SCAN_FOR_NESTED_JARS = "org.nuxeo.runtime.loader.scanForNestedJARs";

    // a directory containing standard jars
    public static final String LIB_DIR = "org.nuxeo.runtime.loader.lib";
    // where runtime application data is stored (persistence, temp data etc)
    public static final String DATA_DIR = "org.nuxeo.runtime.loader.data";
    // directory containing nuxeo runtime osgi bundles
    public static final String BUNDLES_DIR = "org.nuxeo.runtime.loader.bundles";
    // directory containing config data
    public static final String CONFIG_DIR = "org.nuxeo.runtime.loader.config";

    private final Properties properties = new Properties();

    private StandaloneBundleLoader bundleLoader;


    public RuntimeLoader() {
    }

    public RuntimeLoader(File configFile) {
        setProperties(configFile);
    }

    public RuntimeLoader(Properties properties) {
        setProperties(properties);
    }

    /**
     * @return the properties.
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * @param properties the properties to set.
     */
    public void setProperties(Properties properties) {
        if (properties == null) {
            this.properties.clear();
        } else {
            this.properties.putAll(properties);
        }
    }

    public void setProperties(File cfg) {
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(cfg));
            properties.load(in);
        } catch (Exception e) {
            e.printStackTrace(); //TODO
        }
    }

    /**
     * @return the bundleLoader.
     */
    public StandaloneBundleLoader getBundleLoader() {
        return bundleLoader;
    }

    public File getDataDir() {
        return new File(properties.getProperty(DATA_DIR, "data"));
    }

    public File getConfigDir() {
        return new File(properties.getProperty(CONFIG_DIR, "config"));
    }

    public File getLibDir() {
        return new File(properties.getProperty(LIB_DIR, "lib"));
    }

    public File getBundlesDir() {
        return new File(properties.getProperty(BUNDLES_DIR, "bundles"));
    }

    public ClassLoader loadRuntime() throws Exception {
        return loadRuntime(RuntimeLoader.class.getClassLoader());
    }

    public ClassLoader loadRuntime(ClassLoader parentLoader) throws Exception {

        boolean extractNestedJARs = Boolean.parseBoolean(
                properties.getProperty(EXTRACT_NESTED_JARS, "true"));
        boolean scanNestedJARs = Boolean.parseBoolean(
                properties.getProperty(SCAN_FOR_NESTED_JARS, "true"));

        File data = getDataDir();
        File config = getConfigDir();
        File libsDir = getLibDir();
        File bundlesDir = getBundlesDir();

        OSGiAdapter osgi = new OSGiAdapter(data);
        // this is a runtime option that should points to a config directory
        osgi.setProperty("CONFIG_DIR", config.getAbsolutePath());

        // ------- TODO temp fix : deploy now config dir

        // --------------------------------------------------------

        // create the standalone loader
        bundleLoader = new StandaloneBundleLoader(osgi, parentLoader);
        Thread.currentThread().setContextClassLoader(bundleLoader.getSharedClassLoader());
        bundleLoader.setScanForNestedJARs(scanNestedJARs);
        bundleLoader.setExtractNestedJARs(extractNestedJARs);

        List<BundleFile> bundles = new ArrayList<BundleFile>();
        List<BundleFile> jars = new ArrayList<BundleFile>();
        // first load libs
        if (libsDir.isDirectory()) {
            bundleLoader.load(libsDir, bundles, jars);
        }
        // second load bundles
        if (bundlesDir.isDirectory()) {
            bundleLoader.load(bundlesDir, bundles, jars);
        }
        // install found bundles
        bundleLoader.installAll(bundles);
        // that's all
        return bundleLoader.getSharedClassLoader();
    }

}
