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
package org.nuxeo.runtime.reload;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.transaction.Transaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
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

    private static final Log log = LogFactory.getLog(ReloadComponent.class);

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
    @Deprecated
    protected void refreshComponents() {
        String reloadStrategy = Framework.getProperty(RELOAD_STRATEGY_PARAMETER, RELOAD_STRATEGY_VALUE_DEFAULT);
        if (log.isInfoEnabled()) {
            log.info("Refresh components. Strategy: " + reloadStrategy);
        }
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
    public void reload() throws InterruptedException {
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
        Framework.getService(EventService.class)
                 .sendEvent(new Event(RELOAD_TOPIC, RELOAD_SEAM_EVENT_ID, this, null));
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
        Framework.getService(EventService.class)
                 .sendEvent(new Event("usermanager", "user_changed", this, "Deployer"));
        setFlushedNow();
        log.info("After flush the JAAS cache");
    }

    @Override
    public void flushSeamComponents() {
        log.info("Flush Seam components");
        Framework.getService(EventService.class)
                 .sendEvent(new Event(RELOAD_TOPIC, FLUSH_SEAM_EVENT_ID, this, null));
        setFlushedNow();
    }

    /**
     * @deprecated since 9.3 use {@link #reloadBundles(ReloadContext)} instead.
     */
    @Override
    @Deprecated
    public void deployBundles(List<File> files, boolean reloadResources) throws BundleException {
        long begin = System.currentTimeMillis();
        List<String> missingNames = files.stream()
                                         .filter(file -> getOSGIBundleName(file) == null)
                                         .map(File::getAbsolutePath)
                                         .collect(Collectors.toList());
        if (!missingNames.isEmpty()) {
            missingNames.forEach(
                    name -> log.error(String.format("No Bundle-SymbolicName found in MANIFEST for jar at '%s'", name)));
            // TODO investigate why we need to exit here, getBundleContext().installBundle(path) will throw an exception
            // unless, maybe tests ?
            return;
        }
        if (log.isInfoEnabled()) {
            StringBuilder builder = new StringBuilder("Before deploy bundles\n");
            Framework.getRuntime().getStatusMessage(builder);
            log.info(builder.toString());
        }

        // Reload resources
        if (reloadResources) {
            List<URL> urls = files.stream().map(this::toURL).collect(Collectors.toList());
            Framework.reloadResourceLoader(urls, null);
        }

        // Deploy bundles
        Transaction tx = TransactionHelper.suspendTransaction();
        try {
            _deployBundles(files);
            refreshComponents();
        } finally {
            TransactionHelper.resumeTransaction(tx);
        }

        if (log.isInfoEnabled()) {
            StringBuilder builder = new StringBuilder("After deploy bundles.\n");
            Framework.getRuntime().getStatusMessage(builder);
            log.info(builder.toString());
            log.info(String.format("Hot deploy was done in %s ms.", System.currentTimeMillis() - begin));
        }
    }

    /**
     * @deprecated since 9.3 use {@link #reloadBundles(ReloadContext)} instead.
     */
    @Override
    @Deprecated
    public void undeployBundles(List<String> bundleNames, boolean reloadResources) throws BundleException {
        long begin = System.currentTimeMillis();
        if (log.isInfoEnabled()) {
            StringBuilder builder = new StringBuilder("Before undeploy bundles\n");
            Framework.getRuntime().getStatusMessage(builder);
            log.info(builder.toString());
        }

        // Undeploy bundles
        Transaction tx = TransactionHelper.suspendTransaction();
        ReloadResult result = new ReloadResult();
        try {
            result.merge(_undeployBundles(bundleNames));
            refreshComponents();
        } finally {
            TransactionHelper.resumeTransaction(tx);
        }

        // Reload resources
        if (reloadResources) {
            List<URL> undeployedBundleURLs = result.undeployedBundles.stream()
                                                                     .map(this::toURL)
                                                                     .collect(Collectors.toList());
            Framework.reloadResourceLoader(null, undeployedBundleURLs);
        }

        if (log.isInfoEnabled()) {
            StringBuilder builder = new StringBuilder("After undeploy bundles.\n");
            Framework.getRuntime().getStatusMessage(builder);
            log.info(builder.toString());
            log.info(String.format("Hot undeploy was done in %s ms.", System.currentTimeMillis() - begin));
        }
    }

    @Override
    public ReloadResult reloadBundles(ReloadContext context) throws BundleException {
        ReloadResult result = new ReloadResult();
        List<String> bundlesNamesToUndeploy = context.bundlesNamesToUndeploy;

        Watch watch = new Watch(new LinkedHashMap<>()).start();
        if (log.isInfoEnabled()) {
            StringBuilder builder = new StringBuilder("Before updating Nuxeo server\n");
            Framework.getRuntime().getStatusMessage(builder);
            log.info(builder.toString());
        }
        // get class loader
        Optional<DevMutableClassLoader> classLoader = Optional.of(getClass().getClassLoader())
                                                              .filter(DevMutableClassLoader.class::isInstance)
                                                              .map(DevMutableClassLoader.class::cast);

        watch.start("flush");
        flush();
        watch.stop("flush");

        // Suspend current transaction
        Transaction tx = TransactionHelper.suspendTransaction();

        try {
            // Stop or Standby the component manager
            ComponentManager componentManager = Framework.getRuntime().getComponentManager();
            String reloadStrategy = Framework.getProperty(RELOAD_STRATEGY_PARAMETER, RELOAD_STRATEGY_VALUE_DEFAULT);
            if (log.isInfoEnabled()) {
                log.info("Component reload strategy=" + reloadStrategy);
            }

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
            TransactionHelper.resumeTransaction(tx);
        }
        if (log.isInfoEnabled()) {
            StringBuilder builder = new StringBuilder("After updating Nuxeo server\n");
            Framework.getRuntime().getStatusMessage(builder);
            log.info(builder.toString());
        }

        watch.stop();
        if (log.isInfoEnabled()) {
            StringBuilder message = new StringBuilder();
            message.append("Hot reload was done in ")
                   .append(watch.getTotal().elapsed(TimeUnit.MILLISECONDS))
                   .append(" ms, detailed steps:");
            Stream.of(watch.getIntervals())
                  .forEach(i -> message.append("\n- ")
                                       .append(i.getName())
                                       .append(": ")
                                       .append(i.elapsed(TimeUnit.MILLISECONDS))
                                       .append(" ms"));
            log.info(message.toString());
        }
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
                        bundlePath = Files.copy(bundlePath, destinationPath.resolve(bundle.getName()),
                                StandardCopyOption.REPLACE_EXISTING);
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
            if (log.isInfoEnabled()) {
                log.info(String.format("Before deploy bundle for file at '%s'", path));
            }
            Bundle bundle = bundleContext.installBundle(path);
            if (bundle == null) {
                // TODO check why this is necessary, our implementation always return sth
                throw new IllegalArgumentException("Could not find a valid bundle at path: " + path);
            }
            bundle.start();
            result.deployedBundles.add(bundle);
            if (log.isInfoEnabled()) {
                log.info(String.format("Deploy done for bundle with name '%s'", bundle.getSymbolicName()));
            }
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
                        if (log.isInfoEnabled()) {
                            log.info(String.format("Before undeploy bundle with name '%s'.", bundleName));
                        }
                        bundle.stop();
                        bundle.uninstall();
                        result.undeployedBundles.add(bundle);
                        if (log.isInfoEnabled()) {
                            log.info(String.format("After undeploy bundle with name '%s'.", bundleName));
                        }
                    }
                }
            }
        } finally {
            ctx.ungetService(ref);
        }
        return result;
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
        if (log.isDebugEnabled()) {
            StringBuilder builder = new StringBuilder("ComponentManager status:\n");
            Framework.getRuntime().getStatusMessage(builder);
            log.debug(builder.toString());
        }
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
    @Deprecated
    public void installWebResources(File file) throws IOException {
        log.info("Install web resources");
        if (file.isDirectory()) {
            File war = new File(file, "web");
            war = new File(war, "nuxeo.war");
            if (war.isDirectory()) {
                FileUtils.copyTree(war, getAppDir());
            } else {
                // compatibility mode with studio 1.5 - see NXP-6186
                war = new File(file, "nuxeo.war");
                if (war.isDirectory()) {
                    FileUtils.copyTree(war, getAppDir());
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
    @Deprecated
    protected void triggerReloadWithNewTransaction(String eventId) throws InterruptedException {
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
    @Deprecated
    protected void triggerReload(String eventId) {
        log.info("About to send reload event for id: " + eventId);
        Framework.getService(EventService.class)
                 .sendEvent(new Event(RELOAD_TOPIC, BEFORE_RELOAD_EVENT_ID, this, null));
        try {
            Framework.getService(EventService.class).sendEvent(new Event(RELOAD_TOPIC, eventId, this, null));
        } finally {
            Framework.getService(EventService.class)
                     .sendEvent(new Event(RELOAD_TOPIC, AFTER_RELOAD_EVENT_ID, this, null));
            log.info("Returning from reload for event id: " + eventId);
        }
    }
}
