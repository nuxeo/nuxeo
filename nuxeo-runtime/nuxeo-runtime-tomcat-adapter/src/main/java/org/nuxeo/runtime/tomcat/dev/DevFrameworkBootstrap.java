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
import java.net.URI;
import java.net.URL;
import java.nio.file.CopyOption;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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

    public static final String USE_COMPAT_HOT_RELOAD = "nuxeo.hotreload.compat.mechanism";

    protected static final String DEV_BUNDLES_CP = "dev-bundles/*";

    protected final Log log = LogFactory.getLog(DevFrameworkBootstrap.class);

    protected DevBundle[] devBundles;

    protected Timer bundlesCheck;

    protected long lastModified = 0;

    protected ReloadServiceInvoker reloadServiceInvoker;

    protected File devBundlesFile;

    protected final File seamdev;

    protected final File webclasses;

    protected boolean compatHotReload;

    public DevFrameworkBootstrap(MutableClassLoader cl, File home) throws IOException {
        super(cl, home);
        devBundlesFile = new File(home, "dev.bundles");
        seamdev = new File(home, "nuxeo.war/WEB-INF/dev");
        webclasses = new File(home, "nuxeo.war/WEB-INF/classes");
        devBundles = new DevBundle[0];
    }

    @Override
    public void start(MutableClassLoader cl) throws ReflectiveOperationException, IOException, JMException {
        // check if we have dev. bundles or libs to deploy and add them to the
        // classpath
        preloadDevBundles();
        // start the framework
        super.start(cl);
        ClassLoader loader = (ClassLoader) this.loader;
        reloadServiceInvoker = new ReloadServiceInvoker(loader);
        compatHotReload = new FrameworkInvoker(loader).isBooleanPropertyTrue(USE_COMPAT_HOT_RELOAD);
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

    /**
     * Load the development bundles and libs if any in the classpath before starting the framework.
     *
     * @deprecated since 9.3, we now have a new mechanism to hot reload bundles from {@link #devBundlesFile}. The new
     *             mechanism copies bundles to nxserver/bundles, so it's now useless to preload dev bundles as they're
     *             deployed as a regular bundle.
     */
    @Deprecated
    protected void preloadDevBundles() throws IOException {
        if (!compatHotReload) {
            return;
        }
        if (!devBundlesFile.isFile()) {
            return;
        }
        lastModified = devBundlesFile.lastModified();
        devBundles = DevBundle.parseDevBundleLines(new FileInputStream(devBundlesFile));
        if (devBundles.length > 0) {
            installNewClassLoader(devBundles);
        }
    }

    /**
     * @deprecated since 9.3, we now have a new mechanism to hot reload bundles from {@link #devBundlesFile}. The new
     *             mechanism copies bundles to nxserver/bundles, so it's now useless to postload dev bundles as they're
     *             deployed as a regular bundle.
     */
    @Deprecated
    protected void postloadDevBundles() throws ReflectiveOperationException {
        if (!compatHotReload) {
            return;
        }
        if (devBundles.length > 0) {
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

        if (compatHotReload) {
            if (devBundles.length > 0) { // clear last context
                try {
                    reloadServiceInvoker.hotUndeployBundles(devBundles);
                    clearClassLoader();
                } finally {
                    devBundles = new DevBundle[0];
                }
            }

            if (bundles.length > 0) { // create new context
                try {
                    installNewClassLoader(bundles);
                    reloadServiceInvoker.hotDeployBundles(bundles);
                } finally {
                    devBundles = bundles;
                }
            }
        } else {
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
        }
        if (log.isInfoEnabled()) {
            log.info(String.format("Hot reload has been run in %s ms", System.currentTimeMillis() - begin));
        }
    }

    /**
     * Writes to the {@link #devBundlesFile} the input {@code devBundles} by replacing the former file.
     * <p />
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

    /**
     * Zips recursively the content of {@code source} to the {@code target} zip file.
     *
     * @since 9.3
     */
    protected Path zipDirectory(Path source, Path target, CopyOption... options) throws IOException {
        if (!source.toFile().isDirectory()) {
            throw new IllegalArgumentException("Source argument must be a directory to zip");
        }
        // locate file system by using the syntax defined in java.net.JarURLConnection
        URI uri = URI.create("jar:file:" + target.toString());

        try (FileSystem zipfs = FileSystems.newFileSystem(uri, Collections.singletonMap("create", "true"))) {
            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (source.equals(dir)) {
                        // don't process root element
                        return FileVisitResult.CONTINUE;
                    }
                    return visitFile(dir, attrs);
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // retrieve the destination path in zip
                    Path relativePath = source.relativize(file);
                    Path pathInZipFile = zipfs.getPath(relativePath.toString());
                    // copy a file into the zip file
                    Files.copy(file, pathInZipFile, options);
                    return FileVisitResult.CONTINUE;
                }

            });
        }
        return target;
    }

    /**
     * @deprecated since 9.3 not needed anymore, here for backward compatibility, see {@link #compatHotReload}
     */
    @Deprecated
    protected void clearClassLoader() {
        NuxeoDevWebappClassLoader devLoader = (NuxeoDevWebappClassLoader) loader;
        devLoader.clear();
        System.gc();
    }

    /**
     * @deprecated since 9.3 not needed anymore, here for backward compatibility, see {@link #compatHotReload}
     */
    @Deprecated
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

    /**
     * @deprecated since 9.3 not needed anymore, here for backward compatibility, see {@link #compatHotReload}
     */
    @Deprecated
    public void installSeamClasses(File[] dirs) throws IOException {
        if (seamdev.exists()) {
            IOUtils.deleteTree(seamdev);
        }
        seamdev.mkdirs();
        for (File dir : dirs) {
            IOUtils.copyTree(dir, seamdev);
        }
    }

    /**
     * @deprecated since 9.3 not needed anymore, here for backward compatibility, see {@link #compatHotReload}
     */
    @Deprecated
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

    /**
     * @deprecated since 9.3 not needed anymore, here for backward compatibility, see {@link #compatHotReload}
     */
    @Deprecated
    protected static String resourceBundleName(File file) {
        String name = file.getName();
        return name.substring(name.lastIndexOf('-') + 1);
    }

}
