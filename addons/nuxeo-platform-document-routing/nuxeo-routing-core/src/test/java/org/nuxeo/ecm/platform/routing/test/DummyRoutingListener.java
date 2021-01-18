/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Charles Boidot <cboidot@nuxeo.com>
 */
package org.nuxeo.ecm.platform.routing.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.platform.task.TaskEventNames;

/**
 * @since 11.5
 */
public class DummyRoutingListener implements EventListener {

    @Override
    public void handleEvent(Event event) {

        event.markBubbleException();
        if (TaskEventNames.WORKFLOW_TASK_REASSIGNED.equals(event.getName())
                || TaskEventNames.WORKFLOW_TASK_DELEGATED.equals(event.getName())) {
            Map<String, Serializable> prop = event.getContext().getProperties();
            Map<String, Serializable> wfVar = (Map<String, Serializable>) prop.get(Constants.VAR_WORKFLOW);
            assertNotNull(wfVar);
            assertEquals(GraphRouteTest.DUMMY_WF_VAR, wfVar.get("stringfield"));
            Map<String, Serializable> nodeVar = (Map<String, Serializable>) prop.get(Constants.VAR_WORKFLOW_NODE);
            assertNotNull(nodeVar);
            assertEquals(GraphRouteTest.DUMMY_NODE_VAR, nodeVar.get("stringfield2"));
        }
    }

}
