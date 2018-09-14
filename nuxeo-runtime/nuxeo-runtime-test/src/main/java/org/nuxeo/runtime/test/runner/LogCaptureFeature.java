/*
 * (C) Copyright 2012-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Sun Seng David TAN <stan@nuxeo.com>, slacoin, jcarsique
 */
package org.nuxeo.runtime.test.runner;

import static java.util.stream.Collectors.toList;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.message.Message;
import org.junit.Assert;
import org.junit.runners.model.FrameworkMethod;

import com.google.common.base.Strings;

/**
 * Test feature to capture from a log4j appender to check that some log4j calls have been correctly called.</br>
 * On a test class or a test method using this feature, a default filter can be configured with the annotation
 * {@link LogCaptureFeature.FilterOn} or a custom one implementing {@link LogCaptureFeature.Filter} class can be
 * provided with the annotation {@link LogCaptureFeature.FilterWith} to select the log events to capture.</br>
 * A {@link LogCaptureFeature.Result} instance is to be injected with {@link javax.inject.Inject} as an attribute of the
 * test.</br>
 * The method {@link LogCaptureFeature.Result#assertHasEvent()} can then be called from test methods to check that
 * matching log calls (events) have been captured.
 *
 * @since 5.7
 */
public class LogCaptureFeature implements RunnerFeature {

    public class NoLogCaptureFilterException extends Exception {
        private static final long serialVersionUID = 1L;
    }

    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD })
    public @interface FilterWith {
        /**
         * Custom implementation of a filter to select event to capture.
         */
        Class<? extends LogCaptureFeature.Filter> value();
    }

    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD })
    public @interface FilterOn {
        /**
         * Configuration for the default filter
         */
        String loggerName() default "";

        String logLevel() default "";
    }

    private static class DefaultFilter implements LogCaptureFeature.Filter {

        String loggerName;

        Level logLevel;

        public DefaultFilter(String loggerName, String logLevel) {
            super();
            this.loggerName = Strings.emptyToNull(loggerName);
            if (!"".equals(logLevel)) {
                this.logLevel = Level.toLevel(logLevel);
            }
        }

        @Override
        public boolean accept(LogEvent event) {
            if (logLevel != null && !logLevel.equals(event.getLevel())) {
                return false;
            }
            return loggerName == null || loggerName.equals(event.getLoggerName());
        }
    }

    public class Result {
        protected final List<LogEvent> caughtEvents = new ArrayList<>();

        protected boolean noFilterFlag = false;

        public void assertHasEvent() throws NoLogCaptureFilterException {
            if (noFilterFlag) {
                throw new LogCaptureFeature.NoLogCaptureFilterException();
            }
            Assert.assertFalse("No log result found", caughtEvents.isEmpty());
        }

        public void clear() {
            caughtEvents.clear();
            noFilterFlag = false;
        }

        public List<LogEvent> getCaughtEvents() {
            return caughtEvents;
        }

        public List<String> getCaughtEventMessages() {
            return caughtEvents.stream().map(LogEvent::getMessage).map(Message::getFormattedMessage).collect(toList());
        }

        protected void setNoFilterFlag(boolean noFilterFlag) {
            this.noFilterFlag = noFilterFlag;
        }
    }

    public interface Filter {
        /**
         * {@link LogCaptureFeature} will capture the event if it does match the implementation condition.
         */
        boolean accept(LogEvent event);
    }

    protected Filter logCaptureFilter;

    protected final Result myResult = new Result();

    protected Logger rootLogger = LoggerContext.getContext(false).getRootLogger();

    protected Appender logAppender = new AbstractAppender("LOG_CAPTURE_APPENDER", null, null) {

        @Override
        public void append(LogEvent event) {
            if (logCaptureFilter == null) {
                myResult.setNoFilterFlag(true);
                return;
            }
            if (logCaptureFilter.accept(event)) {
                myResult.caughtEvents.add(event);
            }
        }
    };

    private Filter setupCaptureFiler;

    @Override
    public void configure(FeaturesRunner runner, com.google.inject.Binder binder) {
        binder.bind(Result.class).toInstance(myResult);
    }

    @Override
    public void beforeSetup(FeaturesRunner runner) throws Exception {
        Filter filter;
        FilterWith filterProvider = runner.getConfig(FilterWith.class);
        if (filterProvider.value() == null) {
            FilterOn defaultFilterConfig = runner.getConfig(FilterOn.class);
            if (defaultFilterConfig == null) {
                return;
            } else {
                filter = new DefaultFilter(defaultFilterConfig.loggerName(), defaultFilterConfig.logLevel());
            }
        } else {
            filter = filterProvider.value().newInstance();
        }
        enable(filter);
    }

    @Override
    public void afterTeardown(FeaturesRunner runner) {
        disable();
    }

    @Override
    public void beforeMethodRun(FeaturesRunner runner, FrameworkMethod method, Object test) throws Exception {
        Filter filter;
        FilterWith filterProvider = runner.getConfig(method, FilterWith.class);
        if (filterProvider.value() == null) {
            FilterOn defaultFilterConfig = runner.getConfig(method, FilterOn.class);
            if (defaultFilterConfig == null) {
                return;
            } else {
                filter = new DefaultFilter(defaultFilterConfig.loggerName(), defaultFilterConfig.logLevel());
            }
        } else {
            filter = filterProvider.value().newInstance();
        }
        enable(filter);
    }

    @Override
    public void afterMethodRun(FeaturesRunner runner, FrameworkMethod method, Object test) {
        disable();
    }

    /**
     * @since 8.4
     */
    protected void enable(Filter filter) {

        if (logCaptureFilter != null) {
            setupCaptureFiler = logCaptureFilter;
        } else {
            logAppender.start();
            rootLogger.addAppender(logAppender);
        }
        logCaptureFilter = filter;
    }

    /**
     * @since 6.0
     */
    protected void disable() {
        if (setupCaptureFiler != null) {
            logCaptureFilter = setupCaptureFiler;
            setupCaptureFiler = null;
            return;
        }
        if (logCaptureFilter != null) {
            myResult.clear();
            rootLogger.removeAppender(logAppender);
            logAppender.stop();
            logCaptureFilter = null;
        }
    }

}
