/*
 * (C) Copyright 2015-2026 Nuxeo (http://nuxeo.com/) and others.
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.automation.scripting.AutomationScriptingFeature;
import org.nuxeo.automation.scripting.helper.Console;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.LogCaptureFeature.FilterOn;
import org.nuxeo.runtime.test.runner.LoggerLevel;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

/**
 * @since 7.10
 */
@RunWith(FeaturesRunner.class)
@Features({ AutomationScriptingFeature.class, LogCaptureFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@FilterOn(loggerClass = Console.class)
// set the level higher than INFO because we test scripting log which has logic depending on logger's level
@LoggerLevel(klass = Console.class, level = "WARN")
public class TestScriptHelpers {

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService automationService;

    @Inject
    protected LogCaptureFeature.Result logResult;

    @Test
    public void canUseConsoleHelper() throws OperationException {
        try (var ctx = new OperationContext(session)) {
            automationService.run(ctx, "Scripting.UseConsoleHelper", Map.of());
            List<LogEvent> logs = logResult.getCaughtEvents();
            assertEquals(2, logs.size());
            assertThat(logs.get(0).getLevel(), is(Level.WARN));
            assertThat(logs.get(0).getMessage().getFormattedMessage(), is("Warnings"));
            assertThat(logs.get(1).getLevel(), is(Level.ERROR));
            assertThat(logs.get(1).getMessage().getFormattedMessage(), is("Errors"));
        }
    }

    @Test
    @WithFrameworkProperty(name = Framework.NUXEO_DEV_SYSTEM_PROP, value = "true")
    public void canHaveInfoLogsInDevMode() throws OperationException {
        try (var ctx = new OperationContext(session)) {
            automationService.run(ctx, "Scripting.UseConsoleHelper", Map.of());
            List<LogEvent> logs = logResult.getCaughtEvents();
            assertEquals(3, logs.size());
            assertThat(logs.get(0).getLevel(), is(Level.WARN));
            assertThat(logs.get(0).getMessage().getFormattedMessage(), is("[INFO] Informations"));
            assertThat(logs.get(1).getLevel(), is(Level.WARN));
            assertThat(logs.get(1).getMessage().getFormattedMessage(), is("Warnings"));
            assertThat(logs.get(2).getLevel(), is(Level.ERROR));
            assertThat(logs.get(2).getMessage().getFormattedMessage(), is("Errors"));
        }
    }

}
