/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.nuxeo.runtime.test.TargetResourceLocator;
import org.nuxeo.runtime.test.runner.FeaturesLoader.Direction;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.name.Names;

/**
 * A Test Case runner that can be extended through features and provide injection though Guice.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FeaturesRunner extends BlockJUnit4ClassRunner {

    protected static final AnnotationScanner scanner = new AnnotationScanner();

    /**
     * Guice injector.
     */
    protected Injector injector;

    protected final FeaturesLoader loader = new FeaturesLoader(this);

    protected final TargetResourceLocator locator;

    public static AnnotationScanner getScanner() {
        return scanner;
    }

    public FeaturesRunner(Class<?> classToRun) throws InitializationError {
        super(classToRun);
        locator = new TargetResourceLocator(classToRun);
        try {
            loader.loadFeatures(getTargetTestClass());
        } catch (Throwable t) {
            throw new InitializationError(Collections.singletonList(t));
        }
    }

    public Class<?> getTargetTestClass() {
        return super.getTestClass().getJavaClass();
    }

    /**
     * May return null if the test class was not yet instantiated
     */
    public Object getTargetTestInstance() {
        return underTest;
    }

    public Path getTargetTestBasepath() {
        return locator.getBasepath();
    }

    public URL getTargetTestResource(String name) throws IOException {
        return locator.getTargetTestResource(name);
    }

    public Iterable<RunnerFeature> getFeatures() {
        return loader.features();
    }

    /**
     * Returns the list of annotations present on the test class, then on the interfaces implemented by the test class,
     * then on its super class, then on the different features.
     *
     * @return the list of annotations for the current test
     * @since 2021.15
     */
    public <T extends Annotation> List<T> getAnnotations(Class<T> type) {
        List<T> annotations = new ArrayList<>(scanner.getAnnotations(getTargetTestClass(), type));
        loader.apply(Direction.BACKWARD, holder -> annotations.addAll(scanner.getAnnotations(holder.type, type)));
        return annotations;
    }

    /**
     * Returns the list of annotations for the given method.
     *
     * @return the list of annotations for the given method
     * @since 2021.15
     */
    public <T extends Annotation> List<T> getAnnotations(Class<T> type, FrameworkMethod method) {
        return new ArrayList<>(Arrays.asList(method.getMethod().getAnnotationsByType(type)));
    }

    /**
     * Returns the list of annotations for the given method if the method is given, otherwise return the list of
     * annotations for the current test.
     * <p>
     * This is a convenient method for feature that has the same code to execute for before class and before method
     * steps.
     *
     * @return the list of annotations for the given method
     * @since 2021.15
     */
    public <T extends Annotation> List<T> getMethodOrTestAnnotations(Class<T> type, @Nullable FrameworkMethod method) {
        if (method != null) {
            return getAnnotations(type, method);
        }
        return getAnnotations(type);
    }

    /**
     * Returns the list of annotations for the given method, if no annotation has been found, get the annotations for
     * the current test.
     *
     * @return the list of annotations for the given method, if they exist, otherwise the list of annotations for the
     *         current test
     * @since 2021.15
     */
    public <T extends Annotation> List<T> getMethodAnnotationsWithClassFallback(Class<T> type,
            @Nullable FrameworkMethod method) {
        if (method != null) {
            List<T> annotations = getAnnotations(type, method);
            if (!annotations.isEmpty()) {
                return annotations;
            }
        }
        return getAnnotations(type);
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
        loader.apply(Direction.BACKWARD, holder -> {
            T hAnnotation = scanner.getAnnotation(holder.type, type);
            if (hAnnotation != null) {
                configs.add(hAnnotation);
            }
        });
        return Defaults.of(type, configs);
    }

    /**
     * Get the annotation on the test method, if no annotation has been found, get the annotation from the test class
     * (See {@link #getConfig(Class)})
     *
     * @since 5.7
     */
    public <T extends Annotation> T getConfig(FrameworkMethod method, Class<T> type) {
        T config = method.getAnnotation(type);
        if (config != null) {
            return config;
        }
        // if not define, try to get the config of the class
        return getConfig(type);

    }

    @Override
    protected List<FrameworkMethod> computeTestMethods() {
        List<FrameworkMethod> methods = super.computeTestMethods();
        // sort a copy
        methods = new ArrayList<>(methods);
        MethodSorter.sortMethodsUsingSourceOrder(methods);
        return methods;
    }

    protected void initialize() throws Exception {
        for (RunnerFeature each : getFeatures()) {
            each.initialize(this);
        }
    }

    protected void beforeRun() throws Exception {
        loader.apply(Direction.FORWARD, holder -> holder.feature.beforeRun(FeaturesRunner.this));
    }

    protected void beforeMethodRun(final FrameworkMethod method, final Object test) throws Exception {
        loader.apply(Direction.FORWARD, holder -> holder.feature.beforeMethodRun(FeaturesRunner.this, method, test));
    }

    protected void afterMethodRun(final FrameworkMethod method, final Object test) throws Exception {
        loader.apply(Direction.FORWARD, holder -> holder.feature.afterMethodRun(FeaturesRunner.this, method, test));
    }

    protected void afterRun() throws Exception {
        loader.apply(Direction.BACKWARD, holder -> holder.feature.afterRun(FeaturesRunner.this));
    }

    protected void start() throws Exception {
        loader.apply(Direction.FORWARD, holder -> holder.feature.start(FeaturesRunner.this));
    }

    protected void stop() throws Exception {
        loader.apply(Direction.BACKWARD, holder -> holder.feature.stop(FeaturesRunner.this));
    }

    protected void beforeSetup(final FrameworkMethod method, final Object test) {
        loader.apply(Direction.FORWARD, holder -> holder.feature.beforeSetup(FeaturesRunner.this, method, test));
        injector.injectMembers(underTest);
    }

    protected void afterTeardown(final FrameworkMethod method, final Object test) {
        loader.apply(Direction.BACKWARD, holder -> holder.feature.afterTeardown(FeaturesRunner.this, method, test));
    }

    public Injector getInjector() {
        return injector;
    }

    protected Injector onInjector(final RunNotifier aNotifier) {
        return Guice.createInjector(Stage.DEVELOPMENT, new Module() {

            @Override
            public void configure(Binder aBinder) {
                aBinder.bind(FeaturesRunner.class).toInstance(FeaturesRunner.this);
                aBinder.bind(RunNotifier.class).toInstance(aNotifier);
                aBinder.bind(TargetResourceLocator.class).toInstance(locator);
            }

        });
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
            beforeRun();
            injector = injector.createChildInjector(loader.onModule());
            try {
                next.evaluate();
            } finally {
                injector = injector.getParent();
            }
        }

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
        actual = super.withAfterClasses(actual);
        actual = new AfterClassStatement(actual);
        return actual;
    }

    @Override
    protected Statement classBlock(final RunNotifier aNotifier) {
        injector = onInjector(aNotifier);
        return super.classBlock(aNotifier);
    }

    @Override
    protected List<TestRule> classRules() {
        final RulesFactory<ClassRule, TestRule> factory = new RulesFactory<>(ClassRule.class, TestRule.class);

        factory.withRule((base, description) -> new BeforeClassStatement(base)).withRules(super.classRules());
        loader.apply(Direction.FORWARD, holder -> factory.withRules(holder.testClass, null));

        return factory.build();
    }

    protected class BeforeMethodRunStatement extends Statement {

        protected final Statement next;

        protected final FrameworkMethod method;

        protected final Object target;

        protected BeforeMethodRunStatement(FrameworkMethod aMethod, Object aTarget, Statement aStatement) {
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

    protected abstract class MethodStatement extends Statement {

        protected final FrameworkMethod method;

        protected final Object target;

        protected final Statement statement;

        public MethodStatement(FrameworkMethod method, Object target, Statement statement) {
            this.method = method;
            this.target = target;
            this.statement = statement;
        }
    }

    protected class BeforeSetupStatement extends MethodStatement {

        protected BeforeSetupStatement(FrameworkMethod method, Object target, Statement statement) {
            super(method, target, statement);
        }

        @Override
        public void evaluate() throws Throwable {
            beforeSetup(method, target);
            statement.evaluate();
        }

    }

    @Override
    protected Statement withBefores(FrameworkMethod method, Object target, Statement statement) {
        Statement actual = statement;
        actual = new BeforeMethodRunStatement(method, target, actual);
        actual = super.withBefores(method, target, actual);
        actual = new BeforeSetupStatement(method, target, actual);
        return actual;
    }

    protected class AfterMethodRunStatement extends Statement {

        protected final Statement previous;

        protected final FrameworkMethod method;

        protected final Object target;

        protected AfterMethodRunStatement(FrameworkMethod aMethod, Object aTarget, Statement aStatement) {
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

    protected class AfterTeardownStatement extends MethodStatement {

        protected AfterTeardownStatement(FrameworkMethod method, Object target, Statement statement) {
            super(method, target, statement);
        }

        @Override
        public void evaluate() throws Throwable {
            try {
                statement.evaluate();
            } finally {
                afterTeardown(method, target);
            }
        }

    }

    @Override
    protected Statement withAfters(FrameworkMethod method, Object target, Statement statement) {
        Statement actual = statement;
        actual = new AfterMethodRunStatement(method, target, actual);
        actual = super.withAfters(method, target, actual);
        actual = new AfterTeardownStatement(method, target, actual);
        return actual;
    }

    @Override
    protected List<TestRule> getTestRules(Object target) {
        final RulesFactory<Rule, TestRule> factory = new RulesFactory<>(Rule.class, TestRule.class);
        loader.apply(Direction.FORWARD, holder -> factory.withRules(holder.testClass, holder.feature));
        factory.withRules(getTestClass(), target);
        return factory.build();
    }

    @Override
    protected List<MethodRule> rules(Object target) {
        final RulesFactory<Rule, MethodRule> factory = new RulesFactory<>(Rule.class, MethodRule.class);
        loader.apply(Direction.FORWARD, holder -> factory.withRules(holder.testClass, holder.feature));
        factory.withRules(getTestClass(), target);
        return factory.build();
    }

    protected Object underTest;

    @Override
    public Object createTest() throws Exception {
        underTest = super.createTest();
        loader.apply(Direction.FORWARD, holder -> holder.feature.testCreated(underTest));
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
    public String toString() {
        return "FeaturesRunner [fTest=" + getTargetTestClass() + "]";
    }

    protected class RulesFactory<A extends Annotation, R> {

        protected Statement build(final Statement base, final String name) {
            return new Statement() {

                @Override
                public void evaluate() throws Throwable {
                    injector = injector.createChildInjector(new Module() {

                        @SuppressWarnings({ "unchecked", "rawtypes" })
                        @Override
                        public void configure(Binder binder) {
                            for (Object each : rules) {
                                binder.bind((Class) each.getClass()).annotatedWith(Names.named(name)).toInstance(each);
                                binder.requestInjection(each);
                            }
                        }

                    });

                    try {
                        base.evaluate();
                    } finally {
                        injector = injector.getParent();
                    }

                }

            };

        }

        protected class BindRule implements TestRule, MethodRule {

            @Override
            public Statement apply(Statement base, FrameworkMethod method, Object target) {
                Statement statement = build(base, "method");
                for (Object each : rules) {
                    statement = ((MethodRule) each).apply(statement, method, target);
                }
                return statement;
            }

            @Override
            public Statement apply(Statement base, Description description) {
                if (rules.isEmpty()) {
                    return base;
                }
                Statement statement = build(base, "test");
                for (Object each : rules) {
                    statement = ((TestRule) each).apply(statement, description);
                }
                return statement;
            }

        }

        protected final Class<A> annotationType;

        protected final Class<R> ruleType;

        protected ArrayList<R> rules = new ArrayList<>();

        protected RulesFactory(Class<A> anAnnotationType, Class<R> aRuleType) {
            annotationType = anAnnotationType;
            ruleType = aRuleType;
        }

        public RulesFactory<A, R> withRules(List<R> someRules) {
            this.rules.addAll(someRules);
            return this;
        }

        public RulesFactory<A, R> withRule(R aRule) {
            injector.injectMembers(aRule);
            rules.add(aRule);
            return this;
        }

        public RulesFactory<A, R> withRules(TestClass aType, Object aTest) {
            for (R each : aType.getAnnotatedFieldValues(aTest, annotationType, ruleType)) {
                withRule(each);
            }

            for (FrameworkMethod each : aType.getAnnotatedMethods(annotationType)) {
                if (ruleType.isAssignableFrom(each.getMethod().getReturnType())) {
                    withRule(onMethod(ruleType, each, aTest));
                }
            }
            return this;
        }

        public List<R> build() {
            return Collections.singletonList(ruleType.cast(new BindRule()));
        }

        protected R onMethod(Class<R> aRuleType, FrameworkMethod aMethod, Object aTarget, Object... someParms) {
            try {
                return aRuleType.cast(aMethod.invokeExplosively(aTarget, someParms));
            } catch (Throwable cause) {
                throw new RuntimeException("Errors in rules factory " + aMethod, cause);
            }
        }

    }

    public <T extends RunnerFeature> T getFeature(Class<T> aType) {
        return loader.getFeature(aType);
    }

}
