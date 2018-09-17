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
 *     clement chevalier
 */
package org.nuxeo.runtime.test.runner;

import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.LogCaptureFeature.NoLogCaptureFilterException;

@RunWith(FeaturesRunner.class)
@Features({ LogCaptureFeature.class })
public class LogCaptureFeatureFilterOnTest {

    private static final Log log1 = LogFactory.getLog("loggerOne");

    private static final Log log2 = LogFactory.getLog("loggerTwo");

    @Inject
    LogCaptureFeature.Result logCaptureResult;

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "FATAL")
    public void testLogLevel() throws NoLogCaptureFilterException {
        generateTestLogs();
        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        assertEquals(2, events.size());
        assertEquals("Testing loggerOne", events.get(0).getMessage());
        assertEquals("Testing loggerTwo", events.get(1).getMessage());
    }

    @Test
    @LogCaptureFeature.FilterOn(loggerName = "loggerOne")
    public void testLoggerName() throws NoLogCaptureFilterException {
        generateTestLogs();
        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        assertEquals(2, events.size());
        assertEquals("Testing loggerOne", events.get(0).getMessage());
        assertEquals("Another testing of loggerOne", events.get(1).getMessage());
    }

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "WARN", loggerName = "loggerTwo")
    public void testLogLevelAndLoggerName() throws NoLogCaptureFilterException {
        generateTestLogs();
        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        assertEquals(1, events.size());
        assertEquals("This is another test for loggerTwo", events.get(0).getMessage());
    }

    private void generateTestLogs() {
        log1.fatal("Testing loggerOne");
        log1.warn("Another testing of loggerOne");

        log2.fatal("Testing loggerTwo");
        log2.warn("This is another test for loggerTwo");
    }

}
