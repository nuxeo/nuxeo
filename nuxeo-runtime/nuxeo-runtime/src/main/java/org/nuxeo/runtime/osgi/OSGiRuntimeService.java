/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 *     Julien Carsique
 */

package org.nuxeo.runtime.osgi;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.TextTemplate;
import org.nuxeo.common.utils.Vars;
import org.nuxeo.runtime.Version;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.RuntimeModelException;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.model.impl.AbstractRuntimeContext;
import org.nuxeo.runtime.model.impl.AbstractRuntimeService;
import org.nuxeo.runtime.model.impl.ComponentPersistence;
import org.nuxeo.runtime.model.impl.RegistrationInfoImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;

import sun.misc.CompoundEnumeration;

/**
 * The default implementation of NXRuntime over an OSGi compatible environment.
 *
 * @author Bogdan Stefanescu
 * @author Florent Guillaume
 */
public class OSGiRuntimeService extends AbstractRuntimeService implements
        FrameworkListener {

    public static final ComponentName FRAMEWORK_STARTED_COMP = new ComponentName(
            "org.nuxeo.runtime.started");

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

    private final Log log = LogFactory.getLog(OSGiRuntimeService.class);

    protected final BundleContext bundleContext;

    protected boolean appStarted = false;

    protected final OSGiComponentLoader componentLoader = new OSGiComponentLoader(
            this);

    /**
     * OSGi doesn't provide a method to lookup bundles by symbolic name. This
     * table is used to map symbolic names to bundles. This map is not handling
     * bundle versions.
     */
    protected final Map<String, Bundle> bundlesByName = new ConcurrentHashMap<String, Bundle>();

    protected final ComponentPersistence persistence;

    protected static Map<String, String> toMap(Dictionary<?, ?> dict) {
        if (dict == null) {
            return Collections.emptyMap();
        }
        Map<String, String> map = new HashMap<String, String>();
        Enumeration<?> keys = dict.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            map.put(key.toString(), dict.get(key).toString());
        }
        return map;
    }

    public OSGiRuntimeService(BundleContext context,
            Dictionary<String, ?> config) {
        this(new OSGiRuntimeContext(context.getBundle()), context,
                toMap(config));
    }

    public OSGiRuntimeService(OSGiRuntimeContext runtimeContext,
            BundleContext context, Map<String, String> props) {
        super(runtimeContext, props);
        bundleContext = context;
        String bindAddress = context.getProperty(PROP_NUXEO_BIND_ADDRESS);
        if (bindAddress != null) {
            properties.put(PROP_NUXEO_BIND_ADDRESS, bindAddress);
        }
        String homeDir = getProperty(PROP_HOME_DIR);
        log.debug("Home directory: " + homeDir);
        if (homeDir != null) {
            workingDir = new File(homeDir);
        } else {
            workingDir = bundleContext.getDataFile("/");
        }
        // environment may not be set by some bootstrappers (like tests) - we
        // create it now if not yet created
        Environment env = Environment.getDefault();
        if (env == null) {
            Environment.setDefault(new Environment(workingDir));
        }
        workingDir.mkdirs();
        persistence = new ComponentPersistence(this);
        log.debug("Working directory: " + workingDir);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Version getVersion() {
        return VERSION;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    @Override
    public Bundle getBundle(String symbolicName) {
        return bundlesByName.get(symbolicName);
    }

    public Map<String, Bundle> getBundlesMap() {
        return bundlesByName;
    }

    public ComponentPersistence getComponentPersistence() {
        return persistence;
    }

    public synchronized RuntimeContext createContext(Bundle bundle) throws RuntimeModelException {
        OSGiRuntimeContext ctx;
        if (bundle.equals(runtimeContext.getBundle())) {
            ctx = (OSGiRuntimeContext) runtimeContext;
        } else {
            ctx = new OSGiRuntimeContext(bundle);
        }
        contextsByName.put(bundle.getSymbolicName(), ctx);
        ctx.setRegistered(this);
        if (ctx.isResolved() && (bundle.getState() & Bundle.ACTIVE) != 0) {
            try {
                ctx.setActivated();
            } catch (Exception e) {
                throw new IllegalStateException("Cannot activate " + bundle, e);
            }
        }
        return ctx;
    }

    public synchronized void activateContext(Bundle bundle) throws Exception {
        String symbolicName = bundle.getSymbolicName();
        if (!contextsByName.containsKey(symbolicName)) {
            throw new IllegalStateException(
                    "Trying to activate missing bundle context, check deps ("
                            + symbolicName + ")");
        }
        OSGiRuntimeContext context = getContext(bundle);
        if (!context.isResolved()) {
            for (AbstractRuntimeContext req:context.getRequiredPendingContexts()) {
                if (!req.isResolved()) {
                    continue;
                }
                if ((req.getBundle().getState() & (Bundle.STARTING|Bundle.ACTIVE)) == 0) {
                    req.getBundle().start();
                }
                if (!req.isActivated()) {
                    req.setActivated();
                }
            }
        }
        context.setActivated();
    }

    public synchronized void destroyContext(Bundle bundle) {
        RuntimeContext ctx = contextsByName.remove(bundle.getSymbolicName());
        if (ctx != null) {
            ctx.destroy();
        }
    }

    public synchronized OSGiRuntimeContext getContext(Bundle bundle) {
        return (OSGiRuntimeContext)contextsByName.get(bundle.getSymbolicName());
    }

    @Override
    public synchronized OSGiRuntimeContext getContext(String symbolicName) {
        Bundle bundle = getBundle(symbolicName);
        bundle = findHostBundle(bundle);
        return (OSGiRuntimeContext)contextsByName.get(symbolicName);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        bundleContext.addFrameworkListener(this);
        componentLoader.start();
        loadConfig(); // load configuration if any
    }

    @Override
    protected void doStop() throws Exception {
        bundleContext.removeFrameworkListener(this);
        componentLoader.stop();
        super.doStop();
        runtimeContext.destroy();
    }

    public static String getComponentsList(Bundle bundle) {
        return bundle.getHeaders().get("Nuxeo-Component");
    }

    protected void loadConfigurationFromProvider(boolean isJBoss4,
            RuntimeContext context, Iterable<URL> provider) throws Exception {
        // TODO use a OSGi service for this.

        Iterator<URL> it = provider.iterator();
        ArrayList<URL> props = new ArrayList<URL>();
        ArrayList<URL> xmls = new ArrayList<URL>();
        while (it.hasNext()) {
            URL url = it.next();
            String path = url.getPath();
            if (path.endsWith("-config.xml")) {
                xmls.add(url);
            } else if (path.endsWith(".properties")) {
                props.add(url);
            }
        }
        Comparator<URL> comp = new Comparator<URL>() {
            @Override
            public int compare(URL o1, URL o2) {
                return o1.getPath().compareTo(o2.getPath());
            }
        };
        Collections.sort(xmls, comp);
        for (URL url : props) {
            loadProperties(url);
        }
        // TODO: in JBoss there is a deployer that will deploy nuxeo
        // configuration files ..
        for (URL url : xmls) {
            if (!isJBoss4) {
                log.debug("Configuration: deploy config component: " + url);
                try {
                    context.deploy(url);
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                            "Cannot load config from " + url, e);
                }
            }
        }
    }

    protected void loadConfig() throws Exception {
        Environment env = Environment.getDefault();
        if (env != null) {
            log.info("Configuration: host application: "
                    + env.getHostApplicationName());
        } else {
            log.warn("Configuration: no host application");
            return;
        }

        File blacklistFile = new File(env.getConfig(), "blacklist");
        if (blacklistFile.isFile()) {
            List<String> lines = FileUtils.readLines(blacklistFile);
            Set<String> blacklist = new HashSet<String>();
            for (String line : lines) {
                line = line.trim();
                if (line.length() > 0) {
                    blacklist.add(line);
                }
            }
            manager.setBlacklist(new HashSet<String>(lines));
        }

        String configDir = bundleContext.getProperty(PROP_CONFIG_DIR);
        if (configDir != null && configDir.contains(":/")) { // an url of a
            // config file
            log.debug("Configuration: " + configDir);
            URL url = new URL(configDir);
            log.debug("Configuration:   loading properties url: " + configDir);
            loadProperties(url);
            return;
        }

        Iterable<URL> provider = Environment.getDefault().getConfigurationProvider();
        RuntimeContext context = null;

        File dir = env.getConfig();
        if (dir.isDirectory()) {
            Bundle config = bundleContext.installBundle(dir.getPath());
            context = contextsByName.get(config.getSymbolicName());
            if (provider == null) {
                provider = new BundleConfigurationProvider(config);
            }
        }

        if (provider != null) {
            loadConfigurationFromProvider(isJBoss4(env), context, provider);
            return;
        }

        if (dir.isFile()) { // a file - load it
            log.debug("Configuration: loading properties: " + dir);
            loadProperties(dir);
        } else {
            log.debug("Configuration: no configuration file found");
        }

        loadDefaultConfig();
    }

    protected void printDeploymentOrderInfo(String[] fileNames) {
        if (log.isDebugEnabled()) {
            StringBuilder buf = new StringBuilder();
            for (String fileName : fileNames) {
                buf.append("\n\t" + fileName);
            }
            log.debug("Deployment order of configuration files: "
                    + buf.toString());
        }
    }

    @Override
    public void reloadProperties() throws Exception {
        File dir = Environment.getDefault().getConfig();
        String[] names = dir.list();
        if (names != null) {
            Arrays.sort(names, new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    return o1.compareToIgnoreCase(o2);
                }
            });
            Properties props = new Properties();
            for (String name : names) {
                if (name.endsWith(".config") || name.endsWith(".ini")
                        || name.endsWith(".properties")) {
                    FileInputStream in = new FileInputStream(
                            new File(dir, name));
                    try {
                        props.load(in);
                    } finally {
                        in.close();
                    }

                }
            }
            // replace the current runtime properties
            properties = props;
        }
    }

    /**
     * Loads default properties.
     * <p>
     * Used for backward compatibility when adding new mandatory properties
     * </p>
     */
    protected void loadDefaultConfig() {
        String varName = "org.nuxeo.ecm.contextPath";
        if (Framework.getProperty(varName) == null) {
            properties.setProperty(varName, "/nuxeo");
        }
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
            properties.put(prop.getKey().toString(),
                    Vars.expand(prop.getValue().toString(), properties));
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
        if (value.startsWith("$") && value.equals("${" + name + "}")) {
            // avoid loop, don't expand
            return value;
        }
        return expandVars(value);
    }

    /**
     * Overrides the default method to be able to include OSGi properties.
     */
    @Override
    public String expandVars(String expression) {
        return new TextTemplate(getProperties()) {

            @Override
            public String getVariable(String name) {
                String value = super.getVariable(name);
                if (value == null) {
                    value = bundleContext.getProperty(name);
                }
                return value;
            }

        }.process(expression);
    }

    protected void notifyComponentsOnStarted() {
        List<RegistrationInfo> ris = new ArrayList<RegistrationInfo>(
                manager.getRegistrations());
        Collections.sort(ris, new RIApplicationStartedComparator());
        for (RegistrationInfo ri : ris) {
            try {
                ri.notifyApplicationStarted();
            } catch (Exception e) {
                log.error("Failed to notify component '" + ri.getName()
                        + "' on application started", e);
            }
        }
    }

    protected static class CompoundEnumerationBuilder {

        protected final ArrayList<Enumeration<URL>> collected = new ArrayList<Enumeration<URL>>();

        public CompoundEnumerationBuilder add(Enumeration<URL> e) {
            collected.add(e);
            return this;
        }

        public Enumeration<URL> build() {
            return new CompoundEnumeration<URL>(
                    collected.toArray(new Enumeration[collected.size()]));
        }

    }

    protected static class BundleConfigurationProvider implements Iterable<URL> {
        protected final Bundle bundle;

        protected BundleConfigurationProvider(Bundle bundle) {
            this.bundle = bundle;
        }

        @Override
        public Iterator<URL> iterator() {
            CompoundEnumerationBuilder builder = new CompoundEnumerationBuilder();
            builder.add(bundle.findEntries("/", "*.properties", true));
            builder.add(bundle.findEntries("/", "*-config.xml", true));
            builder.add(bundle.findEntries("/", "*-bundle.xml", true));
            final Enumeration<URL> entries = builder.build();
            return new Iterator<URL>() {

                @Override
                public boolean hasNext() {
                    return entries.hasMoreElements();
                }

                @Override
                public URL next() {
                    return entries.nextElement();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }

            };

        }
    }

    protected static class RIApplicationStartedComparator implements
            Comparator<RegistrationInfo> {
        @Override
        public int compare(RegistrationInfo r1, RegistrationInfo r2) {
            int cmp = r1.getApplicationStartedOrder()
                    - r2.getApplicationStartedOrder();
            if (cmp == 0) {
                // fallback on name order, to be deterministic
                cmp = r1.getName().getName().compareTo(r2.getName().getName());
            }
            return cmp;
        }
    }

    public void fireApplicationStarted() throws RuntimeModelException {
        synchronized (this) {
            if (appStarted) {
                return;
            }
            appStarted = true;
        }
        RuntimeModelException.CompoundBuilder errors = new RuntimeModelException.CompoundBuilder();
        try {
            persistence.loadPersistedComponents();
        } catch (RuntimeModelException e) {
            errors.add(e);
        }
        // deploy a fake component that is marking the end of startup
        // XML components that needs to be deployed at the end need to put a
        // requirement
        // on this marker component
        try {
            deployFrameworkStartedComponent();
        } catch (RuntimeModelException e) {
            errors.add(e);
        }
        errors.throwOnError();
        notifyComponentsOnStarted();
        // print the startup message
        printStatusMessage();
    }

    /* --------------- FrameworkListener API ------------------ */

    @Override
    public void frameworkEvent(FrameworkEvent event) {
        if (event.getType() == FrameworkEvent.STARTED) {
            try {
                fireApplicationStarted();
            } catch (RuntimeModelException e) {
                Framework.handleDevError(e);
            }
        }
    }

    private void printStatusMessage() {
        StringBuilder msg = new StringBuilder();
        msg.append("Nuxeo EP Started\n"); // greppable
        if (getStatusMessage(msg)) {
            log.info(msg);
        } else {
            log.error(msg);
        }
    }

    protected void deployFrameworkStartedComponent() throws RuntimeModelException {
        RegistrationInfoImpl ri = new RegistrationInfoImpl(
                FRAMEWORK_STARTED_COMP);
        ri.setContext(runtimeContext);
        // this will register any pending components that waits for the
        // framework to be started
        manager.register(ri);
    }

    public Bundle findHostBundle(Bundle bundle) {
        String hostId = bundle.getHeaders().get(Constants.FRAGMENT_HOST);
        log.debug("Looking for host bundle: " + bundle.getSymbolicName()
                + " host id: " + hostId);
        if (hostId == null) {
            return bundle;
        }
        int p = hostId.indexOf(';');
        if (p > -1) { // remove version or other extra information if any
            hostId = hostId.substring(0, p);
        }
        return bundlesByName.get(hostId);
    }

    protected File getEclipseBundleFileUsingReflection(Bundle bundle) {
        try {
            Object proxy = bundle.getClass().getMethod("getLoaderProxy").invoke(
                    bundle);
            Object loader = proxy.getClass().getMethod("getBundleLoader").invoke(
                    proxy);
            URL root = (URL) loader.getClass().getMethod("findResource",
                    String.class).invoke(loader, "/");
            Field field = root.getClass().getDeclaredField("handler");
            field.setAccessible(true);
            Object handler = field.get(root);
            Field entryField = handler.getClass().getSuperclass().getDeclaredField(
                    "bundleEntry");
            entryField.setAccessible(true);
            Object entry = entryField.get(handler);
            Field fileField = entry.getClass().getDeclaredField("file");
            fileField.setAccessible(true);
            return (File) fileField.get(entry);
        } catch (Throwable e) {
            log.error("Cannot access to eclipse bundle system files of "
                    + bundle.getSymbolicName());
            return null;
        }
    }

    @Override
    public File getBundleFile(Bundle bundle) {
        File file;
        String location = bundle.getLocation();
        String vendor = Framework.getProperty(Constants.FRAMEWORK_VENDOR);
        String name = bundle.getSymbolicName();

        if ("Eclipse".equals(vendor)) { // equinox framework
            log.debug("getBundleFile (Eclipse): " + name + "->" + location);
            return getEclipseBundleFileUsingReflection(bundle);
        } else if (location.startsWith("file:")) { // nuxeo osgi adapter
            try {
                file = FileUtils.urlToFile(location);
            } catch (Exception e) {
                log.error("getBundleFile: Unable to create " + " for bundle: "
                        + name + " as URI: " + location);
                return null;
            }
        } else { // may be a file path - this happens when using
            // JarFileBundle (for ex. in nxshell)
            try {
                file = new File(location);
            } catch (Exception e) {
                log.error("getBundleFile: Unable to create " + " for bundle: "
                        + name + " as file: " + location);
                return null;
            }
        }
        if (file != null && file.exists()) {
            log.debug("getBundleFile: " + name + " bound to file: " + file);
            return file;
        } else {
            log.debug("getBundleFile: " + name
                    + " cannot bind to nonexistent file: " + file);
            return null;
        }
    }

    public static final boolean isJBoss4(Environment env) {
        if (env == null) {
            return false;
        }
        String hn = env.getHostApplicationName();
        String hv = env.getHostApplicationVersion();
        if (hn == null || hv == null) {
            return false;
        }
        return "JBoss".equals(hn) && hv.startsWith("4");
    }

    protected void addWarning(String message) {
        warnings.add(message);
    }

    public void installBundle(Bundle bundle) throws RuntimeModelException {
        bundlesByName.put(bundle.getSymbolicName(), bundle);
        createContext(bundle);
    }

}
