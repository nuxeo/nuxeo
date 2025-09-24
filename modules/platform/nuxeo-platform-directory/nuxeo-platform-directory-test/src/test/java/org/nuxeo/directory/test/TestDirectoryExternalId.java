/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.directory.test;

import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.directory.api.DirectoryConstants.SYSTEM_ID_PROPERTY;
import static org.nuxeo.ecm.directory.api.DirectoryConstants.SYSTEM_SCHEMA;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.local.WithUser;
import org.nuxeo.ecm.core.api.model.ReadOnlyPropertyException;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 2025.9
 */
@RunWith(FeaturesRunner.class)
@Features(DirectoryFeature.class)
@Deploy("org.nuxeo.ecm.directory.tests:test-directories-bundle-externalId-config.xml")
public class TestDirectoryExternalId extends AbstractDirectoryTest {

    @Test
    public void testCreateEntryGenerateSystemId() throws Exception {
        try (Session session = getSession()) {
            Map<String, Object> map = new HashMap<>();
            map.put("username", "user_0");
            map.put("password", "pass_0");
            DocumentModel dm = session.createEntry(map);
            assertNotNull(dm);

            assertEquals("user_0", dm.getId());

            String[] schemaNames = dm.getSchemas();
            assertEquals(2, schemaNames.length);
            assertTrue(ArrayUtils.contains(schemaNames, SCHEMA));
            assertTrue(ArrayUtils.contains(schemaNames, SYSTEM_SCHEMA));

            assertNotNull(dm.getPropertyValue(SYSTEM_ID_PROPERTY));
            assertEquals("user_0", dm.getProperty(SCHEMA, "username"));
            assertEquals("pass_0", dm.getProperty(SCHEMA, "password"));
        }
    }

    @Test
    public void testCreateEntryWithSystemId() throws Exception {
        try (Session session = getSession()) {
            Map<String, Object> map = new HashMap<>();
            map.put("sys:id", "user_0");
            map.put("username", "user_0");
            map.put("password", "pass_0");
            DocumentModel dm = session.createEntry(map);
            assertNotNull(dm);

            assertEquals("user_0", dm.getId());

            String[] schemaNames = dm.getSchemas();
            assertEquals(2, schemaNames.length);
            assertTrue(ArrayUtils.contains(schemaNames, SCHEMA));
            assertTrue(ArrayUtils.contains(schemaNames, SYSTEM_SCHEMA));

            assertEquals("user_0", dm.getPropertyValue(SYSTEM_ID_PROPERTY));
            assertEquals("user_0", dm.getProperty(SCHEMA, "username"));
            assertEquals("pass_0", dm.getProperty(SCHEMA, "password"));
        }
    }

    @Test
    public void testCreateEntryWithSystemIdKeepsIdsUnicity() throws Exception {
        try (Session session = getSession()) {
            // set the sys:id to the id of another entry
            Map<String, Object> map = new HashMap<>();
            map.put("sys:id", "user_3");
            map.put("username", "user_0");
            map.put("password", "pass_0");
            var e = assertThrows(DirectoryException.class, () -> session.createEntry(map));
            assertEquals("Entry with id user_0 or sys:id user_3 already exists in directory userDirectory",
                    e.getMessage());
            assertEquals(SC_CONFLICT, e.getStatusCode());
        }
    }

    @Test
    public void testGetEntryWithSystemId() throws Exception {
        try (Session session = getSession()) {
            // first retrieve user to get its sys:id
            DocumentModel dm = session.getEntry("user_1");
            String sysId = (String) dm.getPropertyValue(SYSTEM_ID_PROPERTY);
            assertNotNull(sysId);

            DocumentModel dmWithSysId = session.getEntry(sysId);
            assertNotNull(dmWithSysId);
            assertEquals(dm.getId(), dmWithSysId.getId());
            assertEquals(dm.getProperty(SYSTEM_SCHEMA, "id"), dmWithSysId.getProperty(SYSTEM_SCHEMA, "id"));
            assertEquals(dm.getProperty(SCHEMA, "username"), dmWithSysId.getProperty(SCHEMA, "username"));
            assertEquals(dm.getProperty(SCHEMA, "password"), dmWithSysId.getProperty(SCHEMA, "password"));
        }
    }

    @Test
    @WithUser("Administrator")
    public void testUpdateEntryWithSystemId() throws Exception {
        try (Session session = getSession()) {
            // check that model has a system id
            DocumentModel dm = session.getEntry("user_1");
            assertNotNull(dm.getPropertyValue(SYSTEM_ID_PROPERTY));

            // update the system id
            dm.setPropertyValue(SYSTEM_ID_PROPERTY, "dummy");
            session.updateEntry(dm);

            // refresh model
            dm = session.getEntry("user_1");
            assertEquals("dummy", dm.getPropertyValue(SYSTEM_ID_PROPERTY));
        }
    }

    @Test
    @WithUser("aUser")
    public void testUpdateEntryWithSystemIdImpossibleForNonAdministrator() throws Exception {
        try (Session session = getSession()) {
            // check that model has a system id
            DocumentModel dm = session.getEntry("user_1");
            assertNotNull(dm.getPropertyValue(SYSTEM_ID_PROPERTY));

            // update the system id
            var e = assertThrows(ReadOnlyPropertyException.class,
                    () -> dm.setPropertyValue(SYSTEM_ID_PROPERTY, "dummy"));
            assertEquals("Cannot set the value of property: sys:id since it is readonly", e.getMessage());
        }
    }

    @Test
    @WithUser("Administrator")
    public void testUpdateEntryWithSystemIdKeepsIdsUnicity() throws Exception {
        try (Session session = getSession()) {
            // update the sys:id to the id of another entry
            DocumentModel dm = session.getEntry("user_1");
            dm.setPropertyValue(SYSTEM_ID_PROPERTY, "user_3");
            var e = assertThrows(DirectoryException.class, () -> session.updateEntry(dm));
            assertEquals("Entry with sys:id user_3 already exists in directory userDirectory", e.getMessage());
            assertEquals(SC_CONFLICT, e.getStatusCode());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDeleteEntryWithSystemId() throws Exception {
        String sysId;
        try (Session session = getSession()) {
            DocumentModel dm = session.getEntry("user_1");
            sysId = (String) dm.getPropertyValue(SYSTEM_ID_PROPERTY);
            session.deleteEntry(sysId);
        }

        try (Session session = getSession()) {
            assertNull(session.getEntry("user_1"));
            assertNull(session.getEntry(sysId));
        }

        try (Session session = directoryService.open(GROUP_DIR)) {
            DocumentModel group1 = session.getEntry("group_1");
            List<String> members = (List<String>) group1.getProperty("group", "members");
            assertTrue(members.isEmpty());
        }
    }
}
