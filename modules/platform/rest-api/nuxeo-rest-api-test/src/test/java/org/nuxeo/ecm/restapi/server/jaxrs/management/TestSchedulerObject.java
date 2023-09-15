/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */

package org.nuxeo.ecm.restapi.server.jaxrs.management;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.nuxeo.ecm.core.event.test.CapturingEventListener;
import org.nuxeo.ecm.restapi.test.ManagementBaseTest;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Deploy;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 2023.3
 */
@Deploy("org.nuxeo.ecm.core.management")
public class TestSchedulerObject extends ManagementBaseTest {

    @Test
    public void testGetSchedulerTasks() throws IOException {
        try (CloseableClientResponse response = httpClientRule.get("/management/scheduler")) {
            assertEquals(HttpServletResponse.SC_OK, response.getStatus());
            JsonNode taskList = mapper.readTree(response.getEntityInputStream());
            assertTrue(taskList.has("entries"));
            var schedules = taskList.get("entries");
            assertFalse(schedules.isEmpty());
            var schedule = schedules.get(0);
            assertFalse(schedule.get("id").asText().isEmpty());
            assertFalse(schedule.get("cronExpression").asText().isEmpty());
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.restapi.test:dummy-schedule.xml")
    public void testStopAndStartScheduler() {
        try (CapturingEventListener listener = new CapturingEventListener("everySecond")) {
            await().atMost(2, TimeUnit.SECONDS).until(() -> listener.hasBeenFired("everySecond"));
            try (CloseableClientResponse response = httpClientRule.put("/management/scheduler/stop", null)) {
                assertEquals(HttpServletResponse.SC_NO_CONTENT, response.getStatus());
            }
            listener.clear();
            await().pollDelay(1200, TimeUnit.MILLISECONDS).until(() -> !listener.hasBeenFired("everySecond"));
            try (CloseableClientResponse response = httpClientRule.put("/management/scheduler/start", null)) {
                assertEquals(HttpServletResponse.SC_NO_CONTENT, response.getStatus());
            }
            listener.clear();
            await().atMost(2, TimeUnit.SECONDS).until(() -> listener.hasBeenFired("everySecond"));
        }
    }
}
