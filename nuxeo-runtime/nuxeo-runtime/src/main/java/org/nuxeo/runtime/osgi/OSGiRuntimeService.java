/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.runtime.osgi;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.runtime.AbstractRuntimeService;
import org.nuxeo.runtime.Version;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.RuntimeContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;

/**
 * The default implementation of NXRuntime over an OSGi compatible environment.
 *
 * @author Bogdan Stefanescu
 * @author Florent Guillaume
 */
public class OSGiRuntimeService extends AbstractRuntimeService implements
        FrameworkListener {

    /** Can be used to change the runtime home directory */
    public static final String PROP_HOME_DIR = "org.nuxeo.runtime.home";

    /** The OSGi application install directory. */
    public static final String PROP_INSTALL_DIR = "INSTALL_DIR";

    /** The OSGi application config directory. */
    public static final String PROP_CONFIG_DIR = "CONFIG_DIR";

    /** The host adapter. */
    public static final String PROP_HOST_ADAPTER = "HOST_ADAPTER";

    public static final String PROP_NUXEO_BIND_ADDRESS = "nuxeo.bind.address";

    public static final String NAME = "OSGi NXRuntime";

    public static final Version VERSION = Version.parseString("1.4.0");

    private static final Log log = LogFactory.getLog(OSGiRuntimeService.class);

    private static final Log componentDebugLog = LogFactory.getLog("nuxeo.bundle.debug");

    private final BundleContext bundleContext;

    private final Map<String, RuntimeContext> contexts;

    public OSGiRuntimeService(BundleContext context) {
        super(new OSGiRuntimeContext(context.getBundle()));
        bundleContext = context;
        contexts = new ConcurrentHashMap<String, RuntimeContext>();
        String bindAddress = context.getProperty(PROP_NUXEO_BIND_ADDRESS);
        if (bindAddress != null) {
            properties.put(PROP_NUXEO_BIND_ADDRESS, bindAddress);
        }
        String homeDir = getProperty(PROP_HOME_DIR);
        componentDebugLog.info("Home directory: " + homeDir);

        if (homeDir != null) {
            workingDir = new File(homeDir);
        } else {
            workingDir = bundleContext.getDataFile("/");
        }
        workingDir.mkdirs();
        componentDebugLog.info("Working directory: " + workingDir);
    }

    public String getName() {
        return NAME;
    }

    public Version getVersion() {
        return VERSION;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public synchronized RuntimeContext createContext(Bundle bundle)
            throws Exception {
        // FIXME: can't work, bundle is a Bundle, context is a Map<String,
        // RuntimeContext>
        RuntimeContext ctx = contexts.get(bundle);
        if (ctx == null) {
            // hack to handle fragment bundles
            ctx = new OSGiRuntimeContext(bundle);
            contexts.put(bundle.getSymbolicName(), ctx);
            loadComponents(bundle, ctx);
        }
        return ctx;
    }

    public synchronized void destroyContext(Bundle bundle) {
        // FIXME: can't work, bundle is a Bundle, context is a Map<String,
        // RuntimeContext>
        RuntimeContext ctx = contexts.remove(bundle);
        if (ctx != null) {
            ctx.destroy();
        }
    }

    public synchronized RuntimeContext getContext(Bundle bundle) {
        // FIXME: can't work, bundle is a Bundle, context is a Map<String,
        // RuntimeContext>
        return contexts.get(bundle);
    }

    @Override
    protected void doStart() throws Exception {
        bundleContext.addFrameworkListener(this);
        loadConfig(); // load configuration if any
        loadComponents(bundleContext.getBundle(), context);
    }

    @Override
    protected void doStop() throws Exception {
        bundleContext.removeFrameworkListener(this);
        super.doStop();
        context.destroy();
    }

    protected void loadComponents(Bundle bundle, RuntimeContext ctx)
            throws Exception {
        String list = getComponentsList(bundle);
        String name = bundle.getSymbolicName();
        componentDebugLog.debug("Bundle: " + name + " components: " + list);
        if (list == null) {
            return;
        }
        StringTokenizer tok = new StringTokenizer(list, ", \t\n\r\f");
        while (tok.hasMoreTokens()) {
            String path = tok.nextToken();
            URL url = bundle.getEntry(path);
            componentDebugLog.debug("Loading component for: " + name +
                    " path: " + path + " url: " + url);
            if (url != null) {
                try {
                    ctx.deploy(url);
                } catch (Exception e) {
                    // just log error to know where is the cause of the
                    // exception
                    log.error("Error deploying resource: " + url);
                    throw e;
                }
            } else {
                String message = "Unknown component '" + path +
                        "' referenced by bundle '" + name + "'";
                log.error(message + ". Check the MANIFEST.MF");
                warnings.add(message);
            }
        }
    }

    public static String getComponentsList(Bundle bundle) {
        return (String) bundle.getHeaders().get("Nuxeo-Component");
    }

    protected void loadConfig() throws Exception {
        Environment env = Environment.getDefault();
        if (env != null) {
            componentDebugLog.info("Configuration: host application: " +
                    env.getHostApplicationName());
        } else {
            componentDebugLog.info("Configuration: no host application");
        }
        // TODO: in JBoss there is a deployer that will deploy nuxeo
        // configuration files ..
        if (env != null && !"JBoss".equals(env.getHostApplicationName())) {
            File dir = env.getConfig();
            if (dir != null) {
                log.debug(dir.getAbsolutePath());
                boolean isDirectory = dir.isDirectory();
                componentDebugLog.info("Configuration: loading from: " +
                        dir.getAbsolutePath() +
                        (isDirectory ? " (is directory)" : ""));
                if (isDirectory) {
                    for (String name : dir.list()) {
                        if (name.endsWith("-config.xml") ||
                                name.endsWith("-bundle.xml")) {
                            // TODO
                            // because of some dep bugs (regarding the
                            // deployment of demo-ds.xml)
                            // we cannot let the runtime deploy config dir at
                            // beginning...
                            // until fixing this we deploy config dir from
                            // NuxeoDeployer
                            File file = new File(dir, name);
                            componentDebugLog.info("Configuration: deploying to " +
                                    "context: " + file.toURL());
                            context.deploy(file.toURL());
                        } else if (name.endsWith(".config") ||
                                name.endsWith(".ini") ||
                                name.endsWith(".properties")) {
                            File file = new File(dir, name);
                            componentDebugLog.info("Configuration: loading " +
                                    "properties: " + name);
                            loadProperties(file);
                        } else {
                            componentDebugLog.info("Configuration: ignoring " +
                                    name);
                        }
                    }
                    return;
                }
            }
        }

        String configDir = bundleContext.getProperty(PROP_CONFIG_DIR);
        componentDebugLog.info("Configuration: " + configDir);
        if (configDir == null) {
            return;
        }

        if (configDir.contains(":/")) { // an url of a config file
            URL url = new URL(configDir);
            componentDebugLog.info("Configuration:   loading properties url: " +
                    configDir);
            loadProperties(url);
            return;
        }

        File dir = new File(configDir);
        if (dir.isDirectory()) {
            for (String name : dir.list()) {
                if (name.endsWith("-config.xml") ||
                        name.endsWith("-bundle.xml")) {
                    // TODO
                    // because of some dep bugs (regarding the deployment of
                    // demo-ds.xml)
                    // we cannot let the runtime deploy config dir at
                    // beginning...
                    // until fixing this we deploy config dir from
                    // NuxeoDeployer

                    // File file = new File(dir, name);
                    // context.deploy(file.toURL());
                    componentDebugLog.info("Configuration:   postponing config: " +
                            name);
                } else if (name.endsWith(".config") || name.endsWith(".ini") ||
                        name.endsWith(".properties")) {
                    File file = new File(dir, name);
                    componentDebugLog.info("Configuration:   loading properties: " +
                            name);
                    loadProperties(file);
                } else {
                    componentDebugLog.info("Configuration:   ignoring: " + name);
                }
            }
        } else { // a file - load it
            File file = new File(configDir);
            componentDebugLog.info("Configuration:   loading properties: " +
                    file);
            loadProperties(file);
        }
        // context.getLocalResource("OSGI-INF/RuntimeService.xml");
    }

    public void loadProperties(File file) throws IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        try {
            loadProperties(in);
        } finally {
            in.close();
        }
    }

    public void loadProperties(URL url) throws IOException {
        InputStream in = url.openStream();
        try {
            loadProperties(in);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    public void loadProperties(InputStream in) throws IOException {
        Properties props = new Properties();
        props.load(in);
        for (Entry<Object, Object> prop : props.entrySet()) {
            properties.put(prop.getKey().toString(), prop.getValue().toString());
        }
    }

    /**
     * Overrides the default method to be able to include OSGi properties.
     */
    @Override
    public String getProperty(String name, String defValue) {
        String value = properties.getProperty(name);
        if (value == null) {
            value = bundleContext.getProperty(name);
            if (value == null) {
                return defValue == null ? null : expandVars(defValue);
            }
        }
        return expandVars(value);
    }

    /* --------------- FrameworkListener API ------------------ */

    public void frameworkEvent(FrameworkEvent event) {
        if (event.getType() == FrameworkEvent.STARTED) {
            printStatusMessage();
        }
    }

    private void printStatusMessage() {
        String hr = "======================================================================";
        StringBuilder msg = new StringBuilder("Nuxeo EP Started\n"); // greppable
        msg.append(hr).append("\n= Nuxeo EP Started\n");
        if (!warnings.isEmpty()) {
            msg.append(hr).append("\n= Component Loading Errors:\n");
            for (String warning : warnings) {
                msg.append("  * ").append(warning).append('\n');
            }
        }

        Map<ComponentName, Set<ComponentName>> pendingRegistrations = manager.getPendingRegistrations();
        Collection<ComponentName> activatingRegistrations = manager.getActivatingRegistrations();
        msg.append(hr).append("\n= Component Loading Status: Pending: ").append(
                pendingRegistrations.size()).append(" / Unstarted: ").append(
                activatingRegistrations.size()).append(" / Total: ").append(
                manager.getRegistrations().size()).append('\n');
        for (Entry<ComponentName, Set<ComponentName>> e : pendingRegistrations.entrySet()) {
            msg.append("  * ").append(e.getKey()).append(" requires ").append(
                    e.getValue()).append('\n');
        }
        for (ComponentName componentName : activatingRegistrations) {
            msg.append("  - ").append(componentName).append('\n');
        }
        msg.append(hr);

        if (warnings.isEmpty() && pendingRegistrations.isEmpty() &&
                activatingRegistrations.isEmpty()) {
            log.info(msg);
        } else {
            log.error(msg);
        }
    }

    public Bundle findHostBundle(Bundle bundle) {
        String hostId = (String) bundle.getHeaders().get(
                Constants.FRAGMENT_HOST);
        componentDebugLog.info("Looking for host bundle: " +
                bundle.getSymbolicName() + " host id: " + hostId);
        if (hostId != null) {
            int p = hostId.indexOf(';');
            if (p > -1) { // remove version or other extra information if any
                hostId = hostId.substring(0, p);
            }
            RuntimeContext ctx = contexts.get(hostId);
            if (ctx != null) {
                componentDebugLog.info("Context was found for host id: " +
                        hostId);
                return ctx.getBundle();
            } else {
                componentDebugLog.info("No context found for host id: " +
                        hostId);

            }
        }
        return null;
    }

    @Override
    public File getBundleFile(Bundle bundle) {
        File file;
        String location = bundle.getLocation();
        String vendor = Framework.getProperty(Constants.FRAMEWORK_VENDOR);
        String name = bundle.getSymbolicName();

        if ("Eclipse".equals(vendor)) { // equinox framework
            componentDebugLog.debug("getBundleFile (Eclipse): " + name + "->" +
                    location);
            // update@plugins/org.eclipse.equinox.launcher_1.0.0.v20070606.jar
            // initial@reference:file:plugins/org.eclipse.update.configurator_3.2.100.v20070615.jar/
            if (location.endsWith("/")) {
                location = location.substring(0, location.length() - 1);
            }
            if (location.startsWith("update@")) {
                location = location.substring("update@".length());
            } else if (location.startsWith("initial@reference:file:")) {
                location = location.substring("initial@reference:file:".length());
            }
            file = new File(location);
        } else if (location.startsWith("file:")) { // nuxeo osgi adapter
            try {                
                file = FileUtils.urlToFile(location);
            } catch (Exception e) {
                componentDebugLog.error("getBundleFile: Unable to create " +
                        " for bundle: " + name + " as URI: " + location);
                return null;
            }
        } else { // may be a file path - this happens when using JarFileBundle
            // (for ex. in nxshell)
            try {
                file = new File(location);
            } catch (Exception e) {
                componentDebugLog.error("getBundleFile: Unable to create " +
                        " for bundle: " + name + " as file: " + location);
                return null;
            }
        }
        if ((file != null) && file.exists()) {
            componentDebugLog.debug("getBundleFile: " + name +
                    " bound to file: " + file);
            return file;
        } else {
            componentDebugLog.debug("getBundleFile: " + name +
                    " cannot bind to nonexistent file: " + file);
            return null;
        }
    }

}
