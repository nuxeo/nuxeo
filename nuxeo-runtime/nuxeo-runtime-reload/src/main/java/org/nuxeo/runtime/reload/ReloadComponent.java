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
import java.util.List;
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
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.deployment.preprocessor.DeploymentPreprocessor;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventService;
import org.nuxeo.runtime.transaction.TransactionHelper;
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

    protected void refreshComponents() {
        String reloadStrategy = Framework.getProperty(RELOAD_STRATEGY_PARAMETER, RELOAD_STRATEGY_VALUE_DEFAULT);
        if (log.isInfoEnabled()) {
            log.info("Refresh components. Strategy: " + reloadStrategy);
        }
        // reload components / contributions
        ComponentManager mgr = Framework.getRuntime().getComponentManager();
        switch(reloadStrategy) {
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
        log.info("Reload runtime properties");
        Framework.getRuntime().reloadProperties();
    }

    @Override
    public void reloadRepository() throws InterruptedException {
        log.info("Reload repository");
        triggerReloadWithNewTransaction(RELOAD_REPOSITORIES_ID);
    }

    @Override
    public void reloadSeamComponents() {
        log.info("Reload Seam components");
        Framework.getLocalService(EventService.class)
                 .sendEvent(new Event(RELOAD_TOPIC, RELOAD_SEAM_EVENT_ID, this, null));
    }

    @Override
    public void flush() {
        log.info("Flush caches");
        Framework.getLocalService(EventService.class).sendEvent(new Event(RELOAD_TOPIC, FLUSH_EVENT_ID, this, null));
        flushJaasCache();
        setFlushedNow();
    }

    @Override
    public void flushJaasCache() {
        log.info("Flush the JAAS cache");
        Framework.getLocalService(EventService.class)
                 .sendEvent(new Event("usermanager", "user_changed", this, "Deployer"));
        setFlushedNow();
    }

    @Override
    public void flushSeamComponents() {
        log.info("Flush Seam components");
        Framework.getLocalService(EventService.class)
                 .sendEvent(new Event(RELOAD_TOPIC, FLUSH_SEAM_EVENT_ID, this, null));
        setFlushedNow();
    }

    @Override
    public void deployBundles(List<File> files, boolean reloadResources) throws BundleException {
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
            BundleContext bundleContext = getBundleContext();
            for (File file : files) {
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
                if (log.isInfoEnabled()) {
                    log.info(String.format("Deploy done for bundle with name '%s'", bundle.getSymbolicName()));
                }
            }
            refreshComponents();
        } finally {
            TransactionHelper.resumeTransaction(tx);
        }

        if (log.isInfoEnabled()) {
            StringBuilder builder = new StringBuilder("After deploy bundles\n");
            Framework.getRuntime().getStatusMessage(builder);
            log.info(builder.toString());
        }
    }

    @Override
    public void undeployBundles(List<String> bundleNames, boolean reloadResources) throws BundleException {
        if (log.isInfoEnabled()) {
            StringBuilder builder = new StringBuilder("Before undeploy bundles\n");
            Framework.getRuntime().getStatusMessage(builder);
            log.info(builder.toString());
        }

        // Undeploy bundles
        BundleContext ctx = getBundleContext();
        ServiceReference ref = ctx.getServiceReference(PackageAdmin.class.getName());
        PackageAdmin srv = (PackageAdmin) ctx.getService(ref);
        Transaction tx = TransactionHelper.suspendTransaction();
        // use a stream builder because it is optimized for one element only
        Stream.Builder<String> paths = Stream.builder();
        try {
            for (String bundleName : bundleNames) {
                for (Bundle bundle : srv.getBundles(bundleName, null)) {
                    if (bundle != null && bundle.getState() == Bundle.ACTIVE) {
                        if (log.isInfoEnabled()) {
                            log.info(String.format("Before undeploy bundle with name '%s'.", bundleName));
                        }
                        paths.add(bundle.getLocation());
                        bundle.stop();
                        bundle.uninstall();
                        if (log.isInfoEnabled()) {
                            log.info(String.format("After undeploy bundle with name '%s'.", bundleName));
                        }
                    }
                }
            }
            refreshComponents();
        } finally {
            TransactionHelper.resumeTransaction(tx);
            ctx.ungetService(ref);
        }

        // Reload resources
        if (reloadResources) {
            List<URL> urls = paths.build().map(File::new).map(this::toURL).collect(Collectors.toList());
            Framework.reloadResourceLoader(null, urls);
        }

        if (log.isInfoEnabled()) {
            StringBuilder builder = new StringBuilder("After undeploy bundles\n");
            Framework.getRuntime().getStatusMessage(builder);
            log.info(builder.toString());
        }
    }

    protected URL toURL(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeServiceException(e);
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
        log.debug("Start running deployment preprocessor");
        String rootPath = Environment.getDefault().getRuntimeHome().getAbsolutePath();
        File root = new File(rootPath);
        DeploymentPreprocessor processor = new DeploymentPreprocessor(root);
        // initialize
        processor.init();
        // and predeploy
        processor.predeploy();
        log.debug("Deployment preprocessing done");
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
        Framework.getLocalService(EventService.class)
                 .sendEvent(new Event(RELOAD_TOPIC, BEFORE_RELOAD_EVENT_ID, this, null));
        try {
            Framework.getLocalService(EventService.class).sendEvent(new Event(RELOAD_TOPIC, eventId, this, null));
        } finally {
            Framework.getLocalService(EventService.class)
                     .sendEvent(new Event(RELOAD_TOPIC, AFTER_RELOAD_EVENT_ID, this, null));
            log.info("Returning from reload for event id: " + eventId);
        }
    }
}
