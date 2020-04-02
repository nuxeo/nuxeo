/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */
package org.nuxeo.segment.io.listener;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.EventBundleImpl;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.event.impl.EventImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;
import org.nuxeo.segment.io.SegmentIO;
import org.nuxeo.segment.io.SegmentIOComponent;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.segmentio.connector")
@Deploy("org.nuxeo.segmentio.connector:listener-test-contrib.xml")
public class TestSegmentIOListener {

    @Inject
    protected SegmentIO service;

    @Inject
    protected CoreSession session;

    private List<Map<String, Object>> testData;

    @Before
    public void before() {
        testData = ((SegmentIOComponent) service).getTestData();
    }

    @After
    public void after() {
        testData.clear();
    }

    @Test
    public void ensureToHandleEveryEventsInABundle() {
        EventBundle eventBundle = new EventBundleImpl();
        // Add events with ignored users
        eventBundle.push(new EventImpl("dummyEvent", buildCtx("MyAdministrator")));
        eventBundle.push(new EventImpl("dummyEvent", buildCtx("mysystem")));
        // Add events with other users
        eventBundle.push(new EventImpl("dummyEvent", buildCtx("johndoe")));
        eventBundle.push(new EventImpl("dummyEvent", buildCtx("janedoe")));

        SegmentIOAsyncListener listener = new SegmentIOAsyncListener();
        listener.handleEvent(eventBundle);

        assertEquals(2, testData.size());
    }

    @Test
    public void ensureDefaultServerUrlIsPassed() {
        EventBundle eventBundle = new EventBundleImpl();
        // Add an event with nuxeo.url not set
        eventBundle.push(new EventImpl("dummyEvent", buildCtx("johndoe")));

        SegmentIOAsyncListener listener = new SegmentIOAsyncListener();
        listener.handleEvent(eventBundle);

        assertEquals(1, testData.size());
        assertEquals("unknown server url", testData.get(0).get("url"));
    }

    @Test
    @WithFrameworkProperty(name = "nuxeo.url", value = "http://mytestserver.com")
    public void ensureServerUrlIsPassed() {
        EventBundle eventBundle = new EventBundleImpl();
        // Add an event with nuxeo.url not set
        eventBundle.push(new EventImpl("dummyEvent", buildCtx("johndoe")));

        SegmentIOAsyncListener listener = new SegmentIOAsyncListener();
        listener.handleEvent(eventBundle);

        assertEquals(1, testData.size());
        assertEquals("http://mytestserver.com", testData.get(0).get("url"));
    }

    protected EventContext buildCtx(String principalName) {
        return new EventContextImpl(session, mockPrincipal(principalName));
    }

    protected NuxeoPrincipal mockPrincipal(String name) {
        NuxeoPrincipal mockedUser = mock(NuxeoPrincipal.class);
        when(mockedUser.getName()).thenReturn(name);
        return mockedUser;
    }
}
