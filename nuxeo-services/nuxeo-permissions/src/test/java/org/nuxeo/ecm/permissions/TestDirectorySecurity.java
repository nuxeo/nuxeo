/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.permissions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.NuxeoLoginContext;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Test that unrestricted access to the underlying directories is not possible.
 *
 * @since 8.4
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy("org.nuxeo.ecm.permissions")
public class TestDirectorySecurity {

    private static final String DIR_NAME = "aceinfo";

    private static final String SCHEMA_NAME = "aceinfo";

    private static final String ID_FIELD = "id";

    @Inject
    public DirectoryService directoryService;

    protected Serializable entryId;

    @Before
    public void setUp() throws Exception {
        // as system, create an dummy entry
        Framework.doPrivileged(() -> {
            try (Session session = directoryService.open(DIR_NAME)) {
                DocumentModel entry = session.createEntry(
                        new HashMap<>(Collections.singletonMap(SCHEMA_NAME + ":" + ID_FIELD, "123")));
                entryId = entry.getPropertyValue(SCHEMA_NAME + ":" + ID_FIELD);
            }
        });
    }

    @Test
    public void testDirectoryRead() throws Exception {
        // as system, we see the entry
        Framework.doPrivileged(() -> {
            try (Session session = directoryService.open(DIR_NAME)) {
                DocumentModel entry = session.getEntry(entryId.toString());
                assertNotNull(entry); // visible entry
            }
        });

        // as a random user, we don't see the entry
        try (NuxeoLoginContext loginContext = Framework.loginUser("aRandomUser");
                Session session = directoryService.open(DIR_NAME)) {
            DocumentModel entry = session.getEntry(entryId.toString());
            assertNull(entry); // hidden entry
        }
    }

    @Test
    public void testDirectoryQuery() throws Exception {
        // as system, we see the entry
        Framework.doPrivileged(() -> {
            try (Session session = directoryService.open(DIR_NAME)) {
                DocumentModelList results = session.query(Collections.singletonMap(ID_FIELD, entryId));
                assertEquals(1, results.size()); // visible entry
            }
        });

        // as a random user, we don't see the entry
        try (NuxeoLoginContext loginContext = Framework.loginUser("aRandomUser");
                Session session = directoryService.open(DIR_NAME)) {
            DocumentModelList results = session.query(Collections.singletonMap(ID_FIELD, entryId));
            assertEquals(0, results.size()); // hidden entry
        }
    }

}
