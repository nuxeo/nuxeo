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
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.SharedResourceLoader;
import org.nuxeo.runtime.deployment.preprocessor.DeploymentPreprocessor;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
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
    public void reload() throws Exception {
        reloadProperties();
        EventService eventService = Framework.getLocalService(EventService.class);
        eventService.sendEvent(new Event(RELOAD_TOPIC, RELOAD_EVENT_ID, this,
                null));
    }

    @Override
    public void reloadProperties() throws Exception {
        Framework.getRuntime().reloadProperties();
    }

    @Override
    public void reloadRepository() throws Exception {
        Framework.getLocalService(EventService.class).sendEvent(
                new Event(RELOAD_TOPIC, "reloadRepositories", this, null));
    }

    @Override
    public void reloadSeamComponents() throws Exception {
        Framework.getLocalService(EventService.class).sendEvent(
                new Event(RELOAD_TOPIC, RELOAD_SEAM_EVENT_ID, this, null));
    }

    @Override
    public void flush() throws Exception {
        flushJaasCache();
        EventService eventService = Framework.getLocalService(EventService.class);
        eventService.sendEvent(new Event(RELOAD_TOPIC, FLUSH_EVENT_ID, this,
                null));
        setFlushedNow();
    }

    @Override
    public void flushJaasCache() throws Exception {
        EventService eventService = Framework.getLocalService(EventService.class);
        eventService.sendEvent(new Event("usermanager", "user_changed", this,
                "Deployer")); // the data argument is optional
        setFlushedNow();
    }

    @Override
    public void flushSeamComponents() throws Exception {
        Framework.getLocalService(EventService.class).sendEvent(
                new Event(RELOAD_TOPIC, FLUSH_SEAM_EVENT_ID, this, null));
        setFlushedNow();
    }

    public String deployBundle(File file, boolean reloadResourceClasspath)
            throws Exception {
        String name = getOSGIBundleName(file);
        if (name == null) {
            log.error(String.format(
                    "No Bundle-SymbolicName found in MANIFEST for jar at '%s'",
                    file.getAbsolutePath()));
            return null;
        }

        String path = file.getAbsolutePath();
        // FIXME this will remove from classpath other bundles deployed at
        // runtime
        if (reloadResourceClasspath) {
            reloadResourceClassPath(Collections.singletonList(path));
        }

        // check if this is a bundle

        Bundle newBundle = getBundleContext().installBundle(path);
        if (newBundle == null) {
            throw new IllegalArgumentException(
                    "Could not find a valid bundle at path: " + path);
        }
        newBundle.start();
        return newBundle.getSymbolicName();
    }

    @Override
    public void undeployBundle(File file) throws Exception {
        BundleContext ctx = getBundleContext();

        String name = getOSGIBundleName(file);
        if (name == null) {
            log.error(String.format(
                    "No Bundle-SymbolicName found in MANIFEST for jar at '%s'",
                    file.getAbsolutePath()));
        } else {
            ServiceReference ref = ctx.getServiceReference(PackageAdmin.class.getName());
            PackageAdmin srv = (PackageAdmin) ctx.getService(ref);
            try {
                for (Bundle b : srv.getBundles(name, null)) {
                    if (b != null && b.getState() == Bundle.ACTIVE) {
                        b.stop();
                        b.uninstall();
                    }
                }
            } finally {
                ctx.ungetService(ref);
            }
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

    public void installWebResources(File file) throws Exception {
        log.info("running fragment processor");
        // we cannot use DeploymentPreprocessor since the initial preprocessing
        // will be overridden
        // FIXME: handle other resources (message bundles for instance)
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
     * The already added paths are removed from the class loader. FIXME: is
     * this an issue for hot-reloading of multiple jars?
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

    public void runDeploymentPreprocessor() throws Exception {
        String rootPath = Environment.getDefault().getHome().getAbsolutePath();
        File root = new File(rootPath);
        DeploymentPreprocessor processor = new DeploymentPreprocessor(root);
        // initialize
        processor.init();
        // and predeploy
        processor.predeploy();
    }

    protected static File getAppDir() {
        return Environment.getDefault().getConfig().getParentFile();
    }

    protected static File getWarDir() {
        return new File(getAppDir(), "nuxeo.war");
    }

    @Override
    public String getOSGIBundleName(File file) {
        if (!file.exists() || file.isDirectory()) {
            return null;
        }
        try {
            JarFile jar = new JarFile(file);
            Manifest mf = jar.getManifest();
            if (mf != null) {
                return mf.getMainAttributes().getValue("Bundle-SymbolicName");
            }
        } catch (IOException e) {
            // maybe not even a jar
            return null;
        }
        return null;
    }

}
