/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.user.preferences.directory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.user.preferences.directory.UserPreferencesServiceImpl.DIRECTORY_NAME;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.security.auth.login.LoginException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.NuxeoLoginContext;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 2025.17
 */
@RunWith(FeaturesRunner.class)
@Features(UserPreferencesFeature.class)
public class TestUserPreferencesDirectorySecurity {

    @Inject
    public DirectoryService directoryService;

    protected String directoryIdField;

    protected String directorySchema;

    protected Serializable entryId;

    @Before
    public void setUp() {
        directoryIdField = directoryService.getDirectoryIdField(DIRECTORY_NAME);
        directorySchema = directoryService.getDirectorySchema(DIRECTORY_NAME);
        // as system, create a dummy entry
        Framework.doPrivileged(() -> {
            try (Session dirSession = directoryService.open(DIRECTORY_NAME)) {
                var entry = dirSession.createEntry(new HashMap<>());
                entryId = entry.getPropertyValue(directorySchema + ":" + directoryIdField);
            }
        });
    }

    @Test
    public void testDirectoryRead() throws LoginException {
        // as system, we see the entry
        Framework.doPrivileged(() -> {
            try (Session session = directoryService.open(DIRECTORY_NAME)) {
                DocumentModel entry = session.getEntry(entryId.toString());
                assertNotNull(entry); // visible entry
            }
        });

        // as a random user, we don't see the entry
        try (NuxeoLoginContext ignored = Framework.loginUser("aRandomUser");
                Session session = directoryService.open(DIRECTORY_NAME)) {
            DocumentModel entry = session.getEntry(entryId.toString());
            assertNull(entry); // hidden entry
        }
    }

    @Test
    public void testDirectoryQuery() throws LoginException {
        // as system, we see the entry
        Framework.doPrivileged(() -> {
            try (Session session = directoryService.open(DIRECTORY_NAME)) {
                DocumentModelList results = session.query(Map.of(directoryIdField, entryId));
                assertEquals(1, results.size()); // visible entry
            }
        });

        // as a random user, we don't see the entry
        try (NuxeoLoginContext ignored = Framework.loginUser("aRandomUser");
                Session session = directoryService.open(DIRECTORY_NAME)) {
            DocumentModelList results = session.query(Map.of(directoryIdField, entryId));
            assertTrue(results.isEmpty()); // hidden entry
        }
    }
}
