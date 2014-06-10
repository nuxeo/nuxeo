package org.nuxeo.runtime.test.runner;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.log4j.MDC;
import org.junit.Ignore;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.nuxeo.runtime.mockito.MockProvider;

import com.google.common.collect.Lists;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

public class OSGiFeaturesRunner extends FeaturesRunner {

    protected Injector injector;

    protected List<RunnerFeature> features;

    public OSGiFeaturesRunner(Class<?> classToRun) throws InitializationError {
        super(classToRun);
    }

    protected final AnnotationScanner scanner = new AnnotationScanner();

    @Override
    public AnnotationScanner getScanner() {
        return scanner;
    }

    @Override
    public <T extends RunnerFeature> T getFeature(Class<T> type) {
        for (RunnerFeature rf : features) {
            if (rf.getClass() == type) {
                return type.cast(rf);
            }
        }
        return null;
    }

    @Override
    public List<RunnerFeature> getFeatures() {
        return features;
    }

    @Override
    public Injector getInjector() {
        return injector;
    }

    protected void resetInjector() {
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

    @Override
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

    @Override
    public <T extends Annotation> T getConfig(FrameworkMethod method,
            Class<T> type) {
        T config = method.getAnnotation(type);
        if (config != null) {
            return config;
        }
        // if not define, try to get the config of the class
        return getConfig(type);

    }

    @Override
    public void run(final RunNotifier notifier) {
        AssertionError errors = new AssertionError("features error");
        loadFeatures(getTargetTestClass(), errors);
        try {
            try {
                start();
                try {
                    beforeRun();
                    resetInjector();
                    super.run(notifier); // launch tests
                } catch (Exception error) {
                    errors.addSuppressed(error);
                } finally {
                    afterRun();
                }
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
                notifier.fireTestFailure(new Failure(getDescription(),
                        errors));
            }
        }
    }

    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier) {
        if (method.getAnnotation(Ignore.class) != null) {
            super.runChild(method, notifier);
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

    // not the most efficient to recompute this all the time
    // but it doesn't matter here
    protected static <T> List<T> reversed(List<T> list) {
        List<T> reversed = new ArrayList<T>(list);
        Collections.reverse(reversed);
        return reversed;
    }

    protected void beforeRun() throws Exception {
        for (RunnerFeature feature : features) {
            feature.beforeRun(this);
        }
    }

    protected void beforeMethodRun(FrameworkMethod method, Object test)
            throws Exception {
        MDC.put("fMethod", method.getMethod());
        for (RunnerFeature feature : features) {
            feature.beforeMethodRun(this, method, test);
        }
        injector.injectMembers(test);
    }

    protected void afterMethodRun(FrameworkMethod method, Object test)
            throws Exception {
        try {
            AssertionError errors = new AssertionError("test cleanup failure");
            for (RunnerFeature feature : reversed(features)) {
                try {
                    feature.afterMethodRun(this, method, test);
                } catch (Throwable error) {
                    errors.addSuppressed(error);
                }
            }
            if (errors.getSuppressed().length > 0) {
                throw errors;
            }
        } finally {
            MDC.remove("fMethod");
        }
    }

    protected void afterRun() throws Exception {
        AssertionError errors = new AssertionError("test cleanup failure");
        for (RunnerFeature feature : reversed(features)) {
            try {
                feature.afterRun(this);
            } catch (Throwable error) {
                errors.addSuppressed(error);
            }
        }
        if (errors.getSuppressed().length > 0) {
            throw errors;
        }
    }

    protected void testCreated(Object test) throws Exception {
        for (RunnerFeature feature : features) {
            feature.testCreated(test);
        }
    }

    protected void start() throws Exception {
        MDC.put("fclass", getTargetTestClass());
        for (RunnerFeature feature : features) {
            feature.start(this);
        }
    }

    protected void stop() throws Exception {
        try {
            AssertionError errors = new AssertionError("test cleanup failure");
            for (RunnerFeature feature : reversed(features)) {
                try {
                    feature.stop(this);
                } catch (Throwable error) {
                    errors.addSuppressed(error);
                }
            }
            if (errors.getSuppressed().length > 0) {
                throw errors;
            }
        } finally {
            MockProvider.INSTANCE.clearBindings();
            MDC.remove("fclass");
        }
    }

    protected void beforeSetup() {
        AssertionError errors = new AssertionError("Before setup errors");
        for (RunnerFeature feature : features) {
            try {
                feature.beforeSetup(this);
            } catch (Exception error) {
                errors.addSuppressed(error);
            }
        }
        if (errors.getSuppressed().length > 0) {
            throw errors;
        }
    }

    protected void afterTeardown() {
        AssertionError errors = new AssertionError("teardown errors");
        for (RunnerFeature feature : reversed(features)) {
            try {
                feature.afterTeardown(this);
            } catch (Throwable error) {
                errors.addSuppressed(error);
            }
        }
        if (errors.getSuppressed().length > 0) {
            throw errors;
        }
    }

    protected void configureBindings(Binder binder) {
        binder.bind(FeaturesRunner.class).toInstance(this);
        for (RunnerFeature feature : features) {
            feature.configure(this, binder);
        }
    }

    protected void loadFeatures(Class<?> classToRun, AssertionError errors)  {
        scanner.scan(classToRun);
        LinkedHashSet<Class<? extends RunnerFeature>> features = new LinkedHashSet<Class<? extends RunnerFeature>>();
        // load required features from annotation
        List<Features> annos = scanner.getAnnotations(classToRun,
                Features.class);
        if (annos != null) {
            for (Features anno : annos) {
                for (Class<? extends RunnerFeature> cl : anno.value()) {
                    if (!features.contains(cl)) {
                        loadFeature(cl, new HashSet<Class<?>>(), features, errors);
                    }
                }
            }
        }
        // register collected features
        this.features = new ArrayList<RunnerFeature>();
        for (Class<? extends RunnerFeature> fc : features) {
            RunnerFeature rf;
            try {
                rf = fc.newInstance();
                rf.initialize(this);
            } catch (Exception cause) {
                errors.addSuppressed(new AssertionError("Cannot load " + fc, cause));
                continue;
            }
            this.features.add(rf);
        }
    }

    protected void loadFeature(Class<? extends RunnerFeature> clazz, HashSet<Class<?>> cycles,
            LinkedHashSet<Class<? extends RunnerFeature>> features,
            AssertionError errors) {
        if (features.contains(clazz)) {
            return;
        }
        if (cycles.contains(clazz)) {
            errors.addSuppressed(new IllegalStateException(
                    "Cycle detected in features dependencies of " + clazz));
            return;
        }
        cycles.add(clazz);
        scanner.scan(clazz);
        // load required features from annotation
        List<Features> annos = scanner.getAnnotations(clazz, Features.class);
        if (annos != null) {
            for (Features anno : annos) {
                for (Class<? extends RunnerFeature> cl : anno.value()) {
                    if (!features.contains(cl)) {
                        loadFeature(cl, cycles, features, errors);
                    }
                }
            }
        }
        features.add(clazz); // add at the end to ensure requirements are added
                             // first
    }

}
