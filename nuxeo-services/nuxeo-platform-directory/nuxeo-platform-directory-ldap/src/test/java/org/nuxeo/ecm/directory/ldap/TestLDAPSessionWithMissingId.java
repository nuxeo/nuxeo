/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id$
 */

package org.nuxeo.ecm.directory.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * Tests for NXP-2461: Manage LDAP directories with missing entries for identifier field.
 *
 * @author Anahide Tchertchian
 */
//override the default server setup
@Deploy("org.nuxeo.ecm.directory.ldap.tests:TestDirectoriesWithInternalApacheDS-override.xml")
public class TestLDAPSessionWithMissingId extends LDAPDirectoryTestCase {

    protected static final String USER_SCHEMANAME = "user";

    protected static final String GROUP_SCHEMANAME = "group";

    // override tests to get specific use cases

    @SuppressWarnings("unchecked")
    @Test
    public void testGetEntry() throws Exception {
        try (Session session = getLDAPDirectory("userDirectory").getSession()) {
            DocumentModel entry = session.getEntry("Administrator");
            assertNull(entry);

            entry = session.getEntry("ogrisel+Administrator@nuxeo.com");
            assertEquals("ogrisel+Administrator@nuxeo.com", entry.getId());
            assertEquals("Administrator", entry.getProperty(USER_SCHEMANAME, "username"));
            assertEquals("Manager", entry.getProperty(USER_SCHEMANAME, "lastName"));

            if (isExternalServer()) {
                assertEquals(Long.valueOf(1), entry.getProperty(USER_SCHEMANAME, "intField"));
            }
            // assertNull(entry.getProperty(USER_SCHEMANAME, "sn"));
            assertEquals("Administrator", entry.getProperty(USER_SCHEMANAME, "firstName"));
            // assertNull(entry.getProperty(USER_SCHEMANAME, "givenName"));
            // assertNull(entry.getProperty(USER_SCHEMANAME, "cn"));
            assertNull(entry.getProperty(USER_SCHEMANAME, "password"));
            // assertNull(entry.getProperty(USER_SCHEMANAME, "userPassword"));

            List<String> val = (List<String>) entry.getProperty(USER_SCHEMANAME, "employeeType");
            assertTrue(val.isEmpty());

            if (isExternalServer()) {
                // LDAP references do not work with the internal test server
                List<String> groups = (List<String>) entry.getProperty(USER_SCHEMANAME, "groups");
                assertEquals(2, groups.size());
                assertTrue(groups.contains("members"));
                assertTrue(groups.contains("administrators"));
            }

            DocumentModel entry2 = session.getEntry("ogrisel+user1@nuxeo.com");
            assertNotNull(entry2);
            assertEquals("ogrisel+user1@nuxeo.com", entry2.getId());
            assertEquals("user1", entry2.getProperty(USER_SCHEMANAME, "username"));
            assertEquals("One", entry2.getProperty(USER_SCHEMANAME, "lastName"));
            assertEquals("User", entry2.getProperty(USER_SCHEMANAME, "firstName"));
            assertNull(entry2.getProperty(USER_SCHEMANAME, "password"));
            // assertNull(entry2.getProperty(USER_SCHEMANAME, "userPassword"));
            assertEquals(Arrays.asList("Boss"), entry2.getProperty(USER_SCHEMANAME, "employeeType"));

            if (isExternalServer()) {
                // default value for missing attribute
                assertEquals(Long.valueOf(0), entry2.getProperty(USER_SCHEMANAME, "intField"));

                // LDAP references do not work with the internal test server
                if (hasDynGroupSchema()) {
                    assertEquals(Arrays.asList("dyngroup1", "dyngroup2", "members", "subgroup"),
                            entry2.getProperty(USER_SCHEMANAME, "groups"));
                } else {
                    assertEquals(Arrays.asList("members", "subgroup"), entry2.getProperty(USER_SCHEMANAME, "groups"));
                }

            }

            DocumentModel entry3 = session.getEntry("UnexistingEntry");
            assertNull(entry3);

            // test special character escaping
            if (isExternalServer()) {
                // for some reason this do not work with the internal
                // ApacheDS server (bug?)
                DocumentModel entry4 = session.getEntry("Admi*");
                assertNull(entry4);

                DocumentModel entry5 = session.getEntry("");
                assertNull(entry5);

                DocumentModel entry6 = session.getEntry("(objectClass=*)");
                assertNull(entry6);
            }

        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetEntry2() {
        try (Session session = getLDAPDirectory("groupDirectory").getSession()) {
            DocumentModel entry = session.getEntry("administrators");
            assertNotNull(entry);
            assertEquals("administrators", entry.getId());
            assertEquals("administrators", entry.getProperty(GROUP_SCHEMANAME, "groupname"));

            if (isExternalServer()) {
                // LDAP references do not work with the internal test server
                List<String> members = (List<String>) entry.getProperty(GROUP_SCHEMANAME, "members");
                assertNotNull(members);
                assertEquals(1, members.size());
                assertTrue(members.contains("ogrisel+Administrator@nuxeo.com"));

                List<String> subGroups = (List<String>) entry.getProperty(GROUP_SCHEMANAME, "subGroups");
                assertNotNull(subGroups);
                assertEquals(0, subGroups.size());

                List<String> parentGroups = (List<String>) entry.getProperty(GROUP_SCHEMANAME, "parentGroups");
                assertNotNull(parentGroups);
                assertEquals(0, parentGroups.size());
            }

            entry = session.getEntry("members");
            assertNotNull(entry);
            assertEquals("members", entry.getId());
            assertEquals("members", entry.getProperty(GROUP_SCHEMANAME, "groupname"));

            if (isExternalServer()) {
                // LDAP references do not work with the internal test server
                List<String> members = (List<String>) entry.getProperty(GROUP_SCHEMANAME, "members");
                assertEquals(2, members.size());
                assertTrue(members.contains("ogrisel+Administrator@nuxeo.com"));
                assertTrue(members.contains("ogrisel+user1@nuxeo.com"));

                List<String> subGroups = (List<String>) entry.getProperty(GROUP_SCHEMANAME, "subGroups");
                assertEquals(1, subGroups.size());
                assertTrue(subGroups.contains("submembers"));

                List<String> parentGroups = (List<String>) entry.getProperty(GROUP_SCHEMANAME, "parentGroups");
                assertEquals(0, parentGroups.size());
            }

            entry = session.getEntry("submembers");
            assertNotNull(entry);
            assertEquals("submembers", entry.getId());
            assertEquals("submembers", entry.getProperty(GROUP_SCHEMANAME, "groupname"));

            if (isExternalServer()) {
                // LDAP references do not work with the internal test server
                assertEquals(Arrays.asList(), entry.getProperty(GROUP_SCHEMANAME, "members"));
                assertEquals(Arrays.asList(), entry.getProperty(GROUP_SCHEMANAME, "subGroups"));

                if (hasDynGroupSchema()) {
                    assertEquals(Arrays.asList("dyngroup1", "members"),
                            entry.getProperty(GROUP_SCHEMANAME, "parentGroups"));
                } else {
                    assertEquals(Arrays.asList("members"), entry.getProperty(GROUP_SCHEMANAME, "parentGroups"));
                }
            }

        }
    }

    @Test
    public void testQuery1() {
        try (Session session = getLDAPDirectory("userDirectory").getSession()) {
            Map<String, Serializable> filter = new HashMap<>();
            filter.put("username", "user");
            Set<String> fulltext = new HashSet<>();
            fulltext.add("username");
            DocumentModelList entries = session.query(filter, fulltext);
            assertNotNull(entries);
            assertEquals(2, entries.size());

            List<String> entryIds = new ArrayList<>();
            for (DocumentModel entry : entries) {
                entryIds.add(entry.getId());
            }
            Collections.sort(entryIds);
            assertEquals("ogrisel+user1@nuxeo.com", entryIds.get(0));
            assertEquals("ogrisel+user3@nuxeo.com", entryIds.get(1));

        }
    }

}
