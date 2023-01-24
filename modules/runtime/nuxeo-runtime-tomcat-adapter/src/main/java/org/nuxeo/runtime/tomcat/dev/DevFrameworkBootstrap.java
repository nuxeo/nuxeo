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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.osgi.application.FrameworkBootstrap;
import org.nuxeo.osgi.application.MutableClassLoader;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DevFrameworkBootstrap extends FrameworkBootstrap implements DevBundlesManager {

    private static final Logger log = LogManager.getLogger(DevFrameworkBootstrap.class);

    public static final String DEV_BUNDLES_NAME = "org.nuxeo:type=sdk,name=dev-bundles";

    public static final String WEB_RESOURCES_NAME = "org.nuxeo:type=sdk,name=web-resources";

    protected static final String DEV_BUNDLES_CP = "dev-bundles/*";

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
        devBundles = new DevBundle[0];
    }

    @Override
    public void start(MutableClassLoader cl) throws ReflectiveOperationException, IOException, JMException {
        // start the framework
        super.start(cl);
        ClassLoader loader = (ClassLoader) this.loader;
        reloadServiceInvoker = new ReloadServiceInvoker(loader);
        writeComponentIndex();
        String installReloadTimerOption = (String) env.get(INSTALL_RELOAD_TIMER);
        if (installReloadTimerOption != null && Boolean.parseBoolean(installReloadTimerOption)) {
            toggleTimer();
        }
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        server.registerMBean(this, new ObjectName(DEV_BUNDLES_NAME));
        server.registerMBean(cl, new ObjectName(WEB_RESOURCES_NAME));
    }

    @Override
    protected void initializeEnvironment() throws IOException {
        super.initializeEnvironment();
        // add the dev-bundles to classpath
        env.computeIfPresent(BUNDLES, (k, v) -> v + ":" + DEV_BUNDLES_CP);
    }

    @Override
    public void toggleTimer() {
        // start reload timer
        if (isTimerRunning()) {
            bundlesCheck.cancel();
            bundlesCheck = null;
        } else {
            bundlesCheck = new Timer("Dev Bundles Loader");
            bundlesCheck.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    try {
                        loadDevBundles();
                    } catch (RuntimeException e) {
                        log.error("Failed to reload dev bundles", e);
                    }
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
            throw new RuntimeException("Failed to reload dev bundles", e);
        }
    }

    @Override
    public void resetDevBundles(String path) {
        try {
            devBundlesFile = new File(path);
            lastModified = 0;
            loadDevBundles();
        } catch (RuntimeException e) {
            log.error("Unable to reset dev bundles", e);
        }
    }

    @Override
    public DevBundle[] getDevBundles() {
        return devBundles;
    }

    protected synchronized void reloadDevBundles(DevBundle[] bundles) throws ReflectiveOperationException, IOException {
        long begin = System.currentTimeMillis();
        // symbolicName of bundlesToDeploy will be filled by hotReloadBundles before hot reload
        // -> this allows server to be hot reloaded again in case of errors
        // if everything goes fine, bundlesToDeploy will be replaced by result of hot reload containing symbolic
        // name and the new bundle path
        DevBundle[] bundlesToDeploy = bundles;
        try {
            bundlesToDeploy = reloadServiceInvoker.hotReloadBundles(devBundles, bundlesToDeploy);

            // write the new dev bundles location to the file
            writeDevBundles(bundlesToDeploy);
        } finally {
            devBundles = bundlesToDeploy;
        }

        log.info("Hot reload has been run in {}ms", System.currentTimeMillis() - begin);
    }

    /**
     * Writes to the {@link #devBundlesFile} the input {@code devBundles} by replacing the former file.
     * <p>
     * This method will {@link #toggleTimer() toggle} the file update check timer if needed.
     *
     * @since 9.3
     */
    protected void writeDevBundles(DevBundle[] devBundles) throws IOException {
        boolean timerExists = isTimerRunning();
        if (timerExists) {
            // timer is running, we need to stop it before editing the file
            toggleTimer();
        }
        // for nuxeo-cli needs, we need to keep comments
        List<String> lines = Files.readAllLines(devBundlesFile.toPath());
        // newBufferedWriter without OpenOption will create/truncate if exist the target file
        try (BufferedWriter writer = Files.newBufferedWriter(devBundlesFile.toPath())) {
            Iterator<DevBundle> devBundlesIt = Arrays.asList(devBundles).iterator();
            for (String line : lines) {
                if (line.startsWith("#")) {
                    writer.write(line);
                } else if (devBundlesIt.hasNext()) {
                    writer.write(devBundlesIt.next().toString());
                } else {
                    // there's a sync problem between dev.bundles file and nuxeo runtime
                    // comment this bundle to not break further attempt
                    writer.write("# ");
                    writer.write(line);
                }
                writer.write(System.lineSeparator());
            }
        } finally {
            if (timerExists) {
                // restore the time status
                lastModified = System.currentTimeMillis();
                toggleTimer();
            }
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
}
