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
package org.nuxeo.runtime.reload;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.jar.Manifest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.JarUtils;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.SharedResourceLoader;
import org.nuxeo.runtime.deployment.preprocessor.DeploymentPreprocessor;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventService;
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
    public void activate(ComponentContext context) throws Exception {
        super.activate(context);
        bundle = context.getRuntimeContext().getBundle();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        super.deactivate(context);
        bundle = null;
    }

    @Override
    public void reload(File[] additionalFiles) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("Starting reload");
        }
        reloadProperties();
        runDeploymentPreprocessor(additionalFiles);
        EventService eventService = Framework.getLocalService(EventService.class);
        eventService.sendEvent(new Event(RELOAD_TOPIC, RELOAD_EVENT_ID, this,
                null));
        if (log.isDebugEnabled()) {
            log.debug("Reload done");
        }
    }

    @Override
    public void reloadProperties() throws Exception {
        log.info("Reload runtime properties");
        Framework.getRuntime().reloadProperties();
    }

    @Override
    public void reloadRepository() throws Exception {
        log.info("Reload repository");
        Framework.getLocalService(EventService.class).sendEvent(
                new Event(RELOAD_TOPIC, RELOAD_REPOSITORIES_ID, this, null));
    }

    @Override
    public void reloadSeamComponents() throws Exception {
        log.info("Reload Seam components");
        Framework.getLocalService(EventService.class).sendEvent(
                new Event(RELOAD_TOPIC, RELOAD_SEAM_EVENT_ID, this, null));
    }

    @Override
    public void flush() throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("Starting flush");
        }
        flushJaasCache();
        EventService eventService = Framework.getLocalService(EventService.class);
        eventService.sendEvent(new Event(RELOAD_TOPIC, FLUSH_EVENT_ID, this,
                null));
        setFlushedNow();
        if (log.isDebugEnabled()) {
            log.debug("Flush done");
        }
    }

    @Override
    public void flushJaasCache() throws Exception {
        log.info("Flush the JAAS cache");
        EventService eventService = Framework.getLocalService(EventService.class);
        eventService.sendEvent(new Event("usermanager", "user_changed", this,
                "Deployer")); // the data argument is optional
        setFlushedNow();
    }

    @Override
    public void flushSeamComponents() throws Exception {
        log.info("Flush Seam components");
        Framework.getLocalService(EventService.class).sendEvent(
                new Event(RELOAD_TOPIC, FLUSH_SEAM_EVENT_ID, this, null));
        setFlushedNow();
    }

    @Override
    public String deployBundle(File file) throws Exception {
        return deployBundle(file, false);
    }

    @Override
    public String deployBundle(File file, boolean reloadResourceClasspath)
            throws MalformedURLException, BundleException {
        String name = getOSGIBundleName(file);
        if (name == null) {
            log.error(String.format(
                    "No Bundle-SymbolicName found in MANIFEST for jar at '%s'",
                    file.getAbsolutePath()));
            return null;
        }

        String path = file.getAbsolutePath();

        log.info(String.format(
                "Before deploy bundle for file at '%s'\n" + "%s", path,
                getRuntimeStatus()));

        if (reloadResourceClasspath) {
            URL url = new File(path).toURI().toURL();
            Framework.reloadResourceLoader(Arrays.asList(url), null);
        }

        // check if this is a bundle first
        Bundle newBundle = getBundleContext().installBundle(path);
        if (newBundle == null) {
            throw new IllegalArgumentException(
                    "Could not find a valid bundle at path: " + path);
        }
        newBundle.start();

        log.info(String.format("Deploy done for bundle with name '%s'.\n"
                + "%s", newBundle.getSymbolicName(), getRuntimeStatus()));

        return newBundle.getSymbolicName();
    }

    @Override
    public void undeployBundle(File file, boolean reloadResources)
            throws Exception {
        String name = getOSGIBundleName(file);
        String path = file.getAbsolutePath();
        if (name == null) {
            log.error(String.format(
                    "No Bundle-SymbolicName found in MANIFEST for jar at '%s'",
                    path));
            return;
        }

        undeployBundle(name);

        if (reloadResources) {
            URL url = new File(path).toURI().toURL();
            Framework.reloadResourceLoader(null, Arrays.asList(url));
        }
    }

    @Override
    public void undeployBundle(String bundleName) throws Exception {
        if (bundleName == null) {
            // ignore
            return;
        }
        log.info(String.format("Before undeploy bundle with name '%s'.\n"
                + "%s", bundleName, getRuntimeStatus()));
        BundleContext ctx = getBundleContext();
        ServiceReference ref = ctx.getServiceReference(PackageAdmin.class.getName());
        PackageAdmin srv = (PackageAdmin) ctx.getService(ref);
        try {
            for (Bundle b : srv.getBundles(bundleName, null)) {
                if (b != null && b.getState() == Bundle.ACTIVE) {
                    b.stop();
                    b.uninstall();
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
        lastFlushed = Long.valueOf(System.currentTimeMillis());
    }

    /**
     * @deprecated since 5.6, use {@link #runDeploymentPreprocessor()} instead
     */
    @Deprecated
    public void installWebResources(File file) throws Exception {
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

    /**
     * Rebuild the framework resource class loader and add to it the given file
     * paths.
     * <p>
     * The already added paths are removed from the class loader. FIXME: is this
     * an issue for hot-reloading of multiple jars?
     */
    protected static void reloadResourceClassPath(Collection<String> files)
            throws Exception {
        Framework.reloadResourceLoader();
        SharedResourceLoader loader = Framework.getResourceLoader();
        if (files != null) {
            for (String path : files) {
                URL url = new File(path).toURI().toURL();
                loader.addURL(url);
            }
        }
    }

    public void runDeploymentPreprocessor(File[] additionalFiles) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("Start running deployment preprocessor");
        }
        DeploymentPreprocessor.reprocess(additionalFiles);
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
        String bundleName = mf.getMainAttributes().getValue(
                "Bundle-SymbolicName");
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

}
