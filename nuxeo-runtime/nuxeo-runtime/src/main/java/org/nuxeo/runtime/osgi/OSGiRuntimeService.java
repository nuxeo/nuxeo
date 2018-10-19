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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 *     Julien Carsique
 */
package org.nuxeo.runtime.osgi;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.Environment;
import org.nuxeo.common.codec.CryptoProperties;
import org.nuxeo.common.utils.TextTemplate;
import org.nuxeo.runtime.AbstractRuntimeService;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.Version;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.model.impl.ComponentPersistence;
import org.nuxeo.runtime.model.impl.RegistrationInfoImpl;
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
public class OSGiRuntimeService extends AbstractRuntimeService implements FrameworkListener {

    public static final ComponentName FRAMEWORK_STARTED_COMP = new ComponentName("org.nuxeo.runtime.started");

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

    private static final Logger log = LogManager.getLogger(OSGiRuntimeService.class);

    private final BundleContext bundleContext;

    private final Map<String, RuntimeContext> contexts;

    private boolean appStarted = false;

    /**
     * OSGi doesn't provide a method to lookup bundles by symbolic name. This table is used to map symbolic names to
     * bundles. This map is not handling bundle versions.
     */
    final Map<String, Bundle> bundles;

    final ComponentPersistence persistence;

    public OSGiRuntimeService(BundleContext context) {
        this(new OSGiRuntimeContext(context.getBundle()), context);
    }

    public OSGiRuntimeService(OSGiRuntimeContext runtimeContext, BundleContext context) {
        super(runtimeContext);
        bundleContext = context;
        bundles = new ConcurrentHashMap<>();
        contexts = new ConcurrentHashMap<>();
        String bindAddress = context.getProperty(PROP_NUXEO_BIND_ADDRESS);
        if (bindAddress != null) {
            properties.put(PROP_NUXEO_BIND_ADDRESS, bindAddress);
        }
        String homeDir = getProperty(PROP_HOME_DIR);
        log.debug("Home directory: {}", homeDir);
        if (homeDir != null) {
            workingDir = new File(homeDir);
        } else {
            workingDir = bundleContext.getDataFile("/");
        }
        // environment may not be set by some bootstrappers (like tests) - we create it now if not yet created
        Environment env = Environment.getDefault();
        if (env == null) {
            env = new Environment(workingDir);
            Environment.setDefault(env);
            env.setServerHome(workingDir);
            env.init();
        }
        workingDir.mkdirs();
        persistence = new ComponentPersistence(this);
        log.debug("Working directory: {}", workingDir);
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
        return bundles.get(symbolicName);
    }

    public Map<String, Bundle> getBundlesMap() {
        return bundles;
    }

    public ComponentPersistence getComponentPersistence() {
        return persistence;
    }

    public synchronized RuntimeContext createContext(Bundle bundle) {
        RuntimeContext ctx = contexts.get(bundle.getSymbolicName());
        if (ctx == null) {
            // workaround to handle fragment bundles
            ctx = new OSGiRuntimeContext(bundle);
            contexts.put(bundle.getSymbolicName(), ctx);
            loadComponents(bundle, ctx);
        }
        return ctx;
    }

    public synchronized void destroyContext(Bundle bundle) {
        RuntimeContext ctx = contexts.remove(bundle.getSymbolicName());
        if (ctx != null) {
            ctx.destroy();
        }
    }

    public synchronized RuntimeContext getContext(Bundle bundle) {
        return contexts.get(bundle.getSymbolicName());
    }

    public synchronized RuntimeContext getContext(String symbolicName) {
        return contexts.get(symbolicName);
    }

    @Override
    protected void doStart() {
        bundleContext.addFrameworkListener(this);
        loadComponents(bundleContext.getBundle(), context);
    }

    @Override
    protected void doStop() {
        // do not destroy context since component manager is already shutdown
        bundleContext.removeFrameworkListener(this);
        super.doStop();
    }

    protected void loadComponents(Bundle bundle, RuntimeContext ctx) {
        String list = getComponentsList(bundle);
        String name = bundle.getSymbolicName();
        if (list == null) {
            log.debug("Bundle {} doesn't have components", name);
            return;
        }
        log.trace("Load Bundle: {} / Components: {}", name, list);
        StringTokenizer tok = new StringTokenizer(list, ", \t\n\r\f");
        while (tok.hasMoreTokens()) {
            String path = tok.nextToken();
            URL url = bundle.getEntry(path);
            if (url != null) {
                log.trace("Load component {} [{}]", name, url);
                try {
                    ctx.deploy(url);
                } catch (IOException e) {
                    // just log error to know where is the cause of the exception
                    log.error("Error deploying resource: {}", url);
                    throw new RuntimeServiceException("Cannot deploy: " + url, e);
                }
            } else {
                String message = "Unknown component '" + path + "' referenced by bundle '" + name + "'";
                log.error(message + ". Check the MANIFEST.MF");
                messageHandler.addError(message);
            }
        }
    }

    public static String getComponentsList(Bundle bundle) {
        return (String) bundle.getHeaders().get("Nuxeo-Component");
    }

    protected boolean loadConfigurationFromProvider() throws IOException {
        // TODO use a OSGi service for this.
        Iterable<URL> provider = Environment.getDefault().getConfigurationProvider();
        if (provider == null) {
            return false;
        }
        Iterator<URL> it = provider.iterator();
        List<URL> props = new ArrayList<>();
        List<URL> xmls = new ArrayList<>();
        while (it.hasNext()) {
            URL url = it.next();
            String path = url.getPath();
            if (path.endsWith("-config.xml")) {
                xmls.add(url);
            } else if (path.endsWith(".properties")) {
                props.add(url);
            }
        }
        xmls.sort(Comparator.comparing(URL::getPath));
        for (URL url : props) {
            loadProperties(url);
        }
        for (URL url : xmls) {
            context.deploy(url);
        }
        return true;
    }

    @Override
    protected void loadConfig() throws IOException {
        Environment env = Environment.getDefault();
        if (env != null) {
            log.debug("Configuration: host application: {}", env.getHostApplicationName());
        } else {
            log.warn("Configuration: no host application");
            return;
        }

        File blacklistFile = new File(env.getConfig(), "blacklist");
        if (blacklistFile.isFile()) {
            Set<String> lines = FileUtils.readLines(blacklistFile, UTF_8)
                                         .stream()
                                         .map(String::trim)
                                         .filter(line -> !line.isEmpty())
                                         .collect(Collectors.toSet());
            manager.setBlacklist(lines);
        }

        if (loadConfigurationFromProvider()) {
            return;
        }

        String configDir = bundleContext.getProperty(PROP_CONFIG_DIR);
        if (configDir != null && configDir.contains(":/")) { // an url of a config file
            URL url = new URL(configDir);
            log.debug("Configuration: loading properties from: {}", configDir);
            loadProperties(url);
            return;
        }

        // TODO: in JBoss there is a deployer that will deploy nuxeo
        // configuration files ..
        boolean isNotJBoss4 = !isJBoss4(env);

        File dir = env.getConfig();
        // File dir = new File(configDir);
        String[] names = dir.list();
        if (names != null) {
            Arrays.sort(names, String::compareToIgnoreCase);
            log.debug("Deployment order of configuration files: {}",
                    () -> Stream.of(names).reduce((n1, n2) -> n1 + "\n\t" + n2).map(n -> "\n\t" + n).orElse(""));
            for (String name : names) {
                if (name.endsWith("-config.xml") || name.endsWith("-bundle.xml")) {
                    // TODO because of some dep bugs (regarding the deployment of demo-ds.xml), we cannot let the
                    // runtime deploy config dir at beginning...
                    // until fixing this we deploy config dir from NuxeoDeployer
                    if (isNotJBoss4) {
                        File file = new File(dir, name);
                        log.trace("Configuration: deploy config component: {}", name);
                        try {
                            context.deploy(file.toURI().toURL());
                        } catch (IOException e) {
                            throw new IllegalArgumentException("Cannot load config from " + file, e);
                        }
                    }
                } else if (name.endsWith(".config") || name.endsWith(".ini") || name.endsWith(".properties")) {
                    File file = new File(dir, name);
                    log.trace("Configuration: loading properties: {}", name);
                    loadProperties(file);
                } else {
                    log.trace("Configuration: ignoring: {}", name);
                }
            }
        } else if (dir.isFile()) { // a file - load it
            log.debug("Configuration: loading properties: {}", dir);
            loadProperties(dir);
        } else {
            log.debug("Configuration: no configuration file found");
        }

        loadDefaultConfig();
    }

    @Override
    public void reloadProperties() throws IOException {
        File dir = Environment.getDefault().getConfig();
        String[] names = dir.list();
        if (names != null) {
            Arrays.sort(names, String::compareToIgnoreCase);
            CryptoProperties props = new CryptoProperties(System.getProperties());
            for (String name : names) {
                if (name.endsWith(".config") || name.endsWith(".ini") || name.endsWith(".properties")) {
                    try (FileInputStream in = new FileInputStream(new File(dir, name))) {
                        props.load(in);
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
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            loadProperties(in);
        }
    }

    public void loadProperties(URL url) throws IOException {
        try (InputStream in = url.openStream()) {
            loadProperties(in);
        }
    }

    public void loadProperties(InputStream in) throws IOException {
        properties.load(in);
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
        if (("${" + name + "}").equals(value)) {
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

        }.processText(expression);
    }

    protected void startComponents() {
        synchronized (this) {
            if (appStarted) {
                return;
            }
            appStarted = true;
        }
        try {
            persistence.loadPersistedComponents();
        } catch (RuntimeException | IOException e) {
            log.error("Failed to load persisted components", e);
        }
        // deploy a fake component that is marking the end of startup
        // XML components that needs to be deployed at the end need to put a
        // requirement
        // on this marker component
        deployFrameworkStartedComponent();
        // ============ activate and start components =======
        manager.start();
        // create a snapshot of the started components - TODO should this be optional?
        manager.snapshot();
        // ==================================================
        // print the startup message
        printStatusMessage();
    }

    /* --------------- FrameworkListener API ------------------ */

    @Override
    public void frameworkEvent(FrameworkEvent event) {
        if (event.getType() != FrameworkEvent.STARTED) {
            return;
        }
        startComponents();
    }

    private void printStatusMessage() {
        StringBuilder msg = new StringBuilder();
        msg.append("Nuxeo Platform Started\n");
        if (getStatusMessage(msg)) {
            log.info(msg);
        } else {
            log.error(msg);
            if (Boolean.getBoolean("nuxeo.start.strict")) {
                throw new IllegalStateException("Startup aborted due to previous failures (strict mode)");
            }
        }
    }

    protected void deployFrameworkStartedComponent() {
        RegistrationInfoImpl ri = new RegistrationInfoImpl(FRAMEWORK_STARTED_COMP);
        ri.setContext(context);
        // this will register any pending components that waits for the
        // framework to be started
        manager.register(ri);
    }

    public Bundle findHostBundle(Bundle bundle) {
        String hostId = (String) bundle.getHeaders().get(Constants.FRAGMENT_HOST);
        log.debug("Looking for host bundle: {} host id: {}", bundle.getSymbolicName(), hostId);
        if (hostId != null) {
            int p = hostId.indexOf(';');
            if (p > -1) { // remove version or other extra information if any
                hostId = hostId.substring(0, p);
            }
            RuntimeContext ctx = contexts.get(hostId);
            if (ctx != null) {
                log.debug("Context was found for host id: {}", hostId);
                return ctx.getBundle();
            } else {
                log.warn("No context found for host id: {}", hostId);

            }
        }
        return null;
    }

    protected File getEclipseBundleFileUsingReflection(Bundle bundle) {
        try {
            Object proxy = bundle.getClass().getMethod("getLoaderProxy").invoke(bundle);
            Object loader = proxy.getClass().getMethod("getBundleLoader").invoke(proxy);
            URL root = (URL) loader.getClass().getMethod("findResource", String.class).invoke(loader, "/");
            Field field = root.getClass().getDeclaredField("handler");
            field.setAccessible(true);
            Object handler = field.get(root);
            Field entryField = handler.getClass().getSuperclass().getDeclaredField("bundleEntry");
            entryField.setAccessible(true);
            Object entry = entryField.get(handler);
            Field fileField = entry.getClass().getDeclaredField("file");
            fileField.setAccessible(true);
            return (File) fileField.get(entry);
        } catch (ReflectiveOperationException e) {
            log.error("Cannot access to eclipse bundle system files of " + bundle.getSymbolicName());
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
            log.debug("getBundleFile (Eclipse): {}->{}", name, location);
            return getEclipseBundleFileUsingReflection(bundle);
        } else if (location.startsWith("file:")) { // nuxeo osgi adapter
            try {
                file = org.nuxeo.common.utils.FileUtils.urlToFile(location);
            } catch (MalformedURLException e) {
                log.error("getBundleFile: Unable to create file for bundle name: {} as URI: {}", name, location);
                return null;
            }
        } else { // may be a file path - this happens when using
            // JarFileBundle (for ex. in nxshell)
            file = new File(location);
        }
        if (file.exists()) {
            log.debug("getBundleFile: {} bound to file: {}", name, file);
            return file;
        } else {
            log.debug("getBundleFile: {} cannot bind to nonexistent file: {}", name, file);
            return null;
        }
    }

    public static boolean isJBoss4(Environment env) {
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

}
