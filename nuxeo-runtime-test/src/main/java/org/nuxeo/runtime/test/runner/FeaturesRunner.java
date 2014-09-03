/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import java.lang.annotation.Annotation;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.MDC;
import org.junit.Ignore;
import org.junit.internal.AssumptionViolatedException;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.nuxeo.runtime.test.TargetResourceLocator;

import com.google.common.collect.Lists;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * A Test Case runner that can be extended through features and provide
 * injection though Guice.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FeaturesRunner extends BlockJUnit4ClassRunner {

    protected static final AnnotationScanner scanner = new AnnotationScanner();

    /**
     * Guice injector.
     */
    protected Injector injector;

    protected final Set<Class<? extends RunnerFeature>> featureClasses = new LinkedHashSet<>();

    protected final List<RunnerFeature> features = new ArrayList<>();

    protected final TargetResourceLocator locator;

    public static AnnotationScanner getScanner() {
        return scanner;
    }

    // not the most efficient to recompute this all the time
    // but it doesn't matter here
    public static <T> List<T> reversed(List<T> list) {
        List<T> reversed = new ArrayList<T>(list);
        Collections.reverse(reversed);
        return reversed;
    }

    public FeaturesRunner(Class<?> classToRun) throws InitializationError {
        super(classToRun);
        locator = new TargetResourceLocator(classToRun);
        try {
            loadFeatures(getTargetTestClass());
        } catch (Throwable t) {
            throw new InitializationError(Collections.singletonList(t));
        }
    }

    public Class<?> getTargetTestClass() {
        return super.getTestClass().getJavaClass();
    }

    public Path getTargetTestBasepath() {
        return locator.getBasepath();
    }

    public URL getTargetTestResource(String name) throws IOException {
        return locator.getTargetTestResource(name);
    }

    protected void loadFeature(HashSet<Class<?>> cycles,
            Set<Class<? extends RunnerFeature>> features,
            Class<? extends RunnerFeature> clazz) throws Exception {
        if (features.contains(clazz)) {
            return;
        }
        if (cycles.contains(clazz)) {
            throw new IllegalStateException(
                    "Cycle detected in features dependencies of " + clazz);
        }
        cycles.add(clazz);
        scanner.scan(clazz);
        // load required features from annotation
        List<Features> annos = scanner.getAnnotations(clazz, Features.class);
        if (annos != null) {
            for (Features anno : annos) {
                for (Class<? extends RunnerFeature> cl : anno.value()) {
                    if (!features.contains(cl)) {
                        loadFeature(cycles, features, cl);
                    }
                }
            }
        }
        features.add(clazz); // add at the end to ensure requirements are added
                             // first
    }

    public void loadFeatures(Class<?> classToRun) throws Exception {
        scanner.scan(classToRun);
        // load required features from annotation
        List<Features> annos = scanner.getAnnotations(classToRun,
                Features.class);
        if (annos != null) {
            for (Features anno : annos) {
                for (Class<? extends RunnerFeature> cl : anno.value()) {
                    if (!features.contains(cl)) {
                        loadFeature(new HashSet<Class<?>>(), featureClasses, cl);
                    }
                }
            }
        }
    }

    public <T extends RunnerFeature> T getFeature(Class<T> type) {
        for (RunnerFeature rf : features) {
            if (rf.getClass() == type) {
                return type.cast(rf);
            }
        }
        return null;
    }

    public List<RunnerFeature> getFeatures() {
        return features;
    }

    /**
     * @since 5.6
     */
    public <T extends Annotation> T getConfig(Class<T> type) {
        List<T> configs = new ArrayList<>();
        T annotation = scanner.getAnnotation(getTargetTestClass(), type);
        if (annotation != null) {
            configs.add(annotation);
        }
        for (RunnerFeature feature : Lists.reverse(features)) {
            annotation = scanner.getAnnotation(feature.getClass(), type);
            if (annotation != null) {
                configs.add(annotation);
            }
        }
        return Defaults.of(type, configs);
    }

    /**
     * Get the annotation on the test method, if no annotation has been found,
     * get the annotation from the test class (See {@link #getConfig(Class)})
     *
     * @since 5.7
     */
    public <T extends Annotation> T getConfig(FrameworkMethod method,
            Class<T> type) {
        T config = method.getAnnotation(type);
        if (config != null) {
            return config;
        }
        // if not define, try to get the config of the class
        return getConfig(type);

    }

    protected void initialize() throws Exception {
        for (Class<? extends RunnerFeature> fc : featureClasses) {
            RunnerFeature rf = fc.newInstance();
            features.add(rf);
        }
        for (RunnerFeature feature : features) {
            feature.initialize(this);
        }
    }

    protected void beforeRun() throws Exception {
        invokeFeatures(features, new FeatureCallable() {

            @Override
            public void call(RunnerFeature feature) throws Exception {
                feature.beforeRun(FeaturesRunner.this);
            }
        });
    }

    protected void beforeMethodRun(final FrameworkMethod method,
            final Object test) throws Exception {
        MDC.put("fMethod", method.getMethod());
        invokeFeatures(features, new FeatureCallable() {

            @Override
            public void call(RunnerFeature feature) throws Exception {
                feature.beforeMethodRun(FeaturesRunner.this, method, test);
            }
        });
        injector.injectMembers(test);
    }

    protected void afterMethodRun(final FrameworkMethod method,
            final Object test) throws Exception {
        try {
            invokeFeatures(features, new FeatureCallable() {

                @Override
                public void call(RunnerFeature feature) throws Exception {
                    feature.afterMethodRun(FeaturesRunner.this, method, test);
                }
            });
        } finally {
            MDC.remove("fMethod");
        }
    }

    protected void afterRun() throws Exception {
        invokeFeatures(reversed(features), new FeatureCallable() {

            @Override
            public void call(RunnerFeature feature) throws Exception {
                feature.afterRun(FeaturesRunner.this);
            }
        });
    }

    protected void testCreated(final Object test) throws Exception {
        invokeFeatures(features, new FeatureCallable() {

            @Override
            public void call(RunnerFeature feature) throws Exception {
                feature.testCreated(test);
            }
        });
    }

    protected void start() throws Exception {
        MDC.put("fclass", getTargetTestClass());
        initialize();
        invokeFeatures(features, new FeatureCallable() {

            @Override
            public void call(RunnerFeature feature) throws Exception {
                feature.start(FeaturesRunner.this);
            }
        });
    }

    protected void stop() throws Exception {
        try {
            invokeFeatures(reversed(features), new FeatureCallable() {

                @Override
                public void call(RunnerFeature feature) throws Exception {
                    feature.stop(FeaturesRunner.this);
                }
            });
        } finally {
            MDC.remove("fclass");
        }
    }

    protected void beforeSetup() {
        invokeFeatures(features, new FeatureCallable() {

            @Override
            public void call(RunnerFeature feature) throws Exception {
                feature.beforeSetup(FeaturesRunner.this);
            }

        });
    }

    protected void afterTeardown() {
        invokeFeatures(reversed(features), new FeatureCallable() {

            @Override
            public void call(RunnerFeature feature) throws Exception {
                feature.afterTeardown(FeaturesRunner.this);
            }

        });
    }

    protected void configureBindings(final Binder binder) {
        binder.bind(FeaturesRunner.class).toInstance(this);
        binder.bind(TargetResourceLocator.class).toInstance(locator);
        invokeFeatures(reversed(features), new FeatureCallable() {

            @Override
            public void call(RunnerFeature feature) throws Exception {
                feature.configure(FeaturesRunner.this, binder);
            }

        });
    }

    /**
     * Gets the Guice injector.
     */
    public Injector getInjector() {
        return injector;
    }

    public void resetInjector() {
        injector = createInjector();
    }

    protected Injector createInjector() {
        Module module = new Module() {
            @Override
            public void configure(Binder arg0) {
                configureBindings(arg0);
            }
        };
        // build injector
        return Guice.createInjector(module);
    }

    protected final ConditionalIgnoreRule ignoreRule = new ConditionalIgnoreRule();

    protected final RandomBug randomBugRule = new RandomBug();

    @Override
    public void run(final RunNotifier notifier) {
        try {
            ignoreRule.check(getConfig(ConditionalIgnoreRule.Ignore.class),
                    getTargetTestClass());
        } catch (AssumptionViolatedException cause) {
            throw cause;
        }
        AssertionError errors = new AssertionError("features error");
        try {
            try {
                start();
                try {
                    beforeRun();
                    resetInjector();
                    super.run(notifier); // launch tests
                } catch (AssumptionViolatedException cause) {
                    throw cause;
                } catch (Exception error) {
                    errors.addSuppressed(error);
                } finally {
                    afterRun();
                }
            } catch (AssumptionViolatedException e) {
                notifier.fireTestIgnored(getDescription());
            } catch (Exception error) {
                errors.addSuppressed(error);
            } finally {
                try {
                    stop();
                } catch (Exception error) {
                    error.addSuppressed(errors);
                }
            }
        } catch (Throwable error) {
            errors.addSuppressed(error);
        } finally {
            if (errors.getSuppressed().length > 0) {
                notifier.fireTestFailure(new Failure(getDescription(), errors));
            }
        }
    }

    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier) {
        if (method.getAnnotation(Ignore.class) != null
                || (method.getAnnotation(RandomBug.Repeat.class) != null && RandomBug.getMode() == RandomBug.MODE.BYPASS)) {
            notifier.fireTestIgnored(describeChild(method));
            return;
        }
        beforeSetup();
        try {
            super.runChild(method, notifier);
        } finally {
            afterTeardown();
        }
    }

    @Override
    public Object createTest() throws Exception {
        // Return a Guice injected test class
        Object test = injector.getInstance(getTestClass().getJavaClass());
        // let features adapt the test object if needed
        try {
            testCreated(test);
        } catch (Exception e) {
            throw new RuntimeException("Failed to prepare test instance: "
                    + test, e);
        }
        return test;
    }

    @Override
    protected void validateZeroArgConstructor(List<Throwable> errors) {
        // Guice can inject constructors with parameters so we don't want this
        // method to trigger an error
    }

    @Override
    protected Statement methodInvoker(FrameworkMethod method, Object test) {
        return new InvokeMethod(method, test);
    }

    protected class InvokeMethod extends Statement {
        protected final FrameworkMethod testMethod;

        protected final Object target;

        protected InvokeMethod(FrameworkMethod testMethod, Object target) {
            this.testMethod = testMethod;
            this.target = target;
        }

        @Override
        public void evaluate() throws Throwable {
            beforeMethodRun(testMethod, target);
            try {
                testMethod.invokeExplosively(target);
            } finally {
                afterMethodRun(testMethod, target);
            }
        }
    }

    @Override
    public String toString() {
        return "FeaturesRunner [fTest=" + getTargetTestClass() + "]";
    }

    @Override
    protected List<org.junit.rules.MethodRule> rules(Object target) {
        List<org.junit.rules.MethodRule> rules = super.rules(target);
        rules.add(ignoreRule);
        rules.add(randomBugRule);
        return rules;
    }

    protected interface FeatureCallable {
        void call(RunnerFeature feature) throws Exception;
    }

    protected void invokeFeatures(List<RunnerFeature> features,
            FeatureCallable callable) {
        AssertionError errors = new AssertionError("invoke on features error "
                + features);
        for (RunnerFeature feature : features) {
            try {
                callable.call(feature);
                ;
            } catch (AssumptionViolatedException cause) {
                throw cause;
            } catch (Exception cause) {
                errors.addSuppressed(cause);
            }
        }
        if (errors.getSuppressed().length > 0) {
            throw errors;
        }
    }
}
