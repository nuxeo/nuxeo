/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     slacoin, jcarsique
 *
 */
package org.nuxeo.runtime.test.runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.MDC;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

/**
 * Define execution rules for an annotated random bug.
 * <p>
 * Principle is to increase consistency on tests which have a random behavior. Such test is a headache because:
 * <ul>
 * <li>some developers may ask to ignore a random test since it's not reliable and produces useless noise most of the
 * time,</li>
 * <li>however, the test may still be useful in continuous integration for checking the non-random part of code it
 * covers,</li>
 * <li>and, after all, there's a random bug which should be fixed!</li>
 * </ul>
 * </p>
 * <p>
 * Compared to the @{@link Ignore} JUnit annotation, the advantage is to provide different behaviors for different use
 * cases. The wanted behavior depending on whereas:
 * <ul>
 * <li>we are working on something else and don't want being bothered by an unreliable test,</li>
 * <li>we are working on the covered code and want to be warned in case of regression,</li>
 * <li>we are working on the random bug and want to reproduce it.</li>
 * </ul>
 * </p>
 * That means that a random bug cannot be ignored. But must attempt to reproduce or hide its random aspect, depending on
 * its execution context. For instance: <blockquote>
 *
 * <pre>
 * <code>
 * import org.nuxeo.runtime.test.runner.FeaturesRunner;
 * import org.nuxeo.runtime.test.RandomBugRule;
 *
 * {@literal @}RunWith(FeaturesRunner.class)
 * public class TestSample {
 *     public static final String NXP99999 = "Some comment or description";
 *
 *     {@literal @}Test
 *     {@literal @}RandomBug.Repeat(issue = NXP99999, onFailure=5, onSuccess=50)
 *     public void testWhichFailsSometimes() throws Exception {
 *         assertTrue(java.lang.Math.random() > 0.2);
 *     }
 * }</code>
 * </pre>
 *
 * </blockquote>
 * <p>
 * In the above example, the test fails sometimes. With the {@link RandomBug.Repeat} annotation, it will be repeated in
 * case of failure up to 5 times until success. This is the default {@link Mode#RELAX} mode. In order to reproduce the
 * bug, use the {@link Mode#STRICT} mode. It will be repeated in case of success up to 50 times until failure. In
 * {@link Mode#BYPASS} mode, the test is ignored.
 * </p>
 * <p>
 * You may also repeat a whole suite in the same way by annotating the class itself. You may want also want to skip some
 * tests, then you can annotate them and set {@link Repeat#bypass()} to true.
 * </p>
 *
 * @see Mode
 * @since 5.9.5
 */
public class RandomBug {
    private static final Log log = LogFactory.getLog(RandomBug.class);

    protected static final RandomBug self = new RandomBug();

    /**
     * Repeat condition based on
     *
     * @see Mode
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD, ElementType.TYPE })
    @Inherited
    public @interface Repeat {
        /**
         * Reference in issue management system. Recommendation is to use a constant which name is the issue reference
         * and value is a description or comment.
         */
        String issue();

        /**
         * Times to repeat until failure in case of success
         */
        int onSuccess() default 30;

        /**
         * Times to repeat until success in case of failure
         */
        int onFailure() default 10;

        /**
         * Bypass a method/suite ....
         */
        boolean bypass() default false;
    }

    public static class Feature implements RunnerFeature {
        @ClassRule
        public static TestRule onClass() {
            return self.onTest();
        }

        @Rule
        public MethodRule onMethod() {
            return self.onMethod();
        }
    }

    public class RepeatRule implements TestRule, MethodRule {
        @Inject
        protected RunNotifier notifier;

        @Inject
        FeaturesRunner runner;

        public RepeatStatement statement;

        @Override
        public Statement apply(Statement base, Description description) {
            Repeat actual = runner.getConfig(Repeat.class);
            if (actual.issue() == null) {
                return base;
            }
            return statement = onRepeat(actual, notifier, base, description);
        }

        @Override
        public Statement apply(Statement base, FrameworkMethod method, Object fixtureTarget) {
            Repeat actual = method.getAnnotation(Repeat.class);
            if (actual == null) {
                return base;
            }
            Class<?> fixtureType = fixtureTarget.getClass();
            Description description = Description.createTestDescription(fixtureType, method.getName(),
                    method.getAnnotations());
            return statement = onRepeat(actual, notifier, base, description);
        }
    }

    protected RepeatRule onTest() {
        return new RepeatRule();
    }

    protected RepeatRule onMethod() {
        return new RepeatRule();
    }

    public static final String MODE_PROPERTY = "nuxeo.tests.random.mode";

    /**
     * <ul>
     * <li>BYPASS: the test is ignored. Like with @{@link Ignore} JUnit annotation.</li>
     * <li>STRICT: the test must fail. On success, the test is repeated until failure or the limit number of tries
     * {@link Repeat#onSuccess()} is reached. If it does not fail during the tries, then the whole test class is marked
     * as failed.</li>
     * <li>RELAX: the test must succeed. On failure, the test is repeated until success or the limit number of tries
     * {@link Repeat#onFailure()} is reached.</li>
     * </ul>
     * Could be set by the environment using the <em>nuxeo.tests.random.mode</em>T system property.
     */
    public enum Mode {
        BYPASS, STRICT, RELAX
    }

    /**
     * The default mode if {@link #MODE_PROPERTY} is not set.
     */
    public final Mode DEFAULT = Mode.RELAX;

    protected Mode fetchMode() {
        String mode = System.getProperty(MODE_PROPERTY, DEFAULT.name());
        return Mode.valueOf(mode.toUpperCase());
    }

    protected abstract class RepeatStatement extends Statement {
        protected final Repeat params;

        protected final RunNotifier notifier;

        protected boolean gotFailure;

        protected final RunListener listener = new RunListener() {
            @Override
            public void testStarted(Description desc) {
                log.debug(displayName(desc) + " STARTED");
            }

            @Override
            public void testFailure(Failure failure) {
                gotFailure = true;
                log.debug(displayName(failure.getDescription()) + " FAILURE");
                log.trace(failure, failure.getException());
            }

            @Override
            public void testAssumptionFailure(Failure failure) {
                log.debug(displayName(failure.getDescription()) + " ASSUMPTION FAILURE");
                log.trace(failure, failure.getException());
            }

            @Override
            public void testIgnored(Description desc) {
                log.debug(displayName(desc) + " IGNORED");
            }

            @Override
            public void testFinished(Description desc) {
                log.debug(displayName(desc) + " FINISHED");
            }
        };

        protected final Statement base;

        protected int serial;

        protected Description description;

        protected RepeatStatement(Repeat someParams, RunNotifier aNotifier, Statement aStatement,
                Description aDescription) {
            params = someParams;
            notifier = aNotifier;
            base = aStatement;
            description = aDescription;
        }

        protected String displayName(Description desc) {
            String displayName = desc.getClassName().substring(desc.getClassName().lastIndexOf(".") + 1);
            if (desc.isTest()) {
                displayName += "." + desc.getMethodName();
            }
            return displayName;
        }

        protected void onEnter(int aSerial) {
            MDC.put("fRepeat", serial = aSerial);
        }

        protected void onLeave() {
            MDC.remove("fRepeat");
        }

        @Override
        public void evaluate() {
            Error error = error();
            notifier.addListener(listener);
            try {
                log.debug(displayName(description) + " STARTED");
                for (int retry = 1; retry <= retryCount(); retry++) {
                    gotFailure = false;
                    onEnter(retry);
                    try {
                        log.debug(displayName(description) + " retry " + retry);
                        base.evaluate();
                    } catch (AssumptionViolatedException cause) {
                        Throwable t = new Throwable("On retry " + retry, cause);
                        error.addSuppressed(t);
                        notifier.fireTestAssumptionFailed(new Failure(description, t));
                    } catch (Throwable cause) {
                        // Repeat annotation is on method (else the Throwable is not raised up to here)
                        Throwable t = new Throwable("On retry " + retry, cause);
                        error.addSuppressed(t);
                        if (returnOnFailure()) {
                            notifier.fireTestFailure(new Failure(description, t));
                        } else {
                            gotFailure = true;
                            log.debug(displayName(description) + " FAILURE SWALLOW");
                            log.trace(t, t);
                        }
                    } finally {
                        onLeave();
                    }
                    if (gotFailure && returnOnFailure()) {
                        log.debug(displayName(description) + " returnOnFailure");
                        return;
                    }
                    if (!gotFailure && returnOnSuccess()) {
                        log.debug(displayName(description) + " returnOnSuccess");
                        return;
                    }
                }
            } finally {
                log.debug(displayName(description) + " FINISHED");
                notifier.removeListener(listener);
            }
            log.trace("throw " + error);
            throw error;
        }

        protected abstract Error error();

        protected abstract int retryCount();

        protected abstract boolean returnOnSuccess();

        protected abstract boolean returnOnFailure();
    }

    protected class RepeatOnFailure extends RepeatStatement {
        protected String issue;

        protected RepeatOnFailure(Repeat someParams, RunNotifier aNotifier, Statement aStatement,
                Description description) {
            super(someParams, aNotifier, aStatement, description);
        }

        @Override
        protected Error error() {
            return new AssertionError(String.format(
                    "No success after %d tries. Either the bug is not random "
                            + "or you should increase the 'onFailure' value.\n" + "Issue: %s",
                    params.onFailure(), issue));
        }

        @Override
        protected int retryCount() {
            return params.onFailure();
        }

        @Override
        protected boolean returnOnFailure() {
            return false;
        }

        @Override
        protected boolean returnOnSuccess() {
            return true;
        }
    }

    protected class RepeatOnSuccess extends RepeatStatement {
        protected RepeatOnSuccess(Repeat someParams, RunNotifier aNotifier, Statement aStatement,
                Description description) {
            super(someParams, aNotifier, aStatement, description);
        }

        @Override
        protected Error error() {
            return new AssertionError(String.format(
                    "No failure after %d tries. Either the bug is fixed "
                            + "or you should increase the 'onSuccess' value.\n" + "Issue: %s",
                    params.onSuccess(), params.issue()));
        }

        @Override
        protected boolean returnOnFailure() {
            return true;
        }

        @Override
        protected boolean returnOnSuccess() {
            return false;
        }

        @Override
        protected int retryCount() {
            return params.onSuccess();
        }
    }

    protected class Bypass extends RepeatStatement {
        public Bypass(Repeat someParams, RunNotifier aNotifier, Statement aStatement, Description description) {
            super(someParams, aNotifier, aStatement, description);
        }

        @Override
        public void evaluate() {
            notifier.fireTestIgnored(description);
        }

        @Override
        protected Error error() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected int retryCount() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected boolean returnOnSuccess() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected boolean returnOnFailure() {
            return false;
        }
    }

    protected RepeatStatement onRepeat(Repeat someParams, RunNotifier aNotifier, Statement aStatement,
            Description description) {
        if (someParams.bypass()) {
            return new Bypass(someParams, aNotifier, aStatement, description);
        }
        switch (fetchMode()) {
        case BYPASS:
            return new Bypass(someParams, aNotifier, aStatement, description);
        case STRICT:
            return new RepeatOnSuccess(someParams, aNotifier, aStatement, description);
        case RELAX:
            return new RepeatOnFailure(someParams, aNotifier, aStatement, description);
        }
        throw new IllegalArgumentException("no such mode");
    }

}
