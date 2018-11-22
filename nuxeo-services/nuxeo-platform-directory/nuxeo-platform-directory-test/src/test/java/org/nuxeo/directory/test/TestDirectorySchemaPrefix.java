/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */
package org.nuxeo.directory.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * Isolated in its own test as the DirectoryFeature doesn't restore correctly the directories when the schema prefix
 * changes.
 *
 * @since 9.2
 */
@RunWith(FeaturesRunner.class)
@Features(DirectoryFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@LocalDeploy({ "org.nuxeo.ecm.directory.tests:test-directories-schema-override.xml",
        "org.nuxeo.ecm.directory.tests:test-directories-bundle.xml",
        "org.nuxeo.ecm.directory.tests:test-directories-schema-prefix.xml" })
public class TestDirectorySchemaPrefix {

    private static final String USER_DIR = "userDirectory";

    private static final String SCHEMA = "user";

    @Inject
    protected DirectoryService directoryService;

    public Session getSession() throws Exception {
        return directoryService.open(USER_DIR);
    }

    @Test
    public void testSchemaWithPrefix() throws Exception {
        try (Session session = getSession()) {
            DocumentModel dm = session.getEntry("user_1");
            assertEquals("user_1", dm.getProperty(SCHEMA, "username"));

            assertTrue(session.hasEntry("user_1"));
        }
    }

    @Test
    public void testCreateEntry() throws Exception {
        try (Session session = getSession()) {
            Map<String, Object> map = new HashMap<>();
            map.put("usr:username", "user_0");
            map.put("usr:password", "pass_0");
            DocumentModel dm = session.createEntry(map);
            assertNotNull(dm);

            assertEquals("user_0", dm.getId());
            assertEquals("pass_0", dm.getProperty(SCHEMA, "password"));
        }
    }

    @Test
    public void testUpdateEntry() throws Exception {
        try (Session session = getSession()) {
            DocumentModel dm = session.getEntry("user_1");
            assertTrue(session.authenticate("user_1", "pass_1"));

            // update entry
            dm.setProperty(SCHEMA, "password", "pass_2");
            session.updateEntry(dm);

            dm = session.getEntry("user_1");
            assertTrue(session.authenticate("user_1", "pass_2"));
        }
    }

    @Test
    public void testAuthenticate() throws Exception {
        try (Session session = getSession()) {
            assertTrue(session.authenticate("Administrator", "Administrator"));
            assertTrue(session.authenticate("user_3", "pass_3"));
            assertFalse(session.authenticate("Administrator", "toto"));
            assertFalse(session.authenticate("titi", "titi"));
        }
    }

    @Test
    public void testQuery() throws Exception {
        try (Session session = getSession()) {
            Map<String, Serializable> filter = new HashMap<>();
            filter.put("username", "user_1");
            filter.put("firstName", "f");
            DocumentModelList list = session.query(filter);
            DocumentModel docModel = list.get(0);
            assertEquals("user_1", docModel.getProperty(SCHEMA, "username"));
            assertEquals("f", docModel.getProperty(SCHEMA, "firstName"));
        }
    }

}
