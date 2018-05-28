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

package org.nuxeo.ecm.platform.dublincore.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_CREATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.EventImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.2
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.dublincore")
public class DublinCoreStorageServiceTest {

    @Inject
    protected DublinCoreStorageService storageService;

    @Inject
    protected CoreSession session;

    @Test
    public void shouldBeUpAndRunning() {
        assertNotNull(storageService);
    }

    @Test
    public void shouldAddOriginatingUserWhenSystemPrincipalIsUsed() {
        DocumentModel doc = session.createDocumentModel("/", "file", "File");

        Map<String, Object> originalProperties = new HashMap<>(doc.getProperties("dublincore"));
        Set<String> expectedChangedProperties = new HashSet<>();
        expectedChangedProperties.add("dc:creator");
        expectedChangedProperties.add("dc:lastContributor");
        expectedChangedProperties.add("dc:contributors");

        SystemPrincipal principal = new SystemPrincipal("Myself");

        Event event = getEventFromDocumentContext(ABOUT_TO_CREATE, session, principal);
        storageService.addContributor(doc, event);

        assertDocumentPropertiesChanges(doc, originalProperties, expectedChangedProperties);

        String principalName = principal.getOriginatingUser();

        assertEquals(principalName, doc.getPropertyValue("dc:creator"));
        assertEquals(principalName, doc.getPropertyValue("dc:lastContributor"));

        List<String> contributors = Arrays.asList((String[]) doc.getPropertyValue("dc:contributors"));
        assertEquals(1, contributors.size());
        assertTrue(contributors.contains(principalName));
    }

    @Test
    public void shouldSetContributorsAndCreatorWhenAboutToCreate() {
        DocumentModel doc = session.createDocumentModel("/", "file", "File");

        Map<String, Object> originalProperties = new HashMap<>(doc.getProperties("dublincore"));
        Set<String> expectedChangedProperties = new HashSet<>();
        expectedChangedProperties.add("dc:creator");
        expectedChangedProperties.add("dc:lastContributor");
        expectedChangedProperties.add("dc:contributors");

        Event event = getEventFromDocumentContext(ABOUT_TO_CREATE, session, session.getPrincipal());
        storageService.addContributor(doc, event);

        assertDocumentPropertiesChanges(doc, originalProperties, expectedChangedProperties);

        String principalName = session.getPrincipal().getName();

        assertEquals(principalName, doc.getPropertyValue("dc:creator"));
        assertEquals(principalName, doc.getPropertyValue("dc:lastContributor"));

        List<String> contributors = Arrays.asList((String[]) doc.getPropertyValue("dc:contributors"));
        assertEquals(1, contributors.size());
        assertTrue(contributors.contains(principalName));
    }

    @Test
    public void shouldAddContributorWhenAlreadyCreated() {
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc.setPropertyValue("dc:creator", "Original Creator");
        doc.setPropertyValue("dc:lastContributor", "Original Creator");
        doc.setPropertyValue("dc:contributors", new String[] { "Original Creator" });

        Map<String, Object> originalProperties = new HashMap<>(doc.getProperties("dublincore"));
        Set<String> expectedChangedProperties = new HashSet<>();
        expectedChangedProperties.add("dc:lastContributor");
        expectedChangedProperties.add("dc:contributors");

        Event event = getEventFromDocumentContext(BEFORE_DOC_UPDATE, session, session.getPrincipal());
        storageService.addContributor(doc, event);

        assertDocumentPropertiesChanges(doc, originalProperties, expectedChangedProperties);

        String principalName = session.getPrincipal().getName();

        assertEquals("Original Creator", doc.getPropertyValue("dc:creator"));

        List<String> contributors = Arrays.asList((String[]) doc.getPropertyValue("dc:contributors"));
        assertEquals(2, contributors.size());
        assertTrue(contributors.contains("Original Creator"));
        assertTrue(contributors.contains(principalName));
    }

    @Test
    public void shouldRemovePrefixContributorsWhenAdding() {
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc.setPropertyValue("dc:creator", "Original Creator");
        doc.setPropertyValue("dc:lastContributor", "Original Creator");
        doc.setPropertyValue("dc:contributors",
                new String[] { "Original Creator", "user:Administrator", "user:Other User" });

        Map<String, Object> originalProperties = new HashMap<>(doc.getProperties("dublincore"));
        Set<String> expectedChangedProperties = new HashSet<>();
        expectedChangedProperties.add("dc:lastContributor");
        expectedChangedProperties.add("dc:contributors");

        Event event = getEventFromDocumentContext(BEFORE_DOC_UPDATE, session, session.getPrincipal());
        storageService.addContributor(doc, event);

        assertDocumentPropertiesChanges(doc, originalProperties, expectedChangedProperties);

        String principalName = session.getPrincipal().getName();

        assertEquals(principalName, doc.getPropertyValue("dc:lastContributor"));

        List<String> contributors = Arrays.asList((String[]) doc.getPropertyValue("dc:contributors"));
        assertEquals(3, contributors.size());
        assertTrue(contributors.contains("Original Creator"));
        assertTrue(contributors.contains("Other User"));
        assertTrue(contributors.contains(principalName));
    }

    @Test
    public void shouldRemoveRepeatedContributorsWhenAdding() {
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc.setPropertyValue("dc:creator", "Administrator");
        doc.setPropertyValue("dc:lastContributor", "Administrator");
        doc.setPropertyValue("dc:contributors", new String[] { "user:Administrator", "user:Administrator",
                "user:Administrator", "user:Administrator" });

        Map<String, Object> originalProperties = new HashMap<>(doc.getProperties("dublincore"));
        Set<String> expectedChangedProperties = Collections.singleton("dc:contributors");

        Event event = getEventFromDocumentContext(BEFORE_DOC_UPDATE, session, session.getPrincipal());
        storageService.addContributor(doc, event);

        assertDocumentPropertiesChanges(doc, originalProperties, expectedChangedProperties);

        String principalName = session.getPrincipal().getName();

        List<String> contributors = Arrays.asList((String[]) doc.getPropertyValue("dc:contributors"));
        assertEquals(1, contributors.size());
        assertFalse(contributors.contains("user:Administrator"));
        assertTrue(contributors.contains(principalName));
    }

    @Test
    public void shouldDoNothingWhenAddingExistingContributor() {
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc.setPropertyValue("dc:creator", "Administrator");
        doc.setPropertyValue("dc:lastContributor", "Administrator");
        doc.setPropertyValue("dc:contributors", new String[] { "Administrator" });

        Map<String, Object> originalProperties = new HashMap<>(doc.getProperties("dublincore"));

        Event event = getEventFromDocumentContext(BEFORE_DOC_UPDATE, session, session.getPrincipal());
        storageService.addContributor(doc, event);

        assertDocumentPropertiesChanges(doc, originalProperties, new HashSet<>());
    }

    protected Event getEventFromDocumentContext(String eventName, CoreSession coreSession, Principal principal) {
        return new EventImpl(eventName,
                new DocumentEventContext(coreSession, principal, coreSession.getRootDocument()));
    }

    protected static void assertDocumentPropertiesChanges(DocumentModel doc, Map<String, Object> originalProperties,
            Set<String> expectedChangedPropertyNames) {
        originalProperties.forEach((key, value) -> {
            Object currentValue = doc.getProperty("dublincore", key);
            if (expectedChangedPropertyNames.contains(key)) {
                assertNotEquals(value, currentValue);
            } else {
                assertEquals(currentValue, value);
            }
        });
    }

}
