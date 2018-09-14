/*
 * (C) Copyright 2015-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.automation.scripting.test;

import static java.lang.Boolean.TRUE;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.automation.scripting.helper.Console;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.LogCaptureFeature.FilterOn;

/**
 * @since 7.10
 */
// this test needs "org.nuxeo.automation.scripting.helper.Console" logger to have level == WARN, see log4j2-test.xml
// because we test scripting log which has logic depending on logger's level, wee Console class
@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, LogCaptureFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.scripting")
@Deploy("org.nuxeo.ecm.automation.scripting:automation-scripting-contrib.xml")
@FilterOn(loggerClass = Console.class)
public class TestScriptHelpers {

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService automationService;

    @Inject
    protected LogCaptureFeature.Result logResult;

    protected ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    protected PrintStream outStream;

    @Before
    public void setUpStreams() {
        outStream = System.out;
        System.setOut(new PrintStream(outContent));
    }

    @After
    public void cleanUpStreams() throws IOException {
        outContent.close();
        System.setOut(outStream);
    }

    @Test
    public void canUseConsoleHelper() throws OperationException {
        OperationContext ctx = new OperationContext(session);
        automationService.run(ctx, "Scripting.UseConsoleHelper", Collections.emptyMap());
        assertEquals("", outContent.toString());
        List<LogEvent> logs = logResult.getCaughtEvents();
        assertEquals(2, logs.size());
        assertThat(logs.get(0).getLevel(), is(Level.WARN));
        assertThat(logs.get(0).getMessage().getFormattedMessage(), is("Warnings"));
        assertThat(logs.get(1).getLevel(), is(Level.ERROR));
        assertThat(logs.get(1).getMessage().getFormattedMessage(), is("Errors"));

        // test now in dev mode
        logResult.clear();
        Framework.getRuntime().setProperty(Framework.NUXEO_DEV_SYSTEM_PROP, TRUE);
        automationService.run(ctx, "Scripting.UseConsoleHelper", Collections.emptyMap());
        logs = logResult.getCaughtEvents();
        assertEquals(3, logs.size());
        assertThat(logs.get(0).getLevel(), is(Level.WARN));
        assertThat(logs.get(0).getMessage().getFormattedMessage(), is("[INFO] Informations"));
        assertThat(logs.get(1).getLevel(), is(Level.WARN));
        assertThat(logs.get(1).getMessage().getFormattedMessage(), is("Warnings"));
        assertThat(logs.get(2).getLevel(), is(Level.ERROR));
        assertThat(logs.get(2).getMessage().getFormattedMessage(), is("Errors"));
    }

}
