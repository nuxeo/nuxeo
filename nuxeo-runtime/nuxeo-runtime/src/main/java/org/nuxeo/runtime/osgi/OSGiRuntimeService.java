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
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.runtime.AbstractRuntimeService;
import org.nuxeo.runtime.Version;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.RuntimeContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
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

    private final BundleContext bundleContext;

    private final Map<Bundle, RuntimeContext> contexts;

    public OSGiRuntimeService(BundleContext context) {
        super(new OSGiRuntimeContext(context.getBundle()));
        bundleContext = context;
        contexts = new HashMap<Bundle, RuntimeContext>();
        String bindAddress = context.getProperty(PROP_NUXEO_BIND_ADDRESS);
        if (bindAddress != null) {
            properties.put(PROP_NUXEO_BIND_ADDRESS, bindAddress);
        }
        String homeDir = getProperty(PROP_HOME_DIR);
        if (homeDir != null) {
            workingDir = new File(homeDir);
        } else {
            workingDir = bundleContext.getDataFile("/");
        }
        workingDir.mkdirs();
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
        RuntimeContext ctx = contexts.get(bundle);
        if (ctx == null) {
            // hack to handle fragment bundles
            ctx = new OSGiRuntimeContext(bundle);
            contexts.put(bundle, ctx);
            loadComponents(bundle, ctx);
        }
        return ctx;
    }

    public synchronized void destroyContext(Bundle bundle) {
        RuntimeContext ctx = contexts.remove(bundle);
        if (ctx != null) {
            ctx.destroy();
        }
    }

    public synchronized RuntimeContext getContext(Bundle bundle) {
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
        if (list == null) {
            return;
        }
        StringTokenizer tok = new StringTokenizer(list, ", \t\n\r\f");
        while (tok.hasMoreTokens()) {
            String desc = tok.nextToken();
            URL url = bundle.getEntry(desc);
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
                String message = "Unknown component '" + desc
                        + "' referenced by bundle '" + bundle.getSymbolicName()
                        + "'";
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
        // TODO: in JBoss there is a deployer that will deploy nuxeo configuration files ..
        if (env != null && !"JBoss".equals(env.getHostApplicationName())) {
            File dir = env.getConfig() ;
            if (dir != null) {
                System.out.println(dir.getAbsolutePath());
                if (dir.isDirectory()) {
                    for (String name : dir.list()) {
                        if (name.endsWith("-config.xml") || name.endsWith("-bundle.xml")) {
                            //TODO
                            // because of some dep bugs (regarding the deployment of demo-ds.xml)
                            // we cannot let the runtime deploy config dir at beginning...
                            // until fixing this we deploy config dir from
                            // NuxeoDeployer
                            File file = new File(dir, name);
                            context.deploy(file.toURL());
                        } else if (name.endsWith(".config")
                                || name.endsWith(".ini")
                                || name.endsWith(".properties")) {
                            File file = new File(dir, name);
                            loadProperties(file);
                        }
                    }
                    return;
                }
            }
        }

        String configDir = bundleContext.getProperty(PROP_CONFIG_DIR);
        if (configDir == null) {
            return;
        }

        if (configDir.contains(":/")) { // an url of a config file
            URL url = new URL(configDir);
            loadProperties(url);
            return;
        }

        File dir = new File(configDir);
        if (dir.isDirectory()) {
            for (String name : dir.list()) {
                if (name.endsWith("-config.xml")
                        || name.endsWith("-bundle.xml")) {
                    // TODO
                    // because of some dep bugs (regarding the deployment of
                    // demo-ds.xml)
                    // we cannot let the runtime deploy config dir at
                    // beginning...
                    // until fixing this we deploy config dir from
                    // NuxeoDeployer

                    // File file = new File(dir, name);
                    // context.deploy(file.toURL());
                } else if (name.endsWith(".config") || name.endsWith(".ini")
                        || name.endsWith(".properties")) {
                    File file = new File(dir, name);
                    loadProperties(file);
                }
            }
        } else { // a file - load it
            File file = new File(configDir);
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
        for (Map.Entry<Object, Object> prop : props.entrySet()) {
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

        if (warnings.isEmpty() && pendingRegistrations.isEmpty()
                && activatingRegistrations.isEmpty()) {
            log.info(msg);
        } else {
            log.error(msg);
        }
    }

}
