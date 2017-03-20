/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.automation.scripting.test.util.LogChecker;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @since 7.10
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.automation.scripting" })
@LocalDeploy({ "org.nuxeo.ecm.automation.scripting.tests:automation-scripting-contrib.xml" })
public class TestScriptHelpers {

    @Inject
    CoreSession session;

    @Inject
    AutomationService automationService;

    protected RuntimeService runtime = Framework.getRuntime();

    ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    private PrintStream outStream;

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
        Logger logger = Logger.getLogger("org.nuxeo.automation.scripting");
        List<LoggingEvent> logs = ((LogChecker) logger.getAppender("CHECKER")).getLogs();
        assertThat(logs.get(0).getLevel(), is(Level.WARN));
        assertThat(logs.get(0).getMessage(), is("Warnings"));
        assertThat(logs.get(1).getLevel(), is(Level.ERROR));
        assertThat(logs.get(1).getMessage(), is("Errors"));
        runtime.setProperty(Framework.NUXEO_DEV_SYSTEM_PROP, true);
        automationService.run(ctx, "Scripting.UseConsoleHelper", Collections.emptyMap());
        logs = ((LogChecker) logger.getAppender("CHECKER")).getLogs();
        assertThat(logs.get(2).getLevel(), is(Level.WARN));
        assertThat(logs.get(2).getMessage(), is("[INFO] Informations"));
        assertThat(logs.get(3).getLevel(), is(Level.WARN));
        assertThat(logs.get(3).getMessage(), is("Warnings"));
        assertThat(logs.get(4).getLevel(), is(Level.ERROR));
        assertThat(logs.get(4).getMessage(), is("Errors"));
    }

}
