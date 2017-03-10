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
package org.nuxeo.runtime.test.runner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.osgi.OSGiRuntimeService;
import org.osgi.framework.Bundle;

import com.google.common.base.Supplier;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class RuntimeDeployment {

    Set<String> bundles = new HashSet<>();

    Map<String, Set<TargetExtensions>> partialBundles = new HashMap<>();

    Map<String, Collection<String>> mainContribs = new HashMap<>();

    SetMultimap<String, String> mainIndex = Multimaps.newSetMultimap(mainContribs, new Supplier<Set<String>>() {
        @Override
        public Set<String> get() {
            return new HashSet<>();
        }
    });

    Map<String, Collection<String>> localContribs = new HashMap<>();

    SetMultimap<String, String> localIndex = Multimaps.newSetMultimap(localContribs, new Supplier<Set<String>>() {
        @Override
        public Set<String> get() {
            return new HashSet<>();
        }
    });

    protected LinkedList<RuntimeContext> contexts = new LinkedList<>();

    protected void index(Class<?> clazz) {
        AnnotationScanner scanner = FeaturesRunner.scanner;
        scanner.scan(clazz);
        List<? extends Annotation> annos = scanner.getAnnotations(clazz);
        if (annos == null) {
            return;
        }
        for (Annotation anno : annos) {
            if (anno.annotationType() == Deploy.class) {
                index((Deploy) anno);
            } else if (anno.annotationType() == LocalDeploy.class) {
                index((LocalDeploy) anno);
            } else if (anno.annotationType() == PartialDeploy.class) {
                index((PartialDeploy) anno);
            }
        }
    }

    protected void index(RunnerFeature feature) {
        index(feature.getClass());
    }

    protected void index(Method method) {
        index(method.getAnnotation(Deploy.class));
        index(method.getAnnotation(LocalDeploy.class));
    }

    protected void index(Deploy config) {
        if (config == null) {
            return;
        }
        for (String each : config.value()) {
            index(each, mainIndex);
        }
    }

    protected void index(LocalDeploy config) {
        if (config == null) {
            return;
        }
        for (String each : config.value()) {
            index(each, localIndex);
        }
    }

    /**
     * @since 9.1
     * @param config
     */
    protected void index(PartialDeploy config) {
        if (config == null) {
            return;
        }

        Set<TargetExtensions> pairs = partialBundles.computeIfAbsent(config.bundle(), key -> new HashSet<>());
        Arrays.stream(config.features()).map(c -> {
            try {
                return c.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }).forEach(pairs::add);
    }

    protected void index(Features features) {
        for (Class<?> each : features.value()) {
            index(each);
        }
    }

    protected void index(String directive, SetMultimap<String, String> contribs) {
        int sepIndex = directive.indexOf(':');
        if (sepIndex == -1) {
            bundles.add(directive);
        } else {
            String bundle = directive.substring(0, sepIndex);
            String resource = directive.substring(sepIndex + 1);
            contribs.put(bundle, resource);
        }
    }

    protected void deploy(FeaturesRunner runner, RuntimeHarness harness) {
        AssertionError errors = new AssertionError("deployment errors");
        OSGiRuntimeService runtime = (OSGiRuntimeService) harness.getContext().getRuntime();
        for (String name : bundles) {
            Bundle bundle = harness.getOSGiAdapter().getBundle(name);
            if (bundle == null) {
                try {
                    harness.deployBundle(name);
                    bundle = harness.getOSGiAdapter().getBundle(name);
                    if (bundle == null) {
                        throw new UnsupportedOperationException("Should not occur");
                    }
                } catch (Exception error) {
                    errors.addSuppressed(error);
                    continue;
                }
                contexts.add(runtime.getContext(bundle));
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
                    URL url = runner.getTargetTestResource(resource);
                    if (url == null) {
                        url = bundle.getEntry(resource);
                    }
                    if (url == null) {
                        url = runner.getTargetTestClass().getClassLoader().getResource(resource);
                    }
                    if (url == null) {
                        throw new AssertionError("Cannot find " + resource + " in " + name);
                    }
                    contexts.add(harness.deployTestContrib(name, url));
                }
            } catch (Exception error) {
                errors.addSuppressed(error);
            }
        }

        for (Map.Entry<String, String> resource : mainIndex.entries()) {
            try {
                harness.deployContrib(resource.getKey(), resource.getValue());
            } catch (Exception error) {
                errors.addSuppressed(error);
            }
        }
        for (Map.Entry<String, String> resource : localIndex.entries()) {
            try {
                contexts.add(harness.deployTestContrib(resource.getKey(), resource.getValue()));
            } catch (Exception error) {
                errors.addSuppressed(error);
            }
        }

        for (Map.Entry<String, Set<TargetExtensions>> resource : partialBundles.entrySet()) {
            try {
                harness.deployPartial(resource.getKey(), resource.getValue());
            } catch (Exception e) {
                errors.addSuppressed(e);
            }
        }

        if (errors.getSuppressed().length > 0) {
            throw errors;
        }

    }

    void undeploy() {
        AssertionError errors = new AssertionError("deployment errors");

        Iterator<RuntimeContext> it = contexts.descendingIterator();
        while (it.hasNext()) {
            RuntimeContext each = it.next();
            it.remove();
            try {
                each.destroy();
            } catch (RuntimeException error) {
                errors.addSuppressed(error);
            }
        }

        if (errors.getSuppressed().length > 0) {
            throw errors;
        }
    }

    public static RuntimeDeployment onTest(FeaturesRunner runner) {
        RuntimeDeployment deployment = new RuntimeDeployment();
        deployment.index(runner.getDescription().getTestClass());
        for (RunnerFeature each : runner.getFeatures()) {
            deployment.index(each);
        }
        return deployment;
    }

    public static MethodRule onMethod() {
        return new OnMethod();
    }

    protected static class OnMethod implements MethodRule {

        @Inject
        protected FeaturesRunner runner;

        @Override
        public Statement apply(Statement base, FrameworkMethod method, Object target) {
            RuntimeDeployment deployment = new RuntimeDeployment();
            deployment.index(method.getMethod());
            return deployment.onStatement(runner, runner.getFeature(RuntimeFeature.class).harness, base);
        }

    }

    protected Statement onStatement(FeaturesRunner runner, RuntimeHarness harness, Statement base) {
        return new DeploymentStatement(runner, harness, base);
    }

    protected class DeploymentStatement extends Statement {

        protected final FeaturesRunner runner;

        protected final RuntimeHarness harness;

        protected final Statement base;

        public DeploymentStatement(FeaturesRunner runner, RuntimeHarness harness, Statement base) {
            this.runner = runner;
            this.harness = harness;
            this.base = base;
        }

        @Override
        public void evaluate() throws Throwable {
            deploy(runner, harness);
            try {
                base.evaluate();
            } finally {
                undeploy();
            }
        }

    }

}
