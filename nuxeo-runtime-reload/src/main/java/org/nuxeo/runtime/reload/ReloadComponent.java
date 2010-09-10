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
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.SharedResourceLoader;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class ReloadComponent extends DefaultComponent implements ReloadService {

    private static final Log log = LogFactory.getLog(ReloadComponent.class);

    public static String RELOAD_TOPIC = "org.nuxeo.runtime.reload";

    protected static Bundle bundle;

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
    public void flushJassCache() throws Exception {
        EventService eventService = Framework.getLocalService(EventService.class);
        eventService.sendEvent(new Event("usermanager", "user_changed", this,
                "Deployer")); // the data argument is optional
    }

    @Override
    public void reloadRepository() throws Exception {
        Framework.getLocalService(EventService.class).sendEvent(
                new Event(RELOAD_TOPIC, "reloadRepository", this, null));
    }

    public void deployBundle(File file, boolean reloadResourceClassPath)
            throws Exception {
        // TODO this will remove from classpath other bundles deployed at
        // runtime and server was not restarted since.
        String path = file.getAbsolutePath();
        if (reloadResourceClassPath) {
            reloadResourceClassPath(Collections.singletonList(path));
        }
        Bundle bundle = Framework.getRuntime().getContext().getBundle().getBundleContext().installBundle(
                path);
        bundle.start();
        // run fragment processor if needed
        processFragment(file);
        reloadRepository();
    }

    @Override
    public void deployBundle(File file) throws Exception {
        deployBundle(file, true);
    }

    public void reloadProperties() throws Exception {
        Framework.getRuntime().reloadProperties();
    }

    /**
     * Rebuild the framework resource class loader and add to it the given file
     * paths.
     * 
     * The already added paths are removed from the class loader.
     * 
     * @param files
     * @throws Exception
     */
    public static void reloadResourceClassPath(Collection<String> files)
            throws Exception {
        Framework.reloadResourceLoader();
        SharedResourceLoader loader = Framework.getResourceLoader();
        for (String path : files) {
            URL url = new File(path).toURI().toURL();
            loader.addURL(url);
        }
    }

    public static void processFragment(File file) throws Exception {
        log.info("running fragment processor");
        // we cannot use DeploymentPreprocessor since the initial preprocessing
        // will be overridden
        if (file.isDirectory()) {
            File war = new File(file, "nuxeo.war");
            if (war.isDirectory()) {
                FileUtils.copyTree(war, getAppDir());
            }
        } else if (file.isFile()) { // a jar
            File war = getWarDir();
            ZipUtils.unzip("nuxeo.war", file, war);
        }
    }

    public static File getAppDir() {
        return Environment.getDefault().getConfig().getParentFile();
    }

    public static File getWarDir() {
        return new File(getAppDir(), "nuxeo.war");
    }

}
