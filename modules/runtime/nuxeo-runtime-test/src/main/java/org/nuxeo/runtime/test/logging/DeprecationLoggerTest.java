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
import static org.junit.Assert.assertTrue;
import static org.nuxeo.runtime.api.Framework.NUXEO_TESTING_SYSTEM_PROP;

import java.util.List;

import javax.inject.Inject;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.logging.DeprecationLogger;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.LogFeature;
import org.nuxeo.runtime.test.runner.LoggerLevel;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, LogFeature.class, LogCaptureFeature.class })
public class DeprecationLoggerTest {

    protected static final String MESSAGE = "Deprecation contribution on component: oldComponent should now be contributed to extension point: newExtension";

    protected static final String DEPRECATED_VERSION = "11.1";

    protected static String ORIGINAL_TESTING_PROPERTY_VALUE;

    @Inject
    protected LogCaptureFeature.Result logCaptureResult;

    @BeforeClass
    public static void beforeAll() {
        ORIGINAL_TESTING_PROPERTY_VALUE = System.getProperty(NUXEO_TESTING_SYSTEM_PROP);
    }

    @AfterClass
    public static void afterAll() {
        System.setProperty(NUXEO_TESTING_SYSTEM_PROP, ORIGINAL_TESTING_PROPERTY_VALUE);
    }

    @Test
    @LogCaptureFeature.FilterOn(loggerClass = DeprecationLogger.class, logLevel = "WARN")
    public void shouldLogMessageAsWarning() {
        logDeprecatedMessageAndVerify(true);
    }

    @Test
    @LogCaptureFeature.FilterOn(loggerClass = DeprecationLogger.class, logLevel = "WARN")
    public void shouldNotLogMessageAsWarning() {
        System.setProperty(NUXEO_TESTING_SYSTEM_PROP, "false");
        logDeprecatedMessageAndVerify(false);
    }

    @Test
    @LogCaptureFeature.FilterOn(loggerClass = DeprecationLogger.class, logLevel = "INFO")
    @LoggerLevel(klass = DeprecationLogger.class, level = "INFO")
    public void shouldLogMessageAsInfo() {
        System.setProperty(NUXEO_TESTING_SYSTEM_PROP, "false");
        logDeprecatedMessageAndVerify(true);
    }

    @Test
    @LogCaptureFeature.FilterOn(loggerClass = DeprecationLogger.class, logLevel = "INFO")
    @LoggerLevel(klass = DeprecationLogger.class, level = "FATAL")
    public void shouldNotLogMessageAsInfo() {
        logDeprecatedMessageAndVerify(false);
    }

    protected void logDeprecatedMessageAndVerify(boolean messageShouldBeLogged) {
        DeprecationLogger.log(MESSAGE, DEPRECATED_VERSION);
        List<String> caughtEvents = logCaptureResult.getCaughtEventMessages();
        if (messageShouldBeLogged) {
            assertEquals(1, caughtEvents.size());
            assertEquals(String.format("Since version %s: %s", DEPRECATED_VERSION, MESSAGE), caughtEvents.get(0));
        } else {
            assertTrue(caughtEvents.isEmpty());
        }
    }
}
