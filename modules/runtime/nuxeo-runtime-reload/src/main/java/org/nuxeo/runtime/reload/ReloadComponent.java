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

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.apache.commons.io.FileUtils.ONE_MB;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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

import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.JarUtils;
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

    public static final String RELOAD_STRATEGY_VALUE_STANDBY = "standby";

    public static final String RELOAD_STRATEGY_VALUE_RESTART = "restart";

    public static final String RELOAD_STRATEGY_VALUE_DEFAULT = RELOAD_STRATEGY_VALUE_STANDBY;

    private static final Logger log = LogManager.getLogger(ReloadComponent.class);

    /**
     * The file copy buffer size (30 MB), copied from commons-io:commons-io
     *
     * @since 2021.19
     */
    private static final long FILE_COPY_BUFFER_SIZE = ONE_MB * 30;

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

    @Override
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

                result.merge(undeployBundles(bundlesNamesToUndeploy));
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

                result.merge(deployBundles(bundlesToDeploy));
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
                final Path srcBundlePath = bundle.toPath();
                final Path destBundlePath;
                // check if the bundle is located under the desired destination
                // if not copy it to the desired destination
                if (srcBundlePath.startsWith(destinationPath)) {
                    destBundlePath = srcBundlePath;
                } else {
                    if (Files.isDirectory(srcBundlePath)) {
                        // If it's a directory, assume that it's an exploded jar
                        destBundlePath = JarUtils.zipDirectory(srcBundlePath,
                                destinationPath.resolve("hotreload-bundle-" + System.currentTimeMillis() + ".jar"),
                                REPLACE_EXISTING);
                    } else {
                        destBundlePath = destinationPath.resolve(bundle.getName());
                        if (SystemUtils.IS_OS_WINDOWS) {
                            // JDK nio Files will replace the existing file if the destination already exists
                            // this is an issue on Windows because you can't replace a file used by the JVM
                            // so copy the file by using the InputStream API
                            copyFile(bundle, destBundlePath.toFile());
                        } else {
                            Files.copy(srcBundlePath, destBundlePath, REPLACE_EXISTING);
                        }
                    }
                }
                bundlesToDeploy.add(destBundlePath.toFile());
            }
            return bundlesToDeploy;
        } catch (IOException e) {
            throw new BundleException("Unable to copy bundles to " + destinationPath, e);
        }
    }

    /**
     * Method copied from commons-io:commons-io:2.6 to handle replace if exist on Windows.
     *
     * @since 2021.19
     */
    protected void copyFile(File srcFile, File destFile) throws IOException {
        if (!srcFile.exists()) {
            throw new FileNotFoundException("Source '" + srcFile + "' does not exist");
        }
        if (srcFile.isDirectory()) {
            throw new IOException("Source '" + srcFile + "' exists but is a directory");
        }
        if (srcFile.getCanonicalPath().equals(destFile.getCanonicalPath())) {
            throw new IOException("Source '" + srcFile + "' and destination '" + destFile + "' are the same");
        }
        File parentFile = destFile.getParentFile();
        if (parentFile != null) {
            if (!parentFile.mkdirs() && !parentFile.isDirectory()) {
                throw new IOException("Destination '" + parentFile + "' directory cannot be created");
            }
        }
        if (destFile.exists() && !destFile.canWrite()) {
            throw new IOException("Destination '" + destFile + "' exists but is read-only");
        }
        if (destFile.exists() && destFile.isDirectory()) {
            throw new IOException("Destination '" + destFile + "' exists but is a directory");
        }

        try (var fis = new FileInputStream(srcFile);
                var input = fis.getChannel();
                var fos = new FileOutputStream(destFile);
                var output = fos.getChannel()) {
            long size = input.size();
            long pos = 0;
            long count = 0;
            while (pos < size) {
                final long remain = size - pos;
                count = Math.min(remain, FILE_COPY_BUFFER_SIZE);
                final long bytesCopied = output.transferFrom(input, pos, count);
                if (bytesCopied == 0) { // IO-385 - can happen if file is truncated after caching the size
                    break; // ensure we don't loop forever
                }
                pos += bytesCopied;
            }
        }
    }

    protected ReloadResult deployBundles(List<File> bundlesToDeploy) throws BundleException {
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

    protected ReloadResult undeployBundles(List<String> bundleNames) throws BundleException {
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
     * sun.net.www.protocol.jar.JarFileFactory otherwise we'll have resource conflict when opening {@link InputStream
     * stream} from {@link URL url}.
     */
    @SuppressWarnings({ "unchecked" })
    protected void clearJarFileFactoryCache(ReloadResult result) {
        try {
            List<String> jarLocations = result.undeployedBundlesAsStream()
                                              .map(Bundle::getLocation)
                                              .collect(Collectors.toList());
            log.debug("Clear JarFileFactory caches for jars={}", jarLocations);
            Class<?> jarFileFactory = Class.forName("sun.net.www.protocol.jar.JarFileFactory");

            Object factoryInstance = FieldUtils.readStaticField(jarFileFactory, "instance", true);
            Map<String, JarFile> fileCache = (Map<String, JarFile>) FieldUtils.readStaticField(jarFileFactory,
                    "fileCache", true);
            Map<JarFile, URL> urlCache = (Map<JarFile, URL>) FieldUtils.readStaticField(jarFileFactory, "urlCache",
                    true);

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
                        log.trace("Removed item from urlCache={}", remove);
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
