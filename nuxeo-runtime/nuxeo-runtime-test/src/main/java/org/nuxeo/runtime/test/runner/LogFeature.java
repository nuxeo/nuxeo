/*
 * (C) Copyright 2017-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */
package org.nuxeo.runtime.test.runner;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.ConsoleAppender.Target;
import org.apache.logging.log4j.core.filter.ThresholdFilter;

/**
 * @since 9.3
 */
public class LogFeature implements RunnerFeature {

    protected static final String CONSOLE_APPENDER = "CONSOLE";

    protected static final String CONSOLE_LOG_FEATURE_APPENDER = "CONSOLE_LOG_FEATURE";

    protected ConsoleAppender consoleAppender;

    protected ConsoleAppender hiddenAppender;

    public void hideWarningFromConsoleLog() {
        setConsoleLogThreshold(Level.ERROR.toString());
    }

    /**
     * @since 9.10
     */
    public void hideErrorFromConsoleLog() {
        setConsoleLogThreshold(Level.FATAL.toString());
    }

    /**
     * @since 9.10
     */
    public void setConsoleLogThreshold(String level) {
        if (consoleAppender != null) {
            return;
        }

        Logger rootLogger = LoggerContext.getContext(false).getRootLogger();
        consoleAppender = (ConsoleAppender) rootLogger.getAppenders().get(CONSOLE_APPENDER);
        rootLogger.removeAppender(consoleAppender);
        ConsoleAppender newAppender = ConsoleAppender.newBuilder()
                                                     .withName(CONSOLE_LOG_FEATURE_APPENDER)
                                                     .setTarget(Target.SYSTEM_OUT)
                                                     .withFilter(ThresholdFilter.createFilter(Level.toLevel(level),
                                                             null, null))
                                                     .build();
        rootLogger.addAppender(newAppender);
        hiddenAppender = newAppender;
    }

    public void restoreConsoleLog() {
        if (consoleAppender == null) {
            return;
        }

        Logger rootLogger = LoggerContext.getContext(false).getRootLogger();
        rootLogger.removeAppender(hiddenAppender);
        rootLogger.addAppender(consoleAppender);
        consoleAppender = null;
        hiddenAppender = null;
    }

}
