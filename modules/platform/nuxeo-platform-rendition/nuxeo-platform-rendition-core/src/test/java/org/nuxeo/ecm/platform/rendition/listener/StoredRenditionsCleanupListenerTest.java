/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
 *      Nuno Cunha (ncunha@nuxeo.com)
 */

package org.nuxeo.ecm.platform.rendition.listener;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventBundleImpl;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.event.impl.EventImpl;
import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.platform.rendition.service.RenditionFeature;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.mockito.RuntimeService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ RenditionFeature.class, MockitoFeature.class })
public class StoredRenditionsCleanupListenerTest {

    protected static final String EVENT_NAME = "storedRenditionsCleanup";

    @Inject
    protected EventService eventService;

    @Inject
    protected RepositoryService repositoryService;

    @Mock
    @RuntimeService
    protected RenditionService renditionService;

    @Test
    public void shouldBeRegistered() {
        String listenerName = "storedRenditionsCleanup";
        EventListenerDescriptor listener = eventService.getEventListener(listenerName);
        assertNotNull(listener);
        assertTrue(listener.acceptEvent(EVENT_NAME));
    }

    @Test
    public void shouldCallRenditionServiceWhenEventIsFired() {
        EventBundle eventBundle = new EventBundleImpl();
        eventBundle.push(new EventImpl(EVENT_NAME, new EventContextImpl()));

        eventService.fireEventBundleSync(eventBundle);

        List<String> repos = repositoryService.getRepositoryNames();
        verify(renditionService, times(repos.size())).deleteStoredRenditions(anyString());

        for (String repo : repos) {
            verify(renditionService).deleteStoredRenditions(repo);
        }
    }

}