/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana
 */

package org.nuxeo.runtime.test.logging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.nuxeo.runtime.test.runner.ConsoleLogLevelThreshold;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogFeature;
import org.nuxeo.runtime.test.runner.LoggerLevel;
import org.nuxeo.runtime.test.runner.LoggerLevels;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * Test for {@link LogFeature}.
 *
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, LogFeature.class })
@LoggerLevel(name = "org.nuxeo.FakeClass2", level = "DEBUG")
@LoggerLevel(name = "org.nuxeo.FakeClass3", level = "FATAL")
public class TestLogFeature {

    protected static final Logger log = LogManager.getLogger(TestLogFeature.class);

    protected static final String FAKE_LOGGER_NAME_1 = "org.nuxeo.FakeClass1";

    protected static final String FAKE_LOGGER_NAME_2 = "org.nuxeo.FakeClass2";

    protected static final String FAKE_LOGGER_NAME_3 = "org.nuxeo.FakeClass3";

    @ClassRule
    public final static LogFeatureClassCheckerRule LOG_FEATURE_CLASS_CHECKER_RULE = new LogFeatureClassCheckerRule();

    @Rule
    public final LogFeatureMethodCheckerRule logFeatureMethodCheckerRule = new LogFeatureMethodCheckerRule();

    @Test
    @LoggerLevel(name = FAKE_LOGGER_NAME_1, level = "TRACE")
    public void shouldAddNewLogger() {
        assertTrue(LogManager.getLogger(FAKE_LOGGER_NAME_1).isTraceEnabled());
    }

    @Test
    @LoggerLevel(klass = TestLogFeature.class, level = "DEBUG")
    public void shouldUpdateLogger() {
        assertTrue(LogManager.getLogger(TestLogFeature.class).isDebugEnabled());
    }

    @Test
    @LoggerLevel(name = FAKE_LOGGER_NAME_2, level = "TRACE")
    @LoggerLevel(name = FAKE_LOGGER_NAME_3, level = "INFO")
    public void shouldOverrideTestLoggerDefinition() {
        assertTrue(LogManager.getLogger(FAKE_LOGGER_NAME_2).isTraceEnabled());
        assertTrue(LogManager.getLogger(FAKE_LOGGER_NAME_3).isInfoEnabled());
    }

    @Test
    public void shouldInheritLogger() {
        assertTrue(LogManager.getLogger(FAKE_LOGGER_NAME_2).isDebugEnabled());
        assertTrue(LogManager.getLogger(FAKE_LOGGER_NAME_3).isFatalEnabled());
    }

    @Test
    @ConsoleLogLevelThreshold("FATAL")
    public void shouldSetConsoleLogThreshold() {
        LoggerContext context = LoggerContext.getContext(false);
        org.apache.logging.log4j.core.Logger rootLogger = context.getRootLogger();
        Appender appender = rootLogger.getAppenders().get("CONSOLE_LOG_FEATURE");
        assertNotNull(appender);
        ConsoleAppender console = (ConsoleAppender) appender;
        Filter filter = console.getFilter();
        assertNotNull(filter);
        assertTrue(filter instanceof ThresholdFilter);
        ThresholdFilter threshold = (ThresholdFilter) filter;
        assertEquals(Level.FATAL, threshold.getLevel());
    }

    /**
     * Ensures that all log levels are preserved before launching the whole class test
     * {@link org.nuxeo.runtime.test.runner.RunnerFeature#beforeRun(FeaturesRunner)} and correctly restored after that
     * which means after the execution of {@link org.nuxeo.runtime.test.runner.RunnerFeature#afterRun(FeaturesRunner)}
     */
    public static class LogFeatureClassCheckerRule implements TestRule {
        @Override
        public Statement apply(final Statement base, final Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    before();
                    try {
                        base.evaluate();
                    } finally {
                        after();
                    }
                }

                protected void before() {
                    LoggerContext context = LoggerContext.getContext(false);

                    // Inherit from "org.nuxeo" see log4j2-test.xml
                    assertTrue(log.isInfoEnabled());

                    // Ensure that there is no loggers for these two fake classes
                    assertFalse(context.hasLogger(FAKE_LOGGER_NAME_1));
                    assertFalse(context.hasLogger(FAKE_LOGGER_NAME_2));
                    assertFalse(context.hasLogger(FAKE_LOGGER_NAME_3));
                }

                protected void after() {
                    // Ensure that we have the original level value, as it was before the whole test.
                    assertTrue(log.isInfoEnabled());

                    // The new created loggers should be turned off.
                    assertEquals(Level.OFF, LogManager.getLogger(FAKE_LOGGER_NAME_1).getLevel());
                    assertEquals(Level.OFF, LogManager.getLogger(FAKE_LOGGER_NAME_2).getLevel());
                    assertEquals(Level.OFF, LogManager.getLogger(FAKE_LOGGER_NAME_3).getLevel());
                }
            };
        }
    }

    /**
     * Ensures that all log levels are preserved before launching each test method
     * {@link org.nuxeo.runtime.test.runner.RunnerFeature#beforeMethodRun(FeaturesRunner, FrameworkMethod, Object)} and
     * correctly restored after that which means after the execution of
     * {@link org.nuxeo.runtime.test.runner.RunnerFeature#afterMethodRun(FeaturesRunner, FrameworkMethod, Object)}
     */
    public class LogFeatureMethodCheckerRule implements TestRule {
        @Override
        public Statement apply(final Statement base, final Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    before(description);
                    try {
                        base.evaluate();
                    } finally {
                        after(description);
                    }
                }

                protected void before(final Description description) {
                    getLoggersAnnotations(description).forEach(a -> check(a, true));
                }

                protected void after(final Description description) {
                    getLoggersAnnotations(description).forEach(a -> check(a, false));
                }
            };
        }

        protected Collection<LoggerLevel> getLoggersAnnotations(Description description) {
            var annotations = new ArrayList<LoggerLevel>();

            LoggerLevel logger = description.getAnnotation(LoggerLevel.class);
            if (logger != null) {
                annotations.add(logger);
            }

            LoggerLevels loggers = description.getAnnotation(LoggerLevels.class);
            if (loggers != null) {
                annotations.addAll(List.of(loggers.value()));
            }

            return annotations;
        }

        protected void check(LoggerLevel logger, boolean before) {
            LoggerContext context = LoggerContext.getContext(false);
            if (logger.klass().equals(TestLogFeature.class)) {
                assertEquals(Level.INFO, log.getLevel());
            } else {
                switch (logger.name()) {
                case FAKE_LOGGER_NAME_1:
                    if (before) {
                        assertFalse(context.hasLogger(FAKE_LOGGER_NAME_1));
                    } else {
                        assertEquals(Level.OFF, LogManager.getLogger(FAKE_LOGGER_NAME_1).getLevel());
                    }
                    break;
                case FAKE_LOGGER_NAME_2:
                    assertEquals(Level.DEBUG, LogManager.getLogger(FAKE_LOGGER_NAME_2).getLevel());
                    break;
                case FAKE_LOGGER_NAME_3:
                    assertEquals(Level.FATAL, LogManager.getLogger(FAKE_LOGGER_NAME_3).getLevel());
                    break;

                default:
                    return;
                }
            }

        }
    }
}
