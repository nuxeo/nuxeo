/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.runtime.test.runner;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.URLStreamHandlerFactoryInstaller;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.osgi.framework.Bundle;

import com.google.common.base.Supplier;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.inject.Binder;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class RuntimeFeature extends SimpleFeature {

    private static final Log log = LogFactory.getLog(RuntimeFeature.class);

    protected RuntimeHarness harness;

    protected final DeploymentSet deploy;

    /**
     * Providers contributed by other features to override the default service
     * provider used for a nuxeo service.
     */
    protected final Map<Class<?>, ServiceProvider<?>> serviceProviders;

    public RuntimeFeature() {
        deploy = new DeploymentSet();
        serviceProviders = new HashMap<Class<?>, ServiceProvider<?>>();
    }

    public <T> void addServiceProvider(ServiceProvider<T> provider) {
        serviceProviders.put(provider.getServiceClass(), provider);
    }

    public RuntimeHarness getHarness() {
        return harness;
    }

    public DeploymentSet deployments() {
        return deploy;
    }

    private void scanDeployments(FeaturesRunner runner) {
        List<RunnerFeature> features = runner.getFeatures();
        if (features == null) {
            throw new IllegalStateException(
                    "Cannot call scanDeployments until features are not loaded");
        }
        for (RunnerFeature feature : features) {
            deploy.load(FeaturesRunner.getScanner(), feature.getClass());
        }
        // load deployments from class to run
        deploy.load(FeaturesRunner.getScanner(),
                runner.getTestClass().getJavaClass());
    }

    public String[] getDeployments() {
        return deploy.getDeployments().toArray(
                new String[deploy.getDeployments().size()]);
    }

    public String[] getLocalDeployments() {
        return deploy.getLocalDeployments().toArray(
                new String[deploy.getLocalDeployments().size()]);
    }

    protected void indexBundleResources(FeaturesRunner runner, Set<String> bundles,
            SetMultimap<String, String> resources, String[] directives)
            throws IOException {
        for (String directive : directives) {
            int sepIndex = directive.indexOf(':');
            if (sepIndex == -1) {
                bundles.add(directive);
            } else {
                String bundle = directive.substring(0, sepIndex);
                String resource = directive.substring(sepIndex + 1);
                resources.put(bundle, resource);
            }
        }
    }

    /**
     * Deploys bundles specified in the @Bundles annotation.
     */
    protected void deployTestClassBundles(FeaturesRunner runner) throws Exception {
        Set<String> bundles = new HashSet<String>();
        Map<String, Collection<String>> mainDeployments = new HashMap<>();
        SetMultimap<String, String> mainIndex = Multimaps.newSetMultimap(mainDeployments,
                new Supplier<Set<String>>() {
                    @Override
                    public Set<String> get() {
                        return new HashSet<String>();
                    }
                });
        Map<String, Collection<String>> localDeployments = new HashMap<>();
        SetMultimap<String, String> localIndex = Multimaps.newSetMultimap(localDeployments,
                new Supplier<Set<String>>() {
                    @Override
                    public Set<String> get() {
                        return new HashSet<String>();
                    }
                });
        indexBundleResources(runner, bundles, mainIndex, getDeployments());
        indexBundleResources(runner, bundles, localIndex, getLocalDeployments());
        AssertionError errors = new AssertionError("cannot deploy components");
        for (String name : bundles) {
            Bundle bundle = null;
            try {
                harness.deployBundle(name);
                bundle = harness.getOSGiAdapter().getBundle(name);
            } catch (Exception error) {
                errors.addSuppressed(error);
                continue;
            }
            try {
                // deploy bundle contribs
                for (String resource : mainIndex.removeAll(name)) {
                    try {
                        harness.deployContrib(name, resource);
                    } catch (Exception error) {
                        errors.addSuppressed(error);
                    }
                }
                // deploy local contribs
                for (String resource : localIndex.removeAll(name)) {
                    URL url = runner.getTargetTestResource(name);
                    if (url == null) {
                        url = bundle.getEntry(resource);
                    }
                    if (url == null) {
                        url = runner.getTargetTestClass().getClassLoader().getResource(
                                resource);
                    }
                    if (url == null) {
                        throw new AssertionError("Cannot find " + resource
                                + " in " + name);
                    }
                    harness.deployTestContrib(name,  url);
                }
            } catch (Exception error) {
                errors.addSuppressed(error);
            }
        }

        for (Map.Entry<String,String> resource:mainIndex.entries()) {
            try {
                harness.deployContrib(resource.getKey(), resource.getValue());
            } catch (Exception error) {
                errors.addSuppressed(error);
            }
        }
        for (Map.Entry<String,String> resource:localIndex.entries()) {
            try {
                harness.deployTestContrib(resource.getKey(),
                        resource.getValue());
            } catch (Exception error) {
                errors.addSuppressed(error);
            }
        }

        if (errors.getSuppressed().length > 0) {
            throw errors;
        }
    }

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {
        harness = new NXRuntimeTestCase(runner.getTargetTestClass());
        scanDeployments(runner);
    }

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        // Starts Nuxeo Runtime
        if (!harness.isStarted()) {
            harness.start();
        }
        // Deploy bundles
        deployTestClassBundles(runner);
    }

    @Override
    public void beforeRun(FeaturesRunner runner) throws Exception {
        harness.fireFrameworkStarted();
    }

    @Override
    public void stop(FeaturesRunner runner) throws Exception {
        try {
            // Stops the harness if needed
            if (harness.isStarted()) {
                // TODO NXP-10915 should undeploy test class bundles
                harness.stop();
                // harness = null;
            }
        } finally {
            cleanupClassLoader();
        }
    }

    protected void cleanupClassLoader() {
        URLStreamHandlerFactoryInstaller.resetURLStreamHandlers();
    }

    protected void resetStaticField(Class<?> clazz, String name) {
        try {
            Field f = clazz.getDeclaredField(name);
            f.setAccessible(true);
            f.set(null, null);
        } catch (NoSuchFieldException | SecurityException
                | IllegalArgumentException | IllegalAccessException e) {
            log.error("Cannot reset field " + clazz.getName() + "." + name, e);
        }
    }

    // TODO this is not ok. we should not force 2 modules layers - we should be
    // able to load any number of module layers.
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        for (String svc : Framework.getRuntime().getComponentManager().getServices()) {
            try {
                Class clazz = Thread.currentThread().getContextClassLoader().loadClass(
                        svc);
                ServiceProvider provider = serviceProviders.get(clazz);
                if (provider == null) {
                    provider = new ServiceProvider(clazz);
                }
                bind0(binder, clazz, provider);
            } catch (Exception e) {
                throw new RuntimeException("Failed to bind service: " + svc, e);
            }
        }
        binder.bind(RuntimeHarness.class).toInstance(getHarness());
        // binder.bind(FeaturesRunner.class).toInstance(runner);
        // binder.bind(NuxeoRunner.class).toInstance(runner);
    }

    protected <T> void bind0(Binder binder, Class<T> type,
            ServiceProvider<T> provider) {
        binder.bind(type).toProvider(provider).in(provider.getScope());
    }

}
