/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.runtime.tomcat.dev;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.osgi.application.FrameworkBootstrap;
import org.nuxeo.osgi.application.MutableClassLoader;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DevFrameworkBootstrap extends FrameworkBootstrap implements DevBundlesManager {

    public static final String DEV_BUNDLES_NAME = "org.nuxeo:type=sdk,name=dev-bundles";

    public static final String WEB_RESOURCES_NAME = "org.nuxeo:type=sdk,name=web-resources";

    protected final Log log = LogFactory.getLog(DevFrameworkBootstrap.class);

    protected DevBundle[] devBundles;

    protected Timer bundlesCheck;

    protected long lastModified = 0;

    protected ReloadServiceInvoker reloadServiceInvoker;

    protected File devBundlesFile;

    protected final File seamdev;

    protected final File webclasses;

    public DevFrameworkBootstrap(MutableClassLoader cl, File home) throws IOException {
        super(cl, home);
        devBundlesFile = new File(home, "dev.bundles");
        seamdev = new File(home, "nuxeo.war/WEB-INF/dev");
        webclasses = new File(home, "nuxeo.war/WEB-INF/classes");
    }

    @Override
    public void start(MutableClassLoader cl) throws ReflectiveOperationException, IOException, JMException {
        // check if we have dev. bundles or libs to deploy and add them to the
        // classpath
        preloadDevBundles();
        // start the framework
        super.start(cl);
        reloadServiceInvoker = new ReloadServiceInvoker((ClassLoader) loader);
        writeComponentIndex();
        postloadDevBundles(); // start dev bundles if any
        String installReloadTimerOption = (String) env.get(INSTALL_RELOAD_TIMER);
        if (installReloadTimerOption != null && Boolean.parseBoolean(installReloadTimerOption)) {
            toggleTimer();
        }
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        server.registerMBean(this, new ObjectName(DEV_BUNDLES_NAME));
        server.registerMBean(cl, new ObjectName(WEB_RESOURCES_NAME));
    }

    @Override
    public void toggleTimer() {
        // start reload timer
        if (bundlesCheck != null) {
            bundlesCheck.cancel();
            bundlesCheck = null;
        } else {
            bundlesCheck = new Timer("Dev Bundles Loader");
            bundlesCheck.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    loadDevBundles();
                }
            }, 2000, 2000);
        }
    }

    @Override
    public boolean isTimerRunning() {
        return bundlesCheck != null;
    }

    @Override
    public void stop(MutableClassLoader cl) throws ReflectiveOperationException, JMException {
        if (bundlesCheck != null) {
            bundlesCheck.cancel();
            bundlesCheck = null;
        }
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            server.unregisterMBean(new ObjectName(DEV_BUNDLES_NAME));
            server.unregisterMBean(new ObjectName(WEB_RESOURCES_NAME));
        } finally {
            super.stop(cl);
        }
    }

    @Override
    public String getDevBundlesLocation() {
        return devBundlesFile.getAbsolutePath();
    }

    /**
     * Load the development bundles and libs if any in the classpath before starting the framework.
     */
    protected void preloadDevBundles() throws IOException {
        if (!devBundlesFile.isFile()) {
            return;
        }
        lastModified = devBundlesFile.lastModified();
        devBundles = DevBundle.parseDevBundleLines(new FileInputStream(devBundlesFile));
        if (devBundles.length == 0) {
            devBundles = null;
            return;
        }
        installNewClassLoader(devBundles);
    }

    protected void postloadDevBundles() throws ReflectiveOperationException {
        if (devBundles != null) {
            reloadServiceInvoker.hotDeployBundles(devBundles);
        }
    }

    @Override
    public void loadDevBundles() {
        long tm = devBundlesFile.lastModified();
        if (lastModified >= tm) {
            return;
        }
        lastModified = tm;
        try {
            reloadDevBundles(DevBundle.parseDevBundleLines(new FileInputStream(devBundlesFile)));
        } catch (ReflectiveOperationException | IOException e) {
            log.error("Failed to deploy dev bundles", e);
        }
    }

    @Override
    public void resetDevBundles(String path) {
        devBundlesFile = new File(path);
        lastModified = 0;
        loadDevBundles();
    }

    @Override
    public DevBundle[] getDevBundles() {
        return devBundles;
    }

    protected synchronized void reloadDevBundles(DevBundle[] bundles) throws ReflectiveOperationException {

        if (devBundles != null) { // clear last context
            try {
                reloadServiceInvoker.hotUndeployBundles(devBundles);
                clearClassLoader();
            } finally {
                devBundles = null;
            }
        }

        if (bundles != null) { // create new context
            try {
                installNewClassLoader(bundles);
                reloadServiceInvoker.hotDeployBundles(bundles);
            } finally {
                devBundles = bundles;
            }
        }
    }

    protected void clearClassLoader() {
        NuxeoDevWebappClassLoader devLoader = (NuxeoDevWebappClassLoader) loader;
        devLoader.clear();
        System.gc();
    }

    protected void installNewClassLoader(DevBundle[] bundles) {
        List<URL> jarUrls = new ArrayList<>();
        List<File> seamDirs = new ArrayList<>();
        List<File> resourceBundleFragments = new ArrayList<>();
        // filter dev bundles types
        for (DevBundle bundle : bundles) {
            if (bundle.devBundleType.isJar) {
                try {
                    jarUrls.add(bundle.url());
                } catch (IOException e) {
                    log.error("Cannot install " + bundle);
                }
            } else if (bundle.devBundleType == DevBundleType.Seam) {
                seamDirs.add(bundle.file());
            } else if (bundle.devBundleType == DevBundleType.ResourceBundleFragment) {
                resourceBundleFragments.add(bundle.file());
            }
        }

        // install class loader
        NuxeoDevWebappClassLoader devLoader = (NuxeoDevWebappClassLoader) loader;
        devLoader.createLocalClassLoader(jarUrls.toArray(new URL[jarUrls.size()]));

        // install seam classes in hot sync folder
        try {
            installSeamClasses(seamDirs.toArray(new File[seamDirs.size()]));
        } catch (IOException e) {
            log.error("Cannot install seam classes in hotsync folder", e);
        }

        // install l10n resources
        try {
            installResourceBundleFragments(resourceBundleFragments);
        } catch (IOException e) {
            log.error("Cannot install l10n resources", e);
        }
    }

    public void writeComponentIndex() {
        File file = new File(home.getParentFile(), "sdk");
        file.mkdirs();
        file = new File(file, "components.index");
        try {
            Method m = getClassLoader().loadClass("org.nuxeo.runtime.model.impl.ComponentRegistrySerializer")
                    .getMethod("writeIndex", File.class);
            m.invoke(null, file);
        } catch (ReflectiveOperationException t) {
            // ignore
        }
    }

    public void installSeamClasses(File[] dirs) throws IOException {
        if (seamdev.exists()) {
            IOUtils.deleteTree(seamdev);
        }
        seamdev.mkdirs();
        for (File dir : dirs) {
            IOUtils.copyTree(dir, seamdev);
        }
    }

    public void installResourceBundleFragments(List<File> files) throws IOException {
        Map<String, List<File>> fragments = new HashMap<>();

        for (File file : files) {
            String name = resourceBundleName(file);
            if (!fragments.containsKey(name)) {
                fragments.put(name, new ArrayList<>());
            }
            fragments.get(name).add(file);
        }
        for (String name : fragments.keySet()) {
            IOUtils.appendResourceBundleFragments(name, fragments.get(name), webclasses);
        }
    }

    protected static String resourceBundleName(File file) {
        String name = file.getName();
        return name.substring(name.lastIndexOf('-') + 1);
    }

}
