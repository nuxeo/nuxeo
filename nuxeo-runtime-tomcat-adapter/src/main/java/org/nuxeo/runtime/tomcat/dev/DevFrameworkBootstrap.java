/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.runtime.tomcat.dev;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.nuxeo.osgi.application.FrameworkBootstrap;
import org.nuxeo.osgi.application.MutableClassLoader;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class DevFrameworkBootstrap extends FrameworkBootstrap {

    protected File devBundlesFile;

    protected DevBundle[] devBundles;

    protected Timer bundlesCheck;

    protected long lastModified = 0;

    public DevFrameworkBootstrap(ClassLoader cl, File home) throws IOException {
        super(cl, home);
    }

    public DevFrameworkBootstrap(MutableClassLoader cl, File home)
            throws IOException {
        super(cl, home);
        devBundlesFile = new File(home, "dev.bundles");
    }

    @Override
    public void start() throws Exception {
        // check if we have dev. bundles or libs to deploy and add them to the
        // classpath
        preloadDevBundles();
        // start the framework
        super.start();
        // start dev bundles if any
        postloadDevBundles();
        // start reload timer
        bundlesCheck = new Timer("Dev Bundles Loader");
        bundlesCheck.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    checkDevBundles();
                } catch (Throwable t) {
                    log.error("Error running dev mode timer", t);
                }
            }
        }, 2000, 2000);
    }

    @Override
    public void stop() throws Exception {
        if (bundlesCheck != null) {
            bundlesCheck.cancel();
            bundlesCheck = null;
        }
        super.stop();
    }

    /**
     * Load the development bundles and libs if any in the classpath before
     * starting the framework.
     */
    protected void preloadDevBundles() throws IOException {
        if (devBundlesFile.isFile()) {
            lastModified = devBundlesFile.lastModified();
            devBundles = getDevBundles();
            if (devBundles.length == 0) {
                devBundles = null;
                return;
            }
            // clear dev classloader
            NuxeoDevWebappClassLoader devLoader = (NuxeoDevWebappClassLoader) loader;
            devLoader.clear();
            URL[] urls = new URL[devBundles.length];
            for (int i = 0; i < devBundles.length; i++) {
                urls[i] = devBundles[i].url();
            }
            devLoader.createLocalClassLoader(urls);
        }
    }

    protected void postloadDevBundles() throws Exception {
        if (devBundles != null) {
            for (DevBundle bundle : devBundles) {
                if (!bundle.isLibrary()) {
                    bundle.name = installBundle(bundle.file);
                }
            }
        }
    }

    protected void checkDevBundles() {
        long tm = devBundlesFile.lastModified();
        if (lastModified >= tm) {
            return;
        }
        lastModified = tm;
        try {
            reloadDevBundles(getDevBundles());
        } catch (Exception e) {
            log.error("Faied to deploy dev bundles", e);
        }
    }

    protected DevBundle[] getDevBundles() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(devBundlesFile)));
        try {
            ArrayList<DevBundle> urls = new ArrayList<DevBundle>();
            String line = reader.readLine();
            while (line != null) {
                line = line.trim();
                if (line.length() > 0 && !line.startsWith("#")) {
                    if (line.startsWith("!")) {
                        // a library
                        urls.add(new DevBundle(new File(line.substring(1)),
                                true));
                    } else { // a bundle
                        urls.add(new DevBundle(new File(line)));
                    }
                }
                line = reader.readLine();
            }
            return urls.toArray(new DevBundle[urls.size()]);
        } finally {
            reader.close();
        }
    }

    protected synchronized void reloadDevBundles(DevBundle[] bundles)
            throws Exception {
        if (devBundles != null) {
            // undeploy
            for (DevBundle bundle : devBundles) {
                if (!bundle.isLibrary()) {
                    if (bundle.name != null) {
                        uninstallBundle(bundle.name);
                    }
                }
            }
        }
        devBundles = bundles;
        // clear dev classloader
        NuxeoDevWebappClassLoader devLoader = (NuxeoDevWebappClassLoader) loader;
        devLoader.clear();
        URL[] urls = new URL[bundles.length];
        for (int i = 0; i < bundles.length; i++) {
            urls[i] = bundles[i].url();
        }
        devLoader.createLocalClassLoader(urls);
        // deploy
        for (DevBundle bundle : devBundles) {
            if (!bundle.isLibrary()) {
                bundle.name = installBundle(bundle.file);
            }
        }
    }

    static class DevBundle {
        String name; // the bundle symbolic name if not a lib

        boolean isLibrary;

        File file;

        public DevBundle(File file) {
            this(file, false);
        }

        public DevBundle(File file, boolean isLibrary) {
            this.file = file;
            this.isLibrary = isLibrary;
        }

        public URL url() throws IOException {
            return file.toURI().toURL();
        }

        public boolean isLibrary() {
            return isLibrary;
        }
    }

}
