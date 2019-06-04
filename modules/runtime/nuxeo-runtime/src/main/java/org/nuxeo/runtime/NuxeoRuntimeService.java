/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.runtime;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.Environment;
import org.nuxeo.common.codec.CryptoProperties;
import org.nuxeo.common.utils.TextTemplate;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.model.URLStreamRef;
import org.nuxeo.runtime.model.impl.ComponentDescriptorReader;
import org.nuxeo.runtime.model.impl.ComponentManagerImpl;
import org.nuxeo.runtime.model.impl.RegistrationInfoImpl;
import org.nuxeo.runtime.osgi.OSGiRuntimeService;

import io.github.classgraph.ClassGraph;

/**
 * A programmatic {@link RuntimeService Nuxeo Runtime}.
 * 
 * @since 11.1
 */
public class NuxeoRuntimeService implements RuntimeService {

    protected static final Logger log = LogManager.getLogger(NuxeoRuntimeService.class);

    protected final File home;

    protected final ClassLoader classLoader;

    /** Map associating the bundle name (ie: Symbolic name) to its {@link Bundle descriptor}. */
    protected final Map<String, Bundle> bundlesInClassLoader;

    protected final CryptoProperties properties;

    /** Map associating the bundle name (ie: Symbolic name) to its {@link RuntimeContext}. */
    protected final Map<String, NuxeoRuntimeContext> bundleContexts;

    protected final ComponentManager manager;

    /** Message handler for runtime. This handler takes care to store messages with the component manager step. */
    protected final RuntimeMessageHandlerImpl messageHandler;

    protected boolean started;

    protected boolean shuttingDown;

    private NuxeoRuntimeService(Builder builder) {
        this.home = builder.home;
        this.classLoader = builder.classLoader;
        this.bundlesInClassLoader = //
                new ClassGraph().overrideClassLoaders(this.classLoader)
                                          .getClasspathFiles()
                                          .stream()
                                          .map(ManifestBundle::from)
                                          .flatMap(Optional::stream)
                                          .peek(p -> log.trace("Bundle discovered in class loader: {}", p))
                                          .collect(Collectors.toUnmodifiableMap(Bundle::getName, Function.identity()));
        this.properties = new CryptoProperties(System.getProperties());
        this.bundleContexts = new HashMap<>();
        this.manager = new ComponentManagerImpl();
        this.messageHandler = new RuntimeMessageHandlerImpl();
        this.manager.addListener(messageHandler);

        initialize(builder);
    }

    protected void initialize(Builder builder) {
        var env = new Environment(builder.home);
        Environment.setDefault(env);
        env.setServerHome(env.getHome());
        env.init();

        // deploy all bundles with nuxeo components in class loader if stated by caller
        if (builder.deployAll) {
            bundlesInClassLoader.forEach((n, b) -> {
                List<String> components = b.getDeclaredComponents();
                if (!components.isEmpty()) {
                    NuxeoRuntimeContext bundleContext = registerContext(n, i -> b);
                    deployDeclaredComponents(bundleContext);
                }
            });
        }
        // deploy given artifacts
        // do it in any cases as artifacts may contain components not deployed by 'deployAll' option
        for (String artifact : builder.artifacts) {
            String bundleName = artifact;
            int artifactSeparatorIdx = bundleName.indexOf(':');
            if (artifactSeparatorIdx > 0) {
                // component deployment
                bundleName = bundleName.substring(0, artifactSeparatorIdx);
            }
            NuxeoRuntimeContext bundleContext = registerContext(bundleName, this::lookupBundle);
            if (artifactSeparatorIdx < 0) {
                // bundle deployment
                deployDeclaredComponents(bundleContext);
            } else {
                // component deployment
                String componentPath = artifact.substring(artifactSeparatorIdx + 1);
                deployComponent(bundleContext, componentPath, new ComponentDescriptorReader());
            }
        }
        // deploy the fake started component whose studio leverages
        var ri = new RegistrationInfoImpl(OSGiRuntimeService.FRAMEWORK_STARTED_COMP); // TODO move the constant
        ri.setContext(null); // TODO replace it by an EmptyRuntimeContext
        manager.register(ri);
    }

    protected Bundle lookupBundle(String name) {
        log.trace("Lookup for bundle: {} in class loader", name);
        Bundle bundle = bundlesInClassLoader.get(name);
        if (bundle == null) {
            throw new RuntimeServiceException(
                    String.format("Bundle: %s is not present in Nuxeo Runtime class loader", name));
        }
        return bundle;
    }

    protected NuxeoRuntimeContext registerContext(String name, Function<String, Bundle> bundleGetter) {
        return bundleContexts.computeIfAbsent(name, n -> {
            Bundle bundle = bundleGetter.apply(n);
            log.trace("Register context with name: {} and bundle: {}", n, bundle);
            return new NuxeoRuntimeContext(bundle, classLoader);
        });
    }

    protected void deployDeclaredComponents(NuxeoRuntimeContext bundleContext) {
        String name = bundleContext.getBundleDescriptor().getName();

        List<String> components = bundleContext.getBundleDescriptor().getDeclaredComponents();
        if (!components.isEmpty()) {
            log.trace("Deploy bundle: {} / components: {}", name, components);
            var reader = new ComponentDescriptorReader();
            for (var component : components) {
                deployComponent(bundleContext, component, reader);
            }
        }
    }

    protected void deployComponent(NuxeoRuntimeContext bundleContext, String path, ComponentDescriptorReader reader) {
        String name = bundleContext.getBundleDescriptor().getName();
        log.trace("Deploy bundle: {} / component location: {}", name, path);

        URL url = bundleContext.getResource(path); // look into the bundle
        if (url == null) {
            throw new RuntimeServiceException(String.format("Component: %s was not found in bundle: %s", path, name));
        }
        log.trace("Load component: {} [{}]", name, url);
        try {
            // read the component and register it
            reader.createRegistrationInfo(bundleContext, new URLStreamRef(url))
                  .filter(ri -> !bundleContext.components.contains(ri.getName()))
                  .ifPresent(ri -> {
                      manager.register(ri);
                      bundleContext.components.add(ri.getName());
                  });
        } catch (IOException e) {
            // just log error to know where is the cause of the exception
            log.error("Error deploying resource: {}", url);
            throw new RuntimeServiceException("Cannot deploy: " + url, e);
        }
    }

    // start the stack
    @Override
    public synchronized void start() {
        if (started) {
            throw new RuntimeServiceException("Nuxeo Runtime is already started, stop it first");
        }
        log.info("Starting Nuxeo Runtime: {}", this);
        // 1. init the Framework to be able to get services
        Framework.initialize(this);
        // 2. start the component manager
        manager.start();
        // 3. print status
        StringBuilder msg = new StringBuilder("Nuxeo Runtime started\n");
        if (getStatusMessage(msg)) {
            log.info(msg);
        } else {
            msg.insert(0, "Nuxeo Runtime failed to start\n");
            throw new RuntimeServiceException(msg.toString());
        }
        started = true;
    }

    @Override
    public synchronized void stop() {
        if (!started) {
            throw new RuntimeServiceException("Nuxeo Runtime is not started");
        } else if (shuttingDown) {
            throw new RuntimeServiceException("Nuxeo Runtime is currently trying to stop");
        }
        log.info("Stopping Nuxeo Runtime: {}", this);
        shuttingDown = true;
        try {
            // 1. stop the component manager
            manager.stop();
            // 2. shutdown Framework
            Framework.shutdown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeServiceException("Unable to stop Nuxeo Runtime", e);
        } finally {
            shuttingDown = false;
            started = false;
        }
    }

    @Override
    public File getHome() {
        return home;
    }

    @Override
    public String getDescription() {
        return toString();
    }

    @Override
    public CryptoProperties getProperties() {
        // do not unreference properties: some methods rely on this to set variables here...
        return properties;
    }

    @Override
    public String getProperty(String name, String defValue) {
        String value = properties.getProperty(name, defValue);
        if (value == null || ("${" + name + "}").equals(value)) {
            // avoid loop, don't expand
            return value;
        }
        return expandVars(value);
    }

    @Override
    public void setProperty(String name, Object value) {
        properties.setProperty(name, value.toString());
    }

    @Override
    public ComponentInstance getComponentInstance(ComponentName name) {
        return manager.getComponent(name);
    }

    @Override
    public ComponentManager getComponentManager() {
        return manager;
    }

    /**
     * @deprecated since 11.1, context on this service is not needed anymore as main usages were to deploy contributions
     */
    @Override
    @Deprecated(since = "11.1")
    public RuntimeContext getContext() {
        throw new UnsupportedOperationException("Context can't be retrieved directly from runtime");
    }

    @Override
    public <T> T getService(Class<T> serviceClass) {
        return manager.getService(serviceClass);
    }

    @Override
    public String expandVars(String expression) {
        return new TextTemplate(properties).processText(expression);
    }

    @Override
    public File getBundleFile(org.osgi.framework.Bundle bundle) {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    public org.osgi.framework.Bundle getBundle(String symbolicName) {
        throw new UnsupportedOperationException("Deprecated");
    }

    /**
     * @since 5.5
     * @param msg summary message about all components loading status
     * @return true if there was no detected error, else return false
     */
    @Override
    public boolean getStatusMessage(StringBuilder msg) {
        // TODO rework
        List<String> warnings = messageHandler.getWarnings();
        List<String> errors = messageHandler.getErrors();
        String hr = "======================================================================";
        if (!warnings.isEmpty()) {
            msg.append(hr).append("\n= Component Loading Warnings:\n");
            for (String warning : warnings) {
                msg.append("  * ").append(warning).append('\n');
            }
        }
        if (!errors.isEmpty()) {
            msg.append(hr).append("\n= Component Loading Errors:\n");
            for (String error : errors) {
                msg.append("  * ").append(error).append('\n');
            }
        }
        Map<ComponentName, Set<ComponentName>> pendingRegistrations = manager.getPendingRegistrations();
        Map<ComponentName, Set<Extension>> missingRegistrations = manager.getMissingRegistrations();
        Collection<ComponentName> unstartedRegistrations = manager.getActivatingRegistrations();
        unstartedRegistrations.addAll(manager.getStartFailureRegistrations());
        msg.append(hr)
           .append("\n= Component Loading Status: Pending: ")
           .append(pendingRegistrations.size())
           .append(" / Missing: ")
           .append(missingRegistrations.size())
           .append(" / Unstarted: ")
           .append(unstartedRegistrations.size())
           .append(" / Total: ")
           .append(manager.getRegistrations().size())
           .append('\n');
        for (Entry<ComponentName, Set<ComponentName>> e : pendingRegistrations.entrySet()) {
            msg.append("  * ").append(e.getKey()).append(" requires ").append(e.getValue()).append('\n');
        }
        for (Entry<ComponentName, Set<Extension>> e : missingRegistrations.entrySet()) {
            msg.append("  * ")
               .append(e.getKey())
               .append(" references missing ")
               .append(e.getValue()
                        .stream()
                        .map(ext -> "target=" + ext.getTargetComponent().getName() + ";point="
                                + ext.getExtensionPoint())
                        .collect(Collectors.toList()))
               .append('\n');
        }
        for (ComponentName componentName : unstartedRegistrations) {
            msg.append("  - ").append(componentName).append('\n');
        }
        msg.append(hr);
        return (errors.isEmpty() && pendingRegistrations.isEmpty() && missingRegistrations.isEmpty()
                && unstartedRegistrations.isEmpty());
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public boolean isShuttingDown() {
        return shuttingDown;
    }

    /**
     * @since 9.10
     */
    @Override
    public RuntimeMessageHandler getMessageHandler() {
        return messageHandler;
    }

    @Override
    public void reloadProperties() {
    }

    @Override
    public String getName() {
        return "Nuxeo Runtime";
    }

    @Override
    public Version getVersion() {
        try (var is = NuxeoRuntimeService.class.getResourceAsStream("META-INF/bundle.properties")) {
            if (is != null) {
                var bundleProps = new Properties();
                bundleProps.load(is);
                String version = bundleProps.getProperty("nuxeo.runtime.version");
                return Version.parseString(version);
            }
        } catch (IOException e) {
            log.warn("Unable to read bundle properties", e);
        }
        return Version.ZERO;
    }

    @Override
    public String toString() {
        return getName() + " version: " + getVersion();
    }

    /**
     * Builder for a {@link NuxeoRuntimeService} looking for bundles present in java class path.
     */
    public static Builder builder(File home) {
        return new Builder(home);
    }

    public static Builder builder(File home, ClassLoader classLoader) {
        return new Builder(home, classLoader);
    }

    public static class Builder {

        protected final File home;

        protected final ClassLoader classLoader;

        protected boolean deployAll;

        protected List<String> artifacts = new ArrayList<>();

        /**
         * Constructor to build a {@link NuxeoRuntimeService} looking for bundles present in java class path.
         */
        public Builder(File home) {
            this(home, lookupClassLoader());
        }

        /**
         * Constructor to build a {@link NuxeoRuntimeService} looking for bundles present in given {@code classLoader}.
         */
        public Builder(File home, ClassLoader classLoader) {
            this.home = home;
            this.classLoader = classLoader;
        }

        /**
         * Deploys all bundles present in class loader.
         */
        public Builder deployAll() {
            deployAll = true;
            return this;
        }

        /**
         * Deploys the given {@code artifact} into Nuxeo Runtime.
         * <p/>
         * Artifacts could be:
         * <ul>
         * <li>{@code org.nuxeo.bundle} - a bundle name</li>
         * <li>{@code org.nuxeo.bundle:OSGI-INF/my-contrib.xml} - a component location inside a bundle</li>
         * </ul>
         */
        public Builder deploy(String artifact) {
            artifacts.add(artifact);
            return this;
        }

        public NuxeoRuntimeService initialize() {
            return new NuxeoRuntimeService(this);
        }

        public NuxeoRuntimeService start() {
            var runtime = initialize();
            runtime.start();
            return runtime;
        }

        protected static ClassLoader lookupClassLoader() {
            URL[] urls = Stream.of(System.getProperty("java.class.path").split(System.getProperty("path.separator")))
                               .map(entry -> {
                                   try {
                                       return new URL("file:" + entry);
                                   } catch (MalformedURLException e) {
                                       throw new RuntimeServiceException(e); // shouldn't happen
                                   }
                               })
                               .toArray(URL[]::new);
            return new URLClassLoader(urls);
        }
    }
}
