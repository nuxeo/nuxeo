/*
 * (C) Copyright 2006-2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *     Anahide Tchertchian
 */
package org.nuxeo.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * Test component manager events
 *
 * @since 9.2
 * @author bogdan
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.runtime.test.tests:component-manager-listener.xml")
@Deploy("org.nuxeo.runtime.test.tests:component-manager-listener-dep.xml")
public class TestComponentManager {

    protected MyListener listener = new MyListener();

    @Test
    public void testManagerEvents() throws Exception {
        ComponentManager mgr = Framework.getRuntime().getComponentManager();
        mgr.addListener(listener);
        listener.assertCounters(0, 0, 0, 0);
        mgr.restart(false);
        listener.assertCounters(1, 1, 1, 1);
        mgr.restart(true);
        listener.assertCounters(2, 2, 2, 2);
        mgr.refresh(true);
        listener.assertCounters(2, 2, 2, 2);
        mgr.stop();
        listener.assertCounters(2, 2, 3, 3);
        mgr.start();
        listener.assertCounters(3, 3, 3, 3);
    }

    protected static class EventsInfo {

        public int beforeStop = 0;

        public int afterStop = 0;

        public int beforeStart = 0;

        public int afterStart = 0;

    }

    protected static class MyListener implements ComponentManager.Listener {

        public EventsInfo info = new EventsInfo();

        @Override
        public void beforeStop(ComponentManager mgr, boolean isStandby) {
            info.beforeStop++;
        }

        @Override
        public void beforeStart(ComponentManager mgr, boolean isResume) {
            info.beforeStart++;
        }

        @Override
        public void afterStop(ComponentManager mgr, boolean isStandby) {
            info.afterStop++;
        }

        @Override
        public void afterStart(ComponentManager mgr, boolean isResume) {
            info.afterStart++;
        }

        public void assertCounters(int beforeStart, int afterStart, int beforeStop, int afterStop) {
            Assert.assertEquals(beforeStart, info.beforeStart);
            Assert.assertEquals(afterStart, info.afterStart);
            Assert.assertEquals(beforeStop, info.beforeStop);
            Assert.assertEquals(afterStop, info.afterStop);
        }
    }

    @Test
    public void testComponentListener() {
        String mockComponentName = "component.manager.listener";
        Object component = Framework.getRuntime().getComponent(mockComponentName);
        assertTrue(component instanceof MockComponentManagerListener);
        MockComponentManagerListener mockComponent = (MockComponentManagerListener) component;
        List<ComponentEvent> events = mockComponent.getEvents();
        assertFalse(events.isEmpty());
        ComponentEvent firstEvent = events.get(0);
        assertEquals("ACTIVATING_COMPONENT: service:org.nuxeo.runtime.EventService", firstEvent.toString());
        for (String testedComp : Arrays.asList("org.nuxeo.runtime.EventService", mockComponentName)) {
            for (int event : Arrays.asList(ComponentEvent.ACTIVATING_COMPONENT, ComponentEvent.COMPONENT_ACTIVATED,
                    ComponentEvent.STARTING_COMPONENT, ComponentEvent.COMPONENT_STARTED)) {
                assertTrue(
                        String.format("No event %s for component %s", ComponentEvent.getEventName(event), testedComp),
                        mockComponent.hasEvent(event, testedComp));
            }
        }

        // too late
        assertFalse(mockComponent.hasEvent(ComponentEvent.COMPONENT_REGISTERED, mockComponentName));
        assertFalse(mockComponent.hasEvent(ComponentEvent.COMPONENT_RESOLVED, mockComponentName));
        // check extension registration event
        assertTrue(mockComponent.hasEvent(ComponentEvent.EXTENSION_REGISTERED,
                "org.nuxeo.runtime.trackers.files.threadstracking.config"));

        // check pending extension registration
        assertTrue(mockComponent.hasEvent(ComponentEvent.EXTENSION_PENDING, "component.manager.listener"));
        assertTrue(mockComponent.hasEvent(ComponentEvent.EXTENSION_REGISTERED, "component.manager.listener"));
    }

}
