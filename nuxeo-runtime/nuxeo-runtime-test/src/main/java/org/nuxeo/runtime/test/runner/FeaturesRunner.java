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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.nuxeo.runtime.test.TargetResourceLocator;

import com.google.common.collect.Lists;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;

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

    protected class FeatureHolder {
        protected final Class<? extends RunnerFeature> type;

        protected final TestClass testClass;

        protected RunnerFeature feature;

        FeatureHolder(Class<? extends RunnerFeature> aType)
                throws InstantiationException, IllegalAccessException {
            type = aType;
            testClass = new TestClass(aType);
            feature = aType.newInstance();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            FeatureHolder other = (FeatureHolder) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (type == null) {
                if (other.type != null) {
                    return false;
                }
            } else if (!type.equals(other.type)) {
                return false;
            }
            return true;
        }

        private FeaturesRunner getOuterType() {
            return FeaturesRunner.this;
        }

    }

    protected final List<FeatureHolder> holders = new LinkedList<>();

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
            Class<? extends RunnerFeature> clazz) throws Exception {
        if (holders.contains(clazz)) {
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
                    loadFeature(cycles, cl);
                }
            }
        }
        holders.add(new FeatureHolder(clazz));
    }

    public void loadFeatures(Class<?> classToRun) throws Exception {
        scanner.scan(classToRun);
        // load required features from annotation
        List<Features> annos = scanner.getAnnotations(classToRun,
                Features.class);
        if (annos != null) {
            for (Features anno : annos) {
                for (Class<? extends RunnerFeature> cl : anno.value()) {
                    loadFeature(new HashSet<Class<?>>(), cl);
                }
            }
        }
    }

    public <T extends RunnerFeature> T getFeature(Class<T> type) {
        return type.cast(holders.get(holders.indexOf(type)).feature);
    }

    public Iterable<RunnerFeature> getFeatures() {
        return new Iterable<RunnerFeature>() {

            @Override
            public Iterator<RunnerFeature> iterator() {
                return new Iterator<RunnerFeature>() {

                    Iterator<FeatureHolder> iterator = holders.iterator();

                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public RunnerFeature next() {
                        return iterator.next().feature;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }

                };
            }

        };
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
        for (FeatureHolder each : Lists.reverse(holders)) {
            annotation = scanner.getAnnotation(each.type, type);
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
        for (RunnerFeature each : getFeatures()) {
            each.initialize(this);
        }
    }

    protected void beforeRun() throws Exception {
        invokeFeatures(holders, new FeatureCallable() {

            @Override
            public void call(RunnerFeature feature) throws Exception {
                feature.beforeRun(FeaturesRunner.this);
            }
        });
    }

    protected void beforeMethodRun(final FrameworkMethod method,
            final Object test) throws Exception {
        invokeFeatures(holders, new FeatureCallable() {

            @Override
            public void call(RunnerFeature feature) throws Exception {
                feature.beforeMethodRun(FeaturesRunner.this, method, test);
            }
        });
        injector.injectMembers(test);
    }

    protected void afterMethodRun(final FrameworkMethod method,
            final Object test) throws Exception {
        invokeFeatures(holders, new FeatureCallable() {

            @Override
            public void call(RunnerFeature feature) throws Exception {
                feature.afterMethodRun(FeaturesRunner.this, method, test);
            }
        });
    }

    protected void afterRun() throws Exception {
        injector = injector.getParent();
        invokeFeatures(reversed(holders), new FeatureCallable() {

            @Override
            public void call(RunnerFeature feature) throws Exception {
                feature.afterRun(FeaturesRunner.this);
            }
        });
    }

    protected void testCreated(final Object test) throws Exception {
        invokeFeatures(holders, new FeatureCallable() {

            @Override
            public void call(RunnerFeature feature) throws Exception {
                feature.testCreated(test);
            }
        });
    }

    protected void start() throws Exception {
        invokeFeatures(holders, new FeatureCallable() {

            @Override
            public void call(RunnerFeature feature) throws Exception {
                feature.start(FeaturesRunner.this);
            }
        });
    }

    protected void stop() throws Exception {
        invokeFeatures(reversed(holders), new FeatureCallable() {

            @Override
            public void call(RunnerFeature feature) throws Exception {
                feature.stop(FeaturesRunner.this);
            }
        });
        holders.clear();
    }

    protected void beforeSetup() throws Exception {

        invokeFeatures(holders, new FeatureCallable() {

            @Override
            public void call(RunnerFeature feature) throws Exception {
                feature.beforeSetup(FeaturesRunner.this);
            }

        });

        injector.injectMembers(underTest);
        testCreated(underTest);
    }

    protected void afterTeardown() {
        invokeFeatures(reversed(holders), new FeatureCallable() {

            @Override
            public void call(RunnerFeature feature) throws Exception {
                feature.afterTeardown(FeaturesRunner.this);
            }

        });
    }

    protected void configureBindings() {
        injector = injector.createChildInjector(new FeaturesModule());
        for (FeatureHolder each : holders) {
            injector.injectMembers(each.feature);
        }
    }

    public Injector getInjector() {
        return injector;
    }

    protected Injector onInjector(RunNotifier aNotifier) {
        Injector injector = Guice.createInjector(Stage.DEVELOPMENT,
                new RunnerModule(aNotifier));
        return injector;

    }

    protected class RunnerModule implements Module {

        protected final RunNotifier notifier;

        protected Binder binder;

        public RunnerModule(RunNotifier aNotifier) {
            notifier = aNotifier;
        }

        @Override
        public void configure(Binder aBinder) {
            if (binder != null) {
                throw new IllegalStateException("Cannot re-enter");
            }

            aBinder.bind(FeaturesRunner.class).toInstance(FeaturesRunner.this);
            aBinder.bind(RunNotifier.class).toInstance(notifier);
            aBinder.bind(TargetResourceLocator.class).toInstance(locator);
            binder = aBinder;
        }
    }

    protected class FeaturesModule implements Module {

        @Override
        public void configure(Binder binder) {
            for (FeatureHolder each : holders) {
                each.feature.configure(FeaturesRunner.this, binder);
            }
        }

    }

    protected class BeforeClassStatement extends Statement {
        protected final Statement next;

        protected BeforeClassStatement(Statement aStatement) {
            next = aStatement;
        }

        @Override
        public void evaluate() throws Throwable {
            initialize();
            start();
            configureBindings();
            beforeRun();
            next.evaluate();
        }

    }

    @Override
    protected Statement withBeforeClasses(Statement statement) {
        Statement actual = statement;
        actual = super.withBeforeClasses(actual);
        actual = new BeforeClassStatement(actual);
        return actual;
    }

    protected class AfterClassStatement extends Statement {
        protected final Statement previous;

        protected AfterClassStatement(Statement aStatement) {
            previous = aStatement;
        }

        @Override
        public void evaluate() throws Throwable {
            previous.evaluate();
            try {
                afterRun();
            } finally {
                stop();
            }
        }
    }

    @Override
    protected Statement withAfterClasses(Statement statement) {
        Statement actual = statement;
        actual = super.withAfterClasses(statement);
        actual = new AfterClassStatement(actual);
        return actual;
    }

    protected RulesFactory<ClassRule, TestRule> classRulesFactory = new RulesFactory<>(
            ClassRule.class, TestRule.class);

    protected RulesFactory<Rule, TestRule> testRulesFactory = new RulesFactory<>(
            Rule.class, TestRule.class);

    protected RulesFactory<Rule, MethodRule> methodRulesFactory = new RulesFactory<>(
            Rule.class, MethodRule.class);

    @Override
    protected Statement classBlock(final RunNotifier aNotifier) {
        injector = onInjector(aNotifier);
        return super.classBlock(aNotifier);
    }

    @Override
    protected List<TestRule> classRules() {
        List<TestRule> actual = new ArrayList<>();

        for (FeatureHolder each : holders) {
            actual.addAll(classRulesFactory.onRules(each.testClass, null));
        }
        actual.addAll(super.classRules());
        return actual;
    }

    protected class BeforeMethodRunStatement extends Statement {

        protected final Statement next;

        protected final FrameworkMethod method;

        protected final Object target;

        protected BeforeMethodRunStatement(FrameworkMethod aMethod,
                Object aTarget, Statement aStatement) {
            method = aMethod;
            target = aTarget;
            next = aStatement;
        }

        @Override
        public void evaluate() throws Throwable {
            beforeMethodRun(method, target);
            next.evaluate();
        }

    }

    protected class BeforeSetupStatement extends Statement {

        protected final Statement next;

        protected BeforeSetupStatement(Statement aStatement) {
            next = aStatement;
        }

        @Override
        public void evaluate() throws Throwable {
            beforeSetup();
            next.evaluate();
        }

    }

    @Override
    protected Statement withBefores(FrameworkMethod method, Object target,
            Statement statement) {
        Statement actual = statement;
        actual = new BeforeMethodRunStatement(method, target, actual);
        actual = super.withBefores(method, target, actual);
        actual = new BeforeSetupStatement(actual);
        return actual;
    }

    protected class AfterMethodRunStatement extends Statement {

        protected final Statement previous;

        protected final FrameworkMethod method;

        protected final Object target;

        protected AfterMethodRunStatement(FrameworkMethod aMethod,
                Object aTarget, Statement aStatement) {
            method = aMethod;
            target = aTarget;
            previous = aStatement;
        }

        @Override
        public void evaluate() throws Throwable {
            try {
                previous.evaluate();
            } finally {
                afterMethodRun(method, target);
            }
        }

    }

    protected class AfterTeardownStatement extends Statement {

        protected final Statement previous;

        protected AfterTeardownStatement(Statement aStatement) {
            previous = aStatement;
        }

        @Override
        public void evaluate() throws Throwable {
            try {
                previous.evaluate();
            } finally {
                afterTeardown();
            }
        }

    }

    @Override
    protected Statement withAfters(FrameworkMethod method, Object target,
            Statement statement) {
        Statement actual = statement;
        actual = new AfterMethodRunStatement(method, target, statement);
        actual = super.withAfters(method, target, statement);
        actual = new AfterTeardownStatement(actual);
        return actual;
    }

    @Override
    protected List<TestRule> getTestRules(Object target) {
        List<TestRule> actual = new ArrayList<TestRule>();
        for (FeatureHolder each : holders) {
            actual.addAll(testRulesFactory
                .onRules(each.testClass, each.feature));
        }
        actual.addAll(testRulesFactory.onRules(getTestClass(), target));
        return actual;
    }

    @Override
    protected List<MethodRule> rules(Object target) {
        final List<MethodRule> actual = new ArrayList<>();
        for (FeatureHolder each : holders) {
            actual.addAll(methodRulesFactory.onRules(each.testClass,
                    each.feature));
        }
        actual.addAll(methodRulesFactory.onRules(getTestClass(), target));
        return actual;
    }

    protected Object underTest;

    @Override
    public Object createTest() throws Exception {
        underTest = super.createTest();
        // TODO replace underTest member with a binding
        // Class<?> testType = underTest.getClass();
        // injector.getInstance(Binder.class).bind(testType)
        // .toInstance(testType.cast(underTest));
        return underTest;
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

    protected interface FeatureCallable {
        void call(RunnerFeature feature) throws Exception;
    }

    protected void invokeFeatures(Iterable<FeatureHolder> features,
            FeatureCallable callable) {
        AssertionError errors = new AssertionError("invoke on features error "
                + features);
        for (FeatureHolder each : features) {
            try {
                callable.call(each.feature);
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

    protected class RulesFactory<A extends Annotation, R> {

        protected final Class<A> annotationType;

        protected final Class<R> ruleType;

        protected RulesFactory(Class<A> anAnnotationType, Class<R> aRuleType) {
            annotationType = anAnnotationType;
            ruleType = aRuleType;
        }

        protected List<R> onRules(TestClass aType, Object aTest) {
            List<R> actual = new ArrayList<>();
            for (R each : aType.getAnnotatedFieldValues(aTest, annotationType,
                    ruleType)) {
                actual.add(each);
            }

            for (FrameworkMethod each : aType
                .getAnnotatedMethods(annotationType)) {
                if (ruleType.isAssignableFrom(each.getMethod().getReturnType())) {
                    actual.add(onRule(ruleType, each, aTest));
                }
            }

            return actual;
        }

        protected R onRule(Class<R> aRuleType, FrameworkMethod aMethod,
                Object aTarget, Object... someParms) {
            try {
                R aRule = aRuleType.cast(aMethod.invokeExplosively(aTarget,
                        someParms));
                injector.injectMembers(aRule);
                return aRule;
            } catch (Throwable cause) {
                throw new RuntimeException(
                        "Errors in rules factory " + aMethod, cause);
            }
        }

    }

}
