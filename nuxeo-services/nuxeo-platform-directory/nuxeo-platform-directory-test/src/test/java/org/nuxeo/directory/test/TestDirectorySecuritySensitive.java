/*
 * (C) Copyright 2017-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.directory.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.local.WithUser;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 9.2
 */
@RunWith(FeaturesRunner.class)
@Features(DirectoryFeature.class)
@Deploy("org.nuxeo.ecm.directory.tests:test-directories-security-sensitive.xml")
public class TestDirectorySecuritySensitive {

    protected static final String SOME_USER = "someUser";

    @Inject
    protected DirectoryService directoryService;

    @Test
    @WithUser("Administrator")
    public void administratorsCanDoEverything() {
        try (Session session = getSession("sensitive")) {
            // Read
            DocumentModelList entries = session.query(Collections.emptyMap());
            assertEquals(3, entries.size());
            assertNotNull(session.getEntry("sensitive1"));
            // Write
            Map<String, Object> rawEntry = new HashMap<>();
            rawEntry.put("id", "newEntry");
            rawEntry.put("label", "new.entry.label");
            DocumentModel entry = session.createEntry(rawEntry);
            assertEquals(4, session.query(Collections.emptyMap()).size());
            assertNotNull(session.getEntry("newEntry"));
            entry.setPropertyValue("vocabulary:label", "new.entry.label.updated");
            session.updateEntry(entry);
            DocumentModel updatedEntry = session.getEntry("newEntry");
            assertEquals("new.entry.label.updated", updatedEntry.getPropertyValue("vocabulary:label"));
            session.deleteEntry("newEntry");
            assertEquals(3, session.query(Collections.emptyMap()).size());
            assertNull(session.getEntry("newEntry"));
        }
    }

    @Test
    @WithUser(SOME_USER)
    public void nobodyCanRead() {
        try (Session session = getSession("sensitive")) {
            DocumentModelList entries = session.query(Collections.emptyMap());
            assertTrue(entries.isEmpty());
            assertNull(session.getEntry("sensitive1"));
        }
    }

    @Test(expected = DirectoryException.class)
    @WithUser(SOME_USER)
    public void nobodyCanCreateEntry() {
        try (Session session = getSession("sensitive")) {
            Map<String, Object> rawEntry = new HashMap<>();
            rawEntry.put("id", "newEntry");
            rawEntry.put("label", "new.entry.label");
            session.createEntry(rawEntry);
        }
    }

    @Test(expected = DirectoryException.class)
    @WithUser(SOME_USER)
    public void nobodyCanUpdateEntry() {
        // First get an entry as an administrator
        DocumentModel entry = Framework.doPrivileged(() -> {
            try (Session session = getSession("sensitive")) {
                DocumentModel doc = session.getEntry("sensitive1");
                assertNotNull(doc);
                return doc;
            }
        });
        try (Session session = getSession("sensitive")) {
            entry.setPropertyValue("vocabulary:label", "label.directories.sensitive.sensitive1.updated");
            session.updateEntry(entry);
        }
    }

    @Test(expected = DirectoryException.class)
    @WithUser(SOME_USER)
    public void nobodyCanDeleteEntry() {
        try (Session session = getSession("sensitive")) {
            session.deleteEntry("sensitive1");
        }
    }

    protected Session getSession(String directory) {
        return directoryService.open(directory);
    }

}
