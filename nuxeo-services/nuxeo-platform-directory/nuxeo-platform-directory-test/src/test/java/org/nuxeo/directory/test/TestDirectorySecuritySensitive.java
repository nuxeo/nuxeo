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
import javax.security.auth.login.LoginException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature;
import org.nuxeo.ecm.platform.login.test.DummyNuxeoLoginModule;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 9.2
 */
@RunWith(FeaturesRunner.class)
@Features({ DirectoryFeature.class, ClientLoginFeature.class })
@Deploy("org.nuxeo.ecm.directory.tests:test-directories-security-sensitive.xml")
public class TestDirectorySecuritySensitive {

    protected static final String SOME_USER = "someUser";

    @Inject
    protected ClientLoginFeature dummyLogin;

    @Inject
    protected DirectoryService directoryService;

    @Test
    public void administratorsCanDoEverything() throws LoginException {
        dummyLogin.login(DummyNuxeoLoginModule.ADMINISTRATOR_USERNAME);
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
        } finally {
            dummyLogin.logout();
        }
    }

    @Test
    public void nobodyCanRead() throws LoginException {
        dummyLogin.login(SOME_USER);
        try (Session session = getSession("sensitive")) {
            DocumentModelList entries = session.query(Collections.emptyMap());
            assertTrue(entries.isEmpty());
            assertNull(session.getEntry("sensitive1"));
        } finally {
            dummyLogin.logout();
        }
    }

    @Test(expected = DirectoryException.class)
    public void nobodyCanCreateEntry() throws LoginException {
        dummyLogin.login(SOME_USER);
        try (Session session = getSession("sensitive")) {
            Map<String, Object> rawEntry = new HashMap<>();
            rawEntry.put("id", "newEntry");
            rawEntry.put("label", "new.entry.label");
            session.createEntry(rawEntry);
        } finally {
            dummyLogin.logout();
        }
    }

    @Test(expected = DirectoryException.class)
    public void nobodyCanUpdateEntry() throws LoginException {
        // First get an entry as an administrator
        DocumentModel entry;
        dummyLogin.login(DummyNuxeoLoginModule.ADMINISTRATOR_USERNAME);
        try (Session session = getSession("sensitive")) {
            entry = session.getEntry("sensitive1");
            assertNotNull(entry);
        } finally {
            dummyLogin.logout();
        }
        dummyLogin.login(SOME_USER);
        try (Session session = getSession("sensitive")) {
            entry.setPropertyValue("vocabulary:label", "label.directories.sensitive.sensitive1.updated");
            session.updateEntry(entry);
        } finally {
            dummyLogin.logout();
        }
    }

    @Test(expected = DirectoryException.class)
    public void nobodyCanDeleteEntry() throws LoginException {
        dummyLogin.login(SOME_USER);
        try (Session session = getSession("sensitive")) {
            session.deleteEntry("sensitive1");
        } finally {
            dummyLogin.logout();
        }
    }

    protected Session getSession(String directory) {
        return directoryService.open(directory);
    }

}
