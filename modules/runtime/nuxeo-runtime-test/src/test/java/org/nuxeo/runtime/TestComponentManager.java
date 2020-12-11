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
 *     Bogdan Stefanescu
 *     Anahide Tchertchian
 */
package org.nuxeo.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.runtime.MockComponentManagerListener.NAME;

import java.util.List;

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
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.runtime.test.tests:component-manager-listener.xml")
@Deploy("org.nuxeo.runtime.test.tests:component-manager-listener-dep.xml")
@Deploy("org.nuxeo.runtime.test.tests:component-manager-listener2.xml")
public class TestComponentManager {

    protected MyListener listener = new MyListener();

    public void checkCounters(MockEventsInfo info, int beforeStart, int afterStart, int beforeStop, int afterStop) {
        assertEquals(beforeStart, info.beforeStart);
        assertEquals(afterStart, info.afterStart);
        assertEquals(beforeStop, info.beforeStop);
        assertEquals(afterStop, info.afterStop);
    }

    protected MockEventsInfo checkMockComponentManagerListener2(boolean isNull) {
        // this listener should be automatically registered
        MockComponentManagerListener2 listener2 = (MockComponentManagerListener2) Framework.getRuntime()
                                                                                           .getComponent(
                                                                                                   MockComponentManagerListener2.NAME);
        if (isNull) {
            assertNull(listener2);
            return null;
        } else {
            assertNotNull(listener2);
            return listener2.info;
        }
    }

    public void checkCounters2(int beforeStart, int afterStart, int beforeStop, int afterStop) {
        MockEventsInfo info = checkMockComponentManagerListener2(false);
        checkCounters(info, beforeStart, afterStart, beforeStop, afterStop);
    }

    @Test
    public void testManagerEvents() throws Exception {
        ComponentManager mgr = Framework.getRuntime().getComponentManager();
        mgr.addListener(listener);
        checkCounters(listener.info, 0, 0, 0, 0);
        checkCounters2(1, 1, 0, 0);
        mgr.restart(false);
        checkCounters(listener.info, 1, 1, 1, 1);
        checkCounters2(1, 1, 0, 0);
        mgr.restart(true);
        checkCounters(listener.info, 2, 2, 2, 2);
        checkCounters2(1, 1, 0, 0);
        mgr.refresh(true);
        checkCounters(listener.info, 2, 2, 2, 2);
        checkCounters2(1, 1, 0, 0);
        mgr.stop();
        checkCounters(listener.info, 2, 2, 3, 3);
        checkMockComponentManagerListener2(true);
        mgr.start();
        checkCounters(listener.info, 3, 3, 3, 3);
        checkCounters2(1, 1, 0, 0);
    }

    protected static class MyListener implements ComponentManager.Listener {

        public MockEventsInfo info = new MockEventsInfo();

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

    }

    @Test
    public void testComponentListener() {
        Object component = Framework.getRuntime().getComponent(NAME);
        assertTrue(component instanceof MockComponentManagerListener);
        MockComponentManagerListener mockComponent = (MockComponentManagerListener) component;
        List<ComponentEvent> events = mockComponent.getEvents();
        assertFalse(events.isEmpty());
        ComponentEvent firstEvent = events.get(0);
        assertEquals("ACTIVATING_COMPONENT: service:org.nuxeo.runtime.EventService", firstEvent.toString());
        for (String testedComp : List.of("org.nuxeo.runtime.EventService", NAME)) {
            for (int event : List.of(ComponentEvent.ACTIVATING_COMPONENT, ComponentEvent.COMPONENT_ACTIVATED,
                    ComponentEvent.STARTING_COMPONENT, ComponentEvent.COMPONENT_STARTED)) {
                assertTrue(
                        String.format("No event %s for component %s", ComponentEvent.getEventName(event), testedComp),
                        mockComponent.hasEvent(event, testedComp));
            }
        }

        // too late
        assertFalse(mockComponent.hasEvent(ComponentEvent.COMPONENT_REGISTERED, NAME));
        assertFalse(mockComponent.hasEvent(ComponentEvent.COMPONENT_RESOLVED, NAME));
        // check extension registration event
        assertTrue(mockComponent.hasEvent(ComponentEvent.EXTENSION_REGISTERED,
                "org.nuxeo.runtime.trackers.files.threadstracking.config"));

        // check pending extension registration
        assertTrue(mockComponent.hasEvent(ComponentEvent.EXTENSION_PENDING, NAME));
        assertTrue(mockComponent.hasEvent(ComponentEvent.EXTENSION_REGISTERED, NAME));
    }

}
