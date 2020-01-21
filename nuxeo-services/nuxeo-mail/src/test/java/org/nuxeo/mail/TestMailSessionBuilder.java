/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.mail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.mail.MailConstants.CONFIGURATION_MAIL_DEBUG;

import java.util.List;
import java.util.Properties;

import javax.inject.Inject;
import javax.mail.Session;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.LogFeature;
import org.nuxeo.runtime.test.runner.LoggerLevel;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

/**
 * @since 11.1
 */
@LoggerLevel(klass = Session.class, level = "DEBUG")
@LogCaptureFeature.FilterOn(loggerClass = Session.class, logLevel = "DEBUG")
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, LogFeature.class, LogCaptureFeature.class })
public class TestMailSessionBuilder {

    @Inject
    protected LogCaptureFeature.Result logCaptureResult;

    @Test
    public void testBuildFromProperties() {
        Session session = MailSessionBuilder.fromProperties(new Properties()).build();
        assertNotNull(session);
        assertTrue(logCaptureResult.getCaughtEventMessages().isEmpty());
    }

    /*
     * NXP-28275
     */
    @Test
    public void testBuildFromPropertiesWithProgrammaticDebug() {
        Session session = MailSessionBuilder.fromProperties(new Properties()).debug().build();
        assertNotNull(session);
        logCaptureResult.assertHasEvent();
        List<String> logs = logCaptureResult.getCaughtEventMessages();
        assertEquals(1, logs.size());
        String log = logs.get(0);
        assertTrue(log, log.startsWith("setDebug: JavaMail version "));
    }

    /*
     * NXP-28275
     */
    @Test
    @WithFrameworkProperty(name = CONFIGURATION_MAIL_DEBUG, value = "true")
    public void testBuildFromPropertiesWithFrameworkDebug() {
        Session session = MailSessionBuilder.fromProperties(new Properties()).build();
        assertNotNull(session);
        logCaptureResult.assertHasEvent();
        List<String> logs = logCaptureResult.getCaughtEventMessages();
        String log = logs.get(0);
        assertTrue(log, log.startsWith("setDebug: JavaMail version "));
    }
}
