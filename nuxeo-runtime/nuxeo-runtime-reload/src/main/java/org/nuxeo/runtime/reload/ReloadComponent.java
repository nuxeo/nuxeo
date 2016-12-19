/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.runtime.reload;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Collections;
import java.util.jar.Manifest;

import javax.transaction.Transaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.JarUtils;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.ServicePassivator;
import org.nuxeo.runtime.deployment.preprocessor.DeploymentPreprocessor;
import org.nuxeo.runtime.model.ComponentContext;
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

    @Override
    public void reload() {
        if (log.isDebugEnabled()) {
            log.debug("Starting reload");
        }
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
    public void reloadRepository() {
        log.info("Reload repository");
        triggerReloadWithNewTransaction(RELOAD_REPOSITORIES_ID);
    }

    @Override
    public void reloadSeamComponents() {
        log.info("Reload Seam components");
        Framework.getLocalService(EventService.class).sendEvent(
                new Event(RELOAD_TOPIC, RELOAD_SEAM_EVENT_ID, this, null));
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
        Framework.getLocalService(EventService.class).sendEvent(
                new Event("usermanager", "user_changed", this, "Deployer"));
        setFlushedNow();
    }

    @Override
    public void flushSeamComponents() {
        log.info("Flush Seam components");
        Framework.getLocalService(EventService.class).sendEvent(
                new Event(RELOAD_TOPIC, FLUSH_SEAM_EVENT_ID, this, null));
        setFlushedNow();
    }

    @Override
    public String deployBundle(File file) throws BundleException {
        return deployBundle(file, false);
    }

    @Override
    public String deployBundle(File file, boolean reloadResourceClasspath) throws BundleException {
        String name = getOSGIBundleName(file);
        if (name == null) {
            log.error(String.format("No Bundle-SymbolicName found in MANIFEST for jar at '%s'", file.getAbsolutePath()));
            return null;
        }

        String path = file.getAbsolutePath();

        log.info(String.format("Before deploy bundle for file at '%s'\n" + "%s", path, getRuntimeStatus()));

        if (reloadResourceClasspath) {
            URL url;
            try {
                url = new File(path).toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            Framework.reloadResourceLoader(Collections.singletonList(url), null);
        }

        // check if this is a bundle first
        Bundle newBundle = getBundleContext().installBundle(path);
        if (newBundle == null) {
            throw new IllegalArgumentException("Could not find a valid bundle at path: " + path);
        }
        Transaction tx = TransactionHelper.suspendTransaction();
        try {
            newBundle.start();
        } finally {
            TransactionHelper.resumeTransaction(tx);
        }

        log.info(String.format("Deploy done for bundle with name '%s'.\n" + "%s", newBundle.getSymbolicName(),
                getRuntimeStatus()));

        return newBundle.getSymbolicName();
    }

    @Override
    public void undeployBundle(File file, boolean reloadResources) throws BundleException {
        String name = getOSGIBundleName(file);
        String path = file.getAbsolutePath();
        if (name == null) {
            log.error(String.format("No Bundle-SymbolicName found in MANIFEST for jar at '%s'", path));
            return;
        }

        undeployBundle(name);

        if (reloadResources) {
            URL url;
            try {
                url = new File(path).toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            Framework.reloadResourceLoader(null, Collections.singletonList(url));
        }
    }

    @Override
    public void undeployBundle(String bundleName) throws BundleException {
        if (bundleName == null) {
            // ignore
            return;
        }
        log.info(String.format("Before undeploy bundle with name '%s'.\n" + "%s", bundleName, getRuntimeStatus()));
        BundleContext ctx = getBundleContext();
        ServiceReference ref = ctx.getServiceReference(PackageAdmin.class.getName());
        PackageAdmin srv = (PackageAdmin) ctx.getService(ref);
        try {
            for (Bundle b : srv.getBundles(bundleName, null)) {
                if (b != null && b.getState() == Bundle.ACTIVE) {
                    Transaction tx = TransactionHelper.suspendTransaction();
                    try {
                        b.stop();
                        b.uninstall();
                    } finally {
                        TransactionHelper.resumeTransaction(tx);
                    }
                }
            }
        } finally {
            ctx.ungetService(ref);
        }
        log.info(String.format("Undeploy done.\n" + "%s", getRuntimeStatus()));
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
        lastFlushed = System.currentTimeMillis();
    }

    @Override
    public void runDeploymentPreprocessor() throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("Start running deployment preprocessor");
        }
        String rootPath = Environment.getDefault().getRuntimeHome().getAbsolutePath();
        File root = new File(rootPath);
        DeploymentPreprocessor processor = new DeploymentPreprocessor(root);
        // initialize
        processor.init();
        // and predeploy
        processor.predeploy();
        if (log.isDebugEnabled()) {
            log.debug("Deployment preprocessing done");
        }
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

    protected String getRuntimeStatus() {
        StringBuilder msg = new StringBuilder();
        RuntimeService runtime = Framework.getRuntime();
        runtime.getStatusMessage(msg);
        return msg.toString();
    }

    protected void triggerReloadWithNewTransaction(String id) {
        if (TransactionHelper.isTransactionMarkedRollback()) {
            throw new AssertionError("The calling transaction is marked rollback");
        }
        // we need to commit or rollback transaction because suspending it leads to a lock/errors when acquiring a new
        // connection during the datasource reload
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        try {
            try {
                triggerReloadWithPassivate(id);
            } catch (RuntimeException cause) {
                TransactionHelper.setTransactionRollbackOnly();
                throw cause;
            } finally {
                TransactionHelper.commitOrRollbackTransaction();
            }
        } finally {
            TransactionHelper.startTransaction();
        }
    }

    protected void triggerReloadWithPassivate(String id) {
        log.info("about to passivate for " + id);
        Framework.getLocalService(EventService.class).sendEvent(
                new Event(RELOAD_TOPIC, BEFORE_RELOAD_EVENT_ID, this, null));
        try {
            ServicePassivator.proceed(Duration.ofSeconds(5), Duration.ofSeconds(30), true, () -> {
                log.info("about to send " + id);
                Framework.getLocalService(EventService.class).sendEvent(new Event(RELOAD_TOPIC, id, this, null));
            }).onFailure(
                    snapshot -> {
                        throw new UnsupportedOperationException("Detected access, should initiate a reboot "
                                + snapshot.toString());
                    });
        } finally {
            Framework.getLocalService(EventService.class).sendEvent(
                    new Event(RELOAD_TOPIC, AFTER_RELOAD_EVENT_ID, this, null));
            log.info("returning from " + id);
        }
    }
}
