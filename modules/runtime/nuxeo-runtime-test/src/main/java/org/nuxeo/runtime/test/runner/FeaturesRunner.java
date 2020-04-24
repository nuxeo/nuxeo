/*
 * (C) Copyright 2006-2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.runtime.test.runner;

import static org.nuxeo.runtime.test.runner.FeaturesRunner.Direction.BACKWARD;
import static org.nuxeo.runtime.test.runner.FeaturesRunner.Direction.FORWARD;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.nuxeo.common.function.ThrowableConsumer;
import org.nuxeo.common.function.ThrowableRunnable;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.test.TargetResourceLocator;

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

    private static final Logger log = LogManager.getLogger(FeaturesRunner.class);

    protected static final AnnotationScanner scanner = new AnnotationScanner();

    /** @since 11.1 */
    protected static final String CUSTOM_ENVIRONMENT_SYSTEM_PROPERTY = "custom.environment";

    /** @since 11.1 */
    protected static final String DEFAULT_BUILD_DIRECTORY = "target";

    /**
     * Guice injector.
     */
    protected Injector injector;

    protected final FeaturesLoader loader = new FeaturesLoader(this);

    protected final TargetResourceLocator locator;

    public static AnnotationScanner getScanner() {
        return scanner;
    }

    /**
     * Returns the Maven build directory, depending on the {@value #CUSTOM_ENVIRONMENT_SYSTEM_PROPERTY} system property.
     *
     * @since 11.1
     */
    public static String getBuildDirectory() {
        String customEnvironment = System.getProperty(CUSTOM_ENVIRONMENT_SYSTEM_PROPERTY);
        return customEnvironment == null ? DEFAULT_BUILD_DIRECTORY
                : String.format("%s-%s", DEFAULT_BUILD_DIRECTORY, customEnvironment);
    }

    public FeaturesRunner(Class<?> classToRun) throws InitializationError {
        super(classToRun);
        locator = new TargetResourceLocator(classToRun);
        try {
            loader.loadFeatures(getTargetTestClass());
        } catch (Throwable t) { // NOSONAR
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

    /**
     * @deprecated since 11.1, seems unused
     */
    @Deprecated(since = "11.1")
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
     * @since 5.6
     */
    public <T extends Annotation> T getConfig(Class<T> type) {
        List<T> configs = new ArrayList<>();
        T annotation = scanner.getAnnotation(getTargetTestClass(), type);
        if (annotation != null) {
            configs.add(annotation);
        }
        apply("getAnnotation", BACKWARD, holder -> {
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
        log.trace("Invoke initialize on features");
        for (RunnerFeature each : getFeatures()) {
            each.initialize(this);
        }
    }

    protected void beforeRun() {
        apply("beforeRun", FORWARD, holder -> holder.feature.beforeRun(FeaturesRunner.this));
    }

    protected void beforeMethodRun(final FrameworkMethod method, final Object test) {
        apply("beforeMethodRun", FORWARD, holder -> holder.feature.beforeMethodRun(FeaturesRunner.this, method, test));
    }

    protected void afterMethodRun(final FrameworkMethod method, final Object test) {
        apply("afterMethodRun", FORWARD, holder -> holder.feature.afterMethodRun(FeaturesRunner.this, method, test));
    }

    protected void afterRun() {
        apply("afterRun", BACKWARD, holder -> holder.feature.afterRun(FeaturesRunner.this));
    }

    protected void start() {
        apply("start", FORWARD, holder -> holder.feature.start(FeaturesRunner.this));
    }

    protected void stop() {
        apply("stop", BACKWARD, holder -> holder.feature.stop(FeaturesRunner.this));
    }

    protected void beforeSetup(final FrameworkMethod method, final Object test) {
        apply("beforeSetup", FORWARD, holder -> holder.feature.beforeSetup(FeaturesRunner.this, method, test));
        injector.injectMembers(underTest);
    }

    protected void afterTeardown(final FrameworkMethod method, final Object test) {
        apply("afterTeardown", BACKWARD, holder -> holder.feature.afterTeardown(FeaturesRunner.this, method, test));
    }

    /**
     * @since 11.1
     */
    protected void apply(String id, Direction direction, ThrowableConsumer<FeaturesLoader.Holder, Exception> consumer) {
        apply(id, direction == FORWARD ? loader.holders() : loader.reversedHolders(), consumer);
    }

    /**
     * @since 11.1
     */
    protected void apply(String id, Collection<FeaturesLoader.Holder> holders,
            ThrowableConsumer<FeaturesLoader.Holder, Exception> consumer) {
        log.trace("Invoke: {} on features: {}", () -> id, () -> formatFeatures(holders));
        Supplier<String> msg = () -> String.format("Error while invoking %s on features: %s", id,
                formatFeatures(holders));
        var errors = new ArrayList<Throwable>();
        for (FeaturesLoader.Holder holder : holders) {
            try {
                consumer.accept(holder);
            } catch (AssumptionViolatedException cause) {
                log.debug("Test ignored by the feature: {}", holder.type);
                throw cause;
            } catch (InterruptedException cause) {
                Thread.currentThread().interrupt();
                throw new RuntimeServiceException(msg.get(), cause);
            } catch (Throwable cause) { // NOSONAR
                log.debug("Error while invoking: {} on feature: {}", id, holder.type);
                throwSeriousError(cause);
                errors.add(cause);
            }
        }
        if (!errors.isEmpty()) {
            var exception = new AssertionError(msg.get());
            errors.forEach(exception::addSuppressed);
            throw exception;
        }
    }

    protected String formatFeatures(Collection<FeaturesLoader.Holder> holders) {
        return holders.stream().map(h -> h.type.getName()).collect(Collectors.joining(", ", "[", "]"));
    }

    public void evaluateRunnable(ThrowableRunnable<Throwable> runnable, ThrowableRunnable<Throwable> finisher)
            throws Throwable {
        Throwable error = null;
        try {
            runnable.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeServiceException(e);
        } catch (Throwable t) { // NOSONAR
            throwSeriousError(t);
            error = t;
        }
        // don't use finally, we want to stop if thread was interrupted
        try {
            finisher.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeServiceException(e);
        } catch (Throwable t) { // NOSONAR
            throwSeriousError(t);
            if (error == null) {
                error = t;
            } else {
                error.addSuppressed(t);
            }
        }
        if (error != null) {
            throw error;
        }
    }

    protected void throwSeriousError(Throwable cause) {
        if (cause instanceof Error && !(cause instanceof AssertionError)) {
            throw (Error) cause; // serious error, rethrow immediately
        }
    }

    public Injector getInjector() {
        return injector;
    }

    protected Injector onInjector(final RunNotifier aNotifier) {
        return Guice.createInjector(Stage.DEVELOPMENT, (Module) aBinder -> {
            aBinder.bind(FeaturesRunner.class).toInstance(FeaturesRunner.this);
            aBinder.bind(RunNotifier.class).toInstance(aNotifier);
            aBinder.bind(TargetResourceLocator.class).toInstance(locator);
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
            evaluateRunnable(FeaturesRunner.this::afterRun, FeaturesRunner.this::stop);
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
        apply("classRules", FORWARD, holder -> factory.withRules(holder.testClass, null));

        return factory.build();
    }

    protected abstract static class MethodStatement extends Statement {

        protected final FrameworkMethod method;

        protected final Object target;

        protected final Statement statement;

        public MethodStatement(FrameworkMethod method, Object target, Statement statement) {
            this.method = method;
            this.target = target;
            this.statement = statement;
        }
    }

    protected class BeforeMethodRunStatement extends MethodStatement {

        protected BeforeMethodRunStatement(FrameworkMethod method, Object target, Statement statement) {
            super(method, target, statement);
        }

        @Override
        public void evaluate() throws Throwable {
            beforeMethodRun(method, target);
            statement.evaluate();
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

    protected class AfterMethodRunStatement extends MethodStatement {

        protected AfterMethodRunStatement(FrameworkMethod method, Object target, Statement statement) {
            super(method, target, statement);
        }

        @Override
        public void evaluate() throws Throwable {
            evaluateRunnable(statement::evaluate, () -> afterMethodRun(method, target));
        }

    }

    protected class AfterTeardownStatement extends MethodStatement {

        protected AfterTeardownStatement(FrameworkMethod method, Object target, Statement statement) {
            super(method, target, statement);
        }

        @Override
        public void evaluate() throws Throwable {
            evaluateRunnable(statement::evaluate, () -> afterTeardown(method, target));
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
        apply("getTestRules", FORWARD, holder -> factory.withRules(holder.testClass, holder.feature));
        factory.withRules(getTestClass(), target);
        return factory.build();
    }

    @Override
    protected List<MethodRule> rules(Object target) {
        final RulesFactory<Rule, MethodRule> factory = new RulesFactory<>(Rule.class, MethodRule.class);
        apply("rules", FORWARD, holder -> factory.withRules(holder.testClass, holder.feature));
        factory.withRules(getTestClass(), target);
        return factory.build();
    }

    protected Object underTest;

    @Override
    public Object createTest() throws Exception {
        underTest = super.createTest();
        apply("createTest", FORWARD, holder -> holder.feature.testCreated(underTest));
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
                @SuppressWarnings({ "unchecked", "rawtypes" })
                public void evaluate() throws Throwable {
                    injector = injector.createChildInjector((Module) binder -> {
                        for (Object each : rules) {
                            binder.bind((Class) each.getClass()).annotatedWith(Names.named(name)).toInstance(each);
                            binder.requestInjection(each);
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
                throw new RuntimeServiceException("Errors in rules factory " + aMethod, cause);
            }
        }

    }

    public <T extends RunnerFeature> T getFeature(Class<T> aType) {
        return loader.getFeature(aType);
    }

    protected enum Direction {
        FORWARD, BACKWARD
    }

}
