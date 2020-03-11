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
 *     Nuno Cunha (ncunha@nuxeo.com)
 */

package org.nuxeo.ecm.platform.dublincore.listener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.nuxeo.ecm.core.api.LifeCycleConstants.TRANSITION_EVENT;
import static org.nuxeo.ecm.core.api.event.CoreEventConstants.DOCUMENT_DIRTY;
import static org.nuxeo.ecm.core.api.event.CoreEventConstants.RESET_CREATOR;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_CREATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED_BY_COPY;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_PUBLISHED;
import static org.nuxeo.ecm.core.event.Event.FLAG_INLINE;
import static org.nuxeo.ecm.platform.dublincore.constants.DublinCoreConstants.DUBLINCORE_CONTRIBUTORS_PROPERTY;
import static org.nuxeo.ecm.platform.dublincore.constants.DublinCoreConstants.DUBLINCORE_CREATOR_PROPERTY;
import static org.nuxeo.ecm.platform.dublincore.constants.DublinCoreConstants.DUBLINCORE_LAST_CONTRIBUTOR_PROPERTY;
import static org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener.DISABLE_DUBLINCORE_LISTENER;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.dublincore.service.DublinCoreStorageService;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.mockito.RuntimeService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.2
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, MockitoFeature.class })
@Deploy("org.nuxeo.ecm.platform.dublincore")
public class DublinCoreListenerTest {

    protected final List<String> events = Arrays.asList(ABOUT_TO_CREATE, BEFORE_DOC_UPDATE, TRANSITION_EVENT,
            DOCUMENT_PUBLISHED, DOCUMENT_CREATED_BY_COPY);

    @Inject
    protected CoreSession session;

    @Inject
    protected EventService eventService;

    @Mock
    @RuntimeService
    protected DublinCoreStorageService storageService;

    @Test
    public void shouldMatchRegisteredEvents() {
        EventListenerDescriptor listener = eventService.getEventListener("dclistener");

        assertTrue(events.stream().allMatch(listener::acceptEvent));
        assertEventsAreTheSame(listener.getEvents());
    }

    @Test(expected = AssertionError.class)
    @Deploy("org.nuxeo.ecm.platform.dublincore:OSGI-INF/events/single-event-contrib.xml")
    public void shouldFailWhenDifferentNumberOfEventsAreRegistered() {
        EventListenerDescriptor listener = eventService.getEventListener("single");
        assertEventsAreTheSame(listener.getEvents());
    }

    @Test(expected = AssertionError.class)
    @Deploy("org.nuxeo.ecm.platform.dublincore:OSGI-INF/events/same-number-of-events-contrib.xml")
    public void shouldFailWhenEventsAreRegisteredButNotTested() {
        EventListenerDescriptor listener = eventService.getEventListener("same");
        assertEventsAreTheSame(listener.getEvents());
    }

    @Test
    public void shouldDoNothingWhenEventContextIsNotDocument() {
        EventContext ctx = new EventContextImpl(session, session.getPrincipal());
        eventService.fireEvent(ctx.newEvent(ABOUT_TO_CREATE));

        verifyZeroInteractions(storageService);
    }

    @Test
    public void shouldDoNothingWhenUnknownEventIsReceived() {
        DocumentModel doc = session.createDocumentModel("File");
        DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), doc);
        eventService.fireEvent(ctx.newEvent("UnknownEvent"));

        verifyZeroInteractions(storageService);
    }

    @Test
    public void shouldDoNothingWhenListenerIsDisabled() {
        DocumentModel doc = session.createDocumentModel("File");

        Map<String, Serializable> properties = new HashMap<>();
        properties.put(DISABLE_DUBLINCORE_LISTENER, true);

        DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), doc);
        ctx.setProperties(properties);

        eventService.fireEvent(ctx.newEvent(ABOUT_TO_CREATE, FLAG_INLINE));

        verifyZeroInteractions(storageService);
    }

    @Test
    public void shouldDoNothingWhenDocumentIsVersion() {
        DocumentModel doc = Mockito.mock(DocumentModel.class);
        when(doc.isVersion()).thenReturn(true);

        DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), doc);
        eventService.fireEvent(ctx.newEvent(ABOUT_TO_CREATE, FLAG_INLINE));

        verifyZeroInteractions(storageService);
    }

    @Test
    public void shouldDoNothingWhenDocumentHasSystemDocumentFacet() {
        DocumentModel doc = Mockito.mock(DocumentModel.class);
        when(doc.hasFacet("SystemDocument")).thenReturn(true);

        DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), doc);
        eventService.fireEvent(ctx.newEvent(ABOUT_TO_CREATE, FLAG_INLINE));

        verifyZeroInteractions(storageService);
    }

    @Test
    public void shouldDoNothingWhenDocumentIsProxyAndEventIsAboutToCreate() {
        DocumentModel doc = Mockito.mock(DocumentModel.class);
        when(doc.isProxy()).thenReturn(true);

        DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), doc);
        eventService.fireEvent(ctx.newEvent(ABOUT_TO_CREATE, FLAG_INLINE));

        verifyZeroInteractions(storageService);
    }

    @Test
    public void shouldDoNothingWhenDocumentIsProxyAndIsImmutable() {
        DocumentModel doc = Mockito.mock(DocumentModel.class);
        when(doc.isProxy()).thenReturn(true);
        when(doc.isImmutable()).thenReturn(true);

        DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), doc);
        eventService.fireEvent(ctx.newEvent(ABOUT_TO_CREATE, FLAG_INLINE));

        verifyZeroInteractions(storageService);
    }

    @Test
    public void shouldInvokeStorageServiceWhenEventIsAboutToCreate() {
        DocumentModel doc = session.createDocumentModel("File");

        DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), doc);
        Event event = ctx.newEvent(ABOUT_TO_CREATE, FLAG_INLINE);
        eventService.fireEvent(event);

        Calendar expectedDate = Calendar.getInstance();
        expectedDate.setTime(new Date(event.getTime()));

        verify(storageService).setCreationDate(eq(doc), eq(expectedDate));
        verify(storageService).setModificationDate(eq(doc), eq(expectedDate));
        verify(storageService).addContributor(eq(doc), eq(event));
    }

    @Test
    public void shouldInvokeStorageServiceButNotSetCreationDateWhenEventIsBeforeUpdateAndDocumentIsDirty() {
        DocumentModel doc = Mockito.mock(DocumentModel.class);
        when(doc.isDirty()).thenReturn(true);

        Map<String, Serializable> properties = new HashMap<>();
        properties.put(DOCUMENT_DIRTY, true);

        DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), doc);
        ctx.setProperties(properties);

        Event event = ctx.newEvent(BEFORE_DOC_UPDATE, FLAG_INLINE);
        eventService.fireEvent(event);

        Calendar expectedDate = Calendar.getInstance();
        expectedDate.setTime(new Date(event.getTime()));

        verify(storageService, never()).setCreationDate(eq(doc), eq(expectedDate));
        verify(storageService).setModificationDate(eq(doc), eq(expectedDate));
        verify(storageService).addContributor(eq(doc), eq(event));
    }

    @Test
    public void shouldInvokeStorageServiceAndResetContributorsWhenEventIsCreatedByCopy() {
        DocumentModel doc = session.createDocumentModel("File");

        Map<String, Serializable> properties = new HashMap<>();
        properties.put(RESET_CREATOR, true);

        DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), doc);
        ctx.setProperties(properties);

        Event event = ctx.newEvent(DOCUMENT_CREATED_BY_COPY, FLAG_INLINE);
        eventService.fireEvent(event);

        Calendar expectedDate = Calendar.getInstance();
        expectedDate.setTime(new Date(event.getTime()));

        assertNull(doc.getPropertyValue(DUBLINCORE_CREATOR_PROPERTY));
        assertNull(doc.getPropertyValue(DUBLINCORE_CONTRIBUTORS_PROPERTY));
        assertNull(doc.getPropertyValue(DUBLINCORE_LAST_CONTRIBUTOR_PROPERTY));

        verify(storageService).setCreationDate(eq(doc), eq(expectedDate));
        verify(storageService).setModificationDate(eq(doc), eq(expectedDate));
        verify(storageService).addContributor(eq(doc), eq(event));
    }

    @Test
    public void shouldInvokeStorageServiceButNotSetCreationDateWhenEventIsTransitionAndDocumentIsNotImmutable() {
        DocumentModel doc = Mockito.mock(DocumentModel.class);
        when(doc.isImmutable()).thenReturn(false);

        DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), doc);
        Event event = ctx.newEvent(TRANSITION_EVENT, FLAG_INLINE);
        eventService.fireEvent(event);

        Calendar expectedDate = Calendar.getInstance();
        expectedDate.setTime(new Date(event.getTime()));

        verify(storageService, never()).setCreationDate(eq(doc), eq(expectedDate));
        verify(storageService).setModificationDate(eq(doc), eq(expectedDate));
        verify(storageService).addContributor(eq(doc), eq(event));
    }

    protected void assertEventsAreTheSame(Set<String> contributionEvents) {
        assertEquals(events.size(), contributionEvents.size());
        events.forEach(event -> {
            if (!contributionEvents.contains(event)) {
                fail(String.format(
                        "The expected event '%s' is not present on the events registered by the contribution: [%s]",
                        event, String.join(",", contributionEvents)));
            }
        });
    }
}
