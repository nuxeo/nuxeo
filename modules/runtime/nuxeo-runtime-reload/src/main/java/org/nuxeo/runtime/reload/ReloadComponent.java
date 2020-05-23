/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.reload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.JarUtils;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.osgi.application.DevMutableClassLoader;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.deployment.preprocessor.DeploymentPreprocessor;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventService;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.runtime.util.Watch;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ReloadComponent extends DefaultComponent implements ReloadService {

    /**
     * The reload strategy to adopt for hot reload. Default value is {@link #RELOAD_STRATEGY_VALUE_DEFAULT}.
     *
     * @since 9.3
     */
    public static final String RELOAD_STRATEGY_PARAMETER = "org.nuxeo.runtime.reload_strategy";

    public static final String RELOAD_STRATEGY_VALUE_UNSTASH = "unstash";

    public static final String RELOAD_STRATEGY_VALUE_STANDBY = "standby";

    public static final String RELOAD_STRATEGY_VALUE_RESTART = "restart";

    public static final String RELOAD_STRATEGY_VALUE_DEFAULT = RELOAD_STRATEGY_VALUE_STANDBY;

    private static final Logger log = LogManager.getLogger(ReloadComponent.class);

    protected static Bundle bundle;

    protected Long lastFlushed;

    public static BundleContext getBundleContext() {
        return bundle.getBundleContext();
    }

    public static Bundle getBundle() {
        return bundle;
    }

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        bundle = context.getRuntimeContext().getBundle();
    }

    @Override
    public void deactivate(ComponentContext context) {
        super.deactivate(context);
        bundle = null;
    }

    /**
     * @deprecated since 9.3, this method is only used in deployBundles and undeployBundles which are deprecated. Keep
     *             it for backward compatibility.
     */
    @Deprecated(since = "9.3")
    protected void refreshComponents() {
        String reloadStrategy = Framework.getProperty(RELOAD_STRATEGY_PARAMETER, RELOAD_STRATEGY_VALUE_DEFAULT);
        log.info("Refresh components. Strategy={}", reloadStrategy);
        // reload components / contributions
        ComponentManager mgr = Framework.getRuntime().getComponentManager();
        switch (reloadStrategy) {
        case RELOAD_STRATEGY_VALUE_UNSTASH:
            // compat mode
            mgr.unstash();
            break;
        case RELOAD_STRATEGY_VALUE_STANDBY:
            // standby / resume
            mgr.standby();
            mgr.unstash();
            mgr.resume();
            break;
        case RELOAD_STRATEGY_VALUE_RESTART:
        default:
            // restart mode
            mgr.refresh(false);
            break;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void reload() {
        log.debug("Starting reload");

        try {
            reloadProperties();
        } catch (IOException e) {
            throw new RuntimeServiceException(e);
        }

        triggerReloadWithNewTransaction(RELOAD_EVENT_ID);
    }

    @Override
    public void reloadProperties() throws IOException {
        log.info("Before reload runtime properties");
        Framework.getRuntime().reloadProperties();
        log.info("After reload runtime properties");
    }

    @Override
    public void reloadSeamComponents() {
        log.info("Reload Seam components");
        Framework.getService(EventService.class).sendEvent(new Event(RELOAD_TOPIC, RELOAD_SEAM_EVENT_ID, this, null));
    }

    @Override
    public void flush() {
        log.info("Before flush caches");
        Framework.getService(EventService.class).sendEvent(new Event(RELOAD_TOPIC, FLUSH_EVENT_ID, this, null));
        flushJaasCache();
        setFlushedNow();
        log.info("After flush caches");
    }

    @Override
    public void flushJaasCache() {
        log.info("Before flush the JAAS cache");
        Framework.getService(EventService.class).sendEvent(new Event("usermanager", "user_changed", this, "Deployer"));
        setFlushedNow();
        log.info("After flush the JAAS cache");
    }

    @Override
    public void flushSeamComponents() {
        log.info("Flush Seam components");
        Framework.getService(EventService.class).sendEvent(new Event(RELOAD_TOPIC, FLUSH_SEAM_EVENT_ID, this, null));
        setFlushedNow();
    }

    /**
     * @deprecated since 9.3 use {@link #reloadBundles(ReloadContext)} instead.
     */
    @Override
    @Deprecated(since = "9.3")
    public void deployBundles(List<File> files, boolean reloadResources) throws BundleException {
        long begin = System.currentTimeMillis();
        List<String> missingNames = files.stream()
                                         .filter(file -> getOSGIBundleName(file) == null)
                                         .map(File::getAbsolutePath)
                                         .collect(Collectors.toList());
        if (!missingNames.isEmpty()) {
            missingNames.forEach(name -> log.error("No Bundle-SymbolicName found in MANIFEST for jar at '{}'", name));
            // TODO investigate why we need to exit here, getBundleContext().installBundle(path) will throw an exception
            // unless, maybe tests ?
            return;
        }

        log.info(() -> {
            StringBuilder builder = new StringBuilder("Before deploy bundles\n");
            Framework.getRuntime().getStatusMessage(builder);
            return builder.toString();
        });

        // Reload resources
        if (reloadResources) {
            List<URL> urls = files.stream().map(this::toURL).collect(Collectors.toList());
            Framework.reloadResourceLoader(urls, null);
        }

        // Deploy bundles
        BundleException exc = TransactionHelper.runWithoutTransaction(() -> {
            try {
                _deployBundles(files);
                refreshComponents();
                return null;
            } catch (BundleException e) {
                return e;
            }
        });
        if (exc != null) {
            throw exc;
        }

        log.info(() -> {
            StringBuilder builder = new StringBuilder("After deploy bundles.\n");
            Framework.getRuntime().getStatusMessage(builder);
            return builder.toString();
        });
        log.info("Hot deploy was done in {} ms.", System.currentTimeMillis() - begin);
    }

    /**
     * @deprecated since 9.3 use {@link #reloadBundles(ReloadContext)} instead.
     */
    @Override
    @Deprecated(since = "9.3")
    public void undeployBundles(List<String> bundleNames, boolean reloadResources) throws BundleException {
        long begin = System.currentTimeMillis();
        log.info(() -> {
            StringBuilder builder = new StringBuilder("Before undeploy bundles\n");
            Framework.getRuntime().getStatusMessage(builder);
            return builder.toString();
        });

        // Undeploy bundles
        ReloadResult result = new ReloadResult();
        BundleException exc = TransactionHelper.runWithoutTransaction(() -> {
            try {
                result.merge(_undeployBundles(bundleNames));
                refreshComponents();
                return null;
            } catch (BundleException e) {
                return e;
            }
        });
        if (exc != null) {
            throw exc;
        }

        // Reload resources
        if (reloadResources) {
            List<URL> undeployedBundleURLs = result.undeployedBundles.stream().map(this::toURL).collect(
                    Collectors.toList());
            Framework.reloadResourceLoader(null, undeployedBundleURLs);
        }

        log.info(() -> {
            StringBuilder builder = new StringBuilder("After undeploy bundles.\n");
            Framework.getRuntime().getStatusMessage(builder);
            return builder.toString();
        });
        log.info("Hot undeploy was done in {} ms.", System.currentTimeMillis() - begin);
    }

    @Override
    public synchronized ReloadResult reloadBundles(ReloadContext context) throws BundleException {
        ReloadResult result = new ReloadResult();
        List<String> bundlesNamesToUndeploy = context.bundlesNamesToUndeploy;

        Watch watch = new Watch(new LinkedHashMap<>()).start();
        log.info(() -> {
            StringBuilder builder = new StringBuilder("Before updating Nuxeo server\n");
            Framework.getRuntime().getStatusMessage(builder);
            return builder.toString();
        });
        // get class loader
        Optional<DevMutableClassLoader> classLoader = Optional.of(getClass().getClassLoader())
                                                              .filter(DevMutableClassLoader.class::isInstance)
                                                              .map(DevMutableClassLoader.class::cast);

        watch.start("flush");
        flush();
        watch.stop("flush");

        // Commit current transaction
        if (TransactionHelper.isTransactionMarkedRollback()) {
            throw new BundleException("Cannot reload bundles when transaction is marked rollback");
        }
        boolean activeTransaction = TransactionHelper.isTransactionActive();
        if (activeTransaction) {
            TransactionHelper.commitOrRollbackTransaction();
        }

        try {
            // Stop or Standby the component manager
            ComponentManager componentManager = Framework.getRuntime().getComponentManager();
            String reloadStrategy = Framework.getProperty(RELOAD_STRATEGY_PARAMETER, RELOAD_STRATEGY_VALUE_DEFAULT);
            log.info("Component reload strategy={}", reloadStrategy);

            watch.start("stop/standby");
            log.info("Before stop/standby component manager");
            if (RELOAD_STRATEGY_VALUE_RESTART.equals(reloadStrategy)) {
                componentManager.stop();
            } else {
                // standby strategy by default
                componentManager.standby();
            }
            log.info("After stop/standby component manager");
            watch.stop("stop/standby");

            // Undeploy bundles
            if (!bundlesNamesToUndeploy.isEmpty()) {
                watch.start("undeploy-bundles");
                log.info("Before undeploy bundles");
                logComponentManagerStatus();

                result.merge(_undeployBundles(bundlesNamesToUndeploy));
                clearJarFileFactoryCache(result);
                componentManager.unstash();

                // Clear the class loader
                classLoader.ifPresent(DevMutableClassLoader::clearPreviousClassLoader);
                // TODO shall we do a GC here ? see DevFrameworkBootstrap#clearClassLoader

                log.info("After undeploy bundles");
                logComponentManagerStatus();
                watch.stop("undeploy-bundles");
            }

            watch.start("delete-copy");
            // Delete old bundles
            log.info("Before delete-copy");
            List<URL> urlsToRemove = result.undeployedBundles.stream()
                                                             .map(Bundle::getLocation)
                                                             .map(File::new)
                                                             .peek(File::delete)
                                                             .map(this::toURL)
                                                             .collect(Collectors.toList());
            // Then copy new ones
            List<File> bundlesToDeploy = copyBundlesToDeploy(context);
            List<URL> urlsToAdd = bundlesToDeploy.stream().map(this::toURL).collect(Collectors.toList());
            log.info("After delete-copy");
            watch.stop("delete-copy");

            // Reload resources
            watch.start("reload-resources");
            Framework.reloadResourceLoader(urlsToAdd, urlsToRemove);
            watch.stop("reload-resources");

            // Deploy bundles
            if (!bundlesToDeploy.isEmpty()) {
                watch.start("deploy-bundles");
                log.info("Before deploy bundles");
                logComponentManagerStatus();

                // Fill the class loader
                classLoader.ifPresent(cl -> cl.addClassLoader(urlsToAdd.toArray(new URL[0])));

                result.merge(_deployBundles(bundlesToDeploy));
                componentManager.unstash();

                log.info("After deploy bundles");
                logComponentManagerStatus();
                watch.stop("deploy-bundles");
            }

            // Start or Resume the component manager
            watch.start("start/resume");
            log.info("Before start/resume component manager");
            if (RELOAD_STRATEGY_VALUE_RESTART.equals(reloadStrategy)) {
                componentManager.start();
            } else {
                // standby strategy by default
                componentManager.resume();
            }
            log.info("After start/resume component manager");
            watch.stop("start/resume");

            try {
                // run deployment preprocessor
                watch.start("deployment-preprocessor");
                runDeploymentPreprocessor();
                watch.stop("deployment-preprocessor");
            } catch (IOException e) {
                throw new BundleException("Unable to run deployment preprocessor", e);
            }

            try {
                // reload
                watch.start("reload-properties");
                reloadProperties();
                watch.stop("reload-properties");
            } catch (IOException e) {
                throw new BundleException("Unable to reload properties", e);
            }
        } finally {
            if (activeTransaction) {
                // Restart a transaction
                TransactionHelper.startTransaction();
            }
        }

        log.info(() -> {
            StringBuilder builder = new StringBuilder("After updating Nuxeo server\n");
            Framework.getRuntime().getStatusMessage(builder);
            return builder.toString();
        });

        watch.stop();
        log.info("Hot reload was done in {} ms, detailed steps:\n{}",
                () -> watch.getTotal().elapsed(TimeUnit.MILLISECONDS),
                () -> Stream.of(watch.getIntervals())
                            .map(i -> "- " + i.getName() + ": " + i.elapsed(TimeUnit.MILLISECONDS) + " ms")
                            .collect(Collectors.joining("\n")));
        return result;
    }

    protected List<File> copyBundlesToDeploy(ReloadContext context) throws BundleException {
        List<File> bundlesToDeploy = new ArrayList<>();
        Path homePath = Framework.getRuntime().getHome().toPath();
        Path destinationPath = homePath.resolve(context.bundlesDestination);
        try {
            Files.createDirectories(destinationPath);
            for (File bundle : context.bundlesToDeploy) {
                Path bundlePath = bundle.toPath();
                // check if the bundle is located under the desired destination
                // if not copy it to the desired destination
                if (!bundlePath.startsWith(destinationPath)) {
                    if (Files.isDirectory(bundlePath)) {
                        // If it's a directory, assume that it's an exploded jar
                        bundlePath = JarUtils.zipDirectory(bundlePath,
                                destinationPath.resolve("hotreload-bundle-" + System.currentTimeMillis() + ".jar"),
                                StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        bundlePath = destinationPath.resolve(bundle.getName());
                        // JDK nio Files will replace the existing file (if destination already exists) which is an
                        // an issue on Windows cause you can't replace a file used by the JVM
                        // so use commons-io instead because it will override the content by using a FileInputStream
                        // instead of replacing the file
                        FileUtils.copyFile(bundle, bundlePath.toFile(), false);
                    }
                }
                bundlesToDeploy.add(bundlePath.toFile());
            }
            return bundlesToDeploy;
        } catch (IOException e) {
            throw new BundleException("Unable to copy bundles to " + destinationPath, e);
        }
    }

    /*
     * TODO Change this method name when deployBundles will be removed.
     */
    protected ReloadResult _deployBundles(List<File> bundlesToDeploy) throws BundleException {
        ReloadResult result = new ReloadResult();
        BundleContext bundleContext = getBundleContext();
        for (File file : bundlesToDeploy) {
            String path = file.getAbsolutePath();
            log.info("Before deploy bundle for file at '{}'", path);
            Bundle bundle = bundleContext.installBundle(path);
            if (bundle == null) {
                // TODO check why this is necessary, our implementation always return sth
                throw new IllegalArgumentException("Could not find a valid bundle at path: " + path);
            }
            bundle.start();
            result.deployedBundles.add(bundle);
            log.info("Deploy done for bundle with name '{}'", bundle.getSymbolicName());
        }
        return result;
    }

    /*
     * TODO Change this method name when undeployBundles will be removed.
     */
    protected ReloadResult _undeployBundles(List<String> bundleNames) throws BundleException {
        ReloadResult result = new ReloadResult();
        BundleContext ctx = getBundleContext();
        ServiceReference ref = ctx.getServiceReference(PackageAdmin.class.getName());
        PackageAdmin srv = (PackageAdmin) ctx.getService(ref);
        try {
            for (String bundleName : bundleNames) {
                for (Bundle bundle : srv.getBundles(bundleName, null)) {
                    if (bundle != null && bundle.getState() == Bundle.ACTIVE) {
                        log.info("Before undeploy bundle with name '{}'.", bundleName);
                        bundle.stop();
                        bundle.uninstall();
                        result.undeployedBundles.add(bundle);
                        log.info("After undeploy bundle with name '{}'.", bundleName);
                    }
                }
            }
        } finally {
            ctx.ungetService(ref);
        }
        return result;
    }

    /**
     * Gets the un-deployed bundle from given {@link ReloadResult result} and try to remove them from
     * {@link sun.net.www.protocol.jar.JarFileFactory} otherwise we'll have resource conflict when opening
     * {@link InputStream stream} from {@link URL url}.
     */
    @SuppressWarnings({ "unchecked", "SynchronizationOnLocalVariableOrMethodParameter" })
    protected void clearJarFileFactoryCache(ReloadResult result) {
        try {
            List<String> jarLocations = result.undeployedBundlesAsStream().map(Bundle::getLocation).collect(
                    Collectors.toList());
            log.debug("Clear JarFileFactory caches for jars={}", jarLocations);
            Class<?> jarFileFactory = Class.forName("sun.net.www.protocol.jar.JarFileFactory");

            Object factoryInstance = FieldUtils.readStaticField(jarFileFactory, "instance", true);
            Map<String, JarFile> fileCache = (Map<String, JarFile>) FieldUtils.readStaticField(jarFileFactory, "fileCache", true);
            Map<JarFile, URL> urlCache = (Map<JarFile, URL>) FieldUtils.readStaticField(jarFileFactory, "urlCache", true);

            synchronized (factoryInstance) {
                // collect keys of cache
                List<JarFile> urlCacheRemoveKeys = new ArrayList<>();
                for (Entry<JarFile, URL> entry : urlCache.entrySet()) {
                    JarFile jarFile = entry.getKey();
                    if (jarLocations.stream().anyMatch(jar -> jar.startsWith(jarFile.getName()))) {
                        urlCacheRemoveKeys.add(jarFile);
                    }
                }

                List<String> fileCacheRemoveKeys = new ArrayList<>();
                for (Entry<String, JarFile> entry : fileCache.entrySet()) {
                    if (urlCacheRemoveKeys.contains(entry.getValue())) {
                        fileCacheRemoveKeys.add(entry.getKey());
                    }
                }

                // now remove from factory
                for (String fileCacheRemoveKey : fileCacheRemoveKeys) {
                    JarFile remove = fileCache.remove(fileCacheRemoveKey);
                    if (remove != null) {
                        log.trace("Removed item from fileCache={}", remove);
                    }
                }

                for (JarFile urlCacheRemoveKey : urlCacheRemoveKeys) {
                    URL remove = urlCache.remove(urlCacheRemoveKey);
                    try {
                        urlCacheRemoveKey.close();
                    } catch (IOException e) {
                        log.info("Unable to close JarFile={}", urlCacheRemoveKey, e);
                    }
                    if (remove != null) {
                        log.trace("Removed item from urlCache={}",  remove);
                    }
                }
            }
        } catch (ReflectiveOperationException | ClassCastException e) {
            log.error("Unable to clear JarFileFactory, you might need to restart Nuxeo", e);
        }
    }

    /**
     * This method needs to be called before bundle uninstallation, otherwise {@link Bundle#getLocation()} throw a NPE.
     */
    protected URL toURL(Bundle bundle) {
        String location = bundle.getLocation();
        File file = new File(location);
        return toURL(file);
    }

    protected URL toURL(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeServiceException(e);
        }
    }

    /**
     * Logs the {@link ComponentManager} status.
     */
    protected void logComponentManagerStatus() {
        log.debug(() -> {
            StringBuilder builder = new StringBuilder("ComponentManager status:\n");
            Framework.getRuntime().getStatusMessage(builder);
            return builder.toString();
        });
    }

    @Override
    public Long lastFlushed() {
        return lastFlushed;
    }

    /**
     * Sets the last date date to current date timestamp
     *
     * @since 5.6
     */
    protected void setFlushedNow() {
        lastFlushed = Long.valueOf(System.currentTimeMillis());
    }

    /**
     * @deprecated since 5.6, use {@link #runDeploymentPreprocessor()} instead. Keep it as compatibility code until
     *             NXP-9642 is done.
     */
    @Override
    @Deprecated(since = "5.6")
    public void installWebResources(File file) throws IOException {
        log.info("Install web resources");
        if (file.isDirectory()) {
            File war = new File(file, "web");
            war = new File(war, "nuxeo.war");
            if (war.isDirectory()) {
                org.nuxeo.common.utils.FileUtils.copyTree(war, getAppDir());
            } else {
                // compatibility mode with studio 1.5 - see NXP-6186
                war = new File(file, "nuxeo.war");
                if (war.isDirectory()) {
                    org.nuxeo.common.utils.FileUtils.copyTree(war, getAppDir());
                }
            }
        } else if (file.isFile()) { // a jar
            File war = getWarDir();
            ZipUtils.unzip("web/nuxeo.war", file, war);
            // compatibility mode with studio 1.5 - see NXP-6186
            ZipUtils.unzip("nuxeo.war", file, war);
        }
    }

    @Override
    public void runDeploymentPreprocessor() throws IOException {
        log.info("Start running deployment preprocessor");
        String rootPath = Environment.getDefault().getRuntimeHome().getAbsolutePath();
        File root = new File(rootPath);
        DeploymentPreprocessor processor = new DeploymentPreprocessor(root);
        // initialize
        processor.init();
        // and predeploy
        processor.predeploy();
        log.info("Deployment preprocessing done");
    }

    protected static File getAppDir() {
        return Environment.getDefault().getConfig().getParentFile();
    }

    protected static File getWarDir() {
        return new File(getAppDir(), "nuxeo.war");
    }

    @Override
    public String getOSGIBundleName(File file) {
        Manifest mf = JarUtils.getManifest(file);
        if (mf == null) {
            return null;
        }
        String bundleName = mf.getMainAttributes().getValue("Bundle-SymbolicName");
        if (bundleName == null) {
            return null;
        }
        int index = bundleName.indexOf(';');
        if (index > -1) {
            bundleName = bundleName.substring(0, index);
        }
        return bundleName;
    }

    /**
     * @deprecated since 9.3 should not be needed anymore
     */
    @Deprecated(since = "9.3")
    protected void triggerReloadWithNewTransaction(String eventId) {
        if (TransactionHelper.isTransactionMarkedRollback()) {
            throw new AssertionError("The calling transaction is marked rollback");
        }
        // we need to commit or rollback transaction because suspending it leads to a lock/errors when acquiring a new
        // connection during the datasource reload
        boolean hasTransaction = TransactionHelper.isTransactionActiveOrMarkedRollback();
        if (hasTransaction) {
            TransactionHelper.commitOrRollbackTransaction();
        }
        try {
            TransactionHelper.runInTransaction(() -> triggerReload(eventId));
        } finally {
            // start a new transaction only if one already existed
            // this is because there's no user transaction when coming from SDK
            if (hasTransaction) {
                TransactionHelper.startTransaction();
            }
        }
    }

    /**
     * @deprecated since 9.3 should not be needed anymore
     */
    @Deprecated(since = "9.3")
    protected void triggerReload(String eventId) {
        log.info("About to send reload event for id: {}", eventId);
        Framework.getService(EventService.class).sendEvent(new Event(RELOAD_TOPIC, BEFORE_RELOAD_EVENT_ID, this, null));
        try {
            Framework.getService(EventService.class).sendEvent(new Event(RELOAD_TOPIC, eventId, this, null));
        } finally {
            Framework.getService(EventService.class)
                     .sendEvent(new Event(RELOAD_TOPIC, AFTER_RELOAD_EVENT_ID, this, null));
            log.info("Returning from reload for event id: {}", eventId);
        }
    }
}
