/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id$
 */

package org.nuxeo.ecm.directory.ldap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Session;

/**
 * Tests for NXP-2461: Manage LDAP directories with missing entries for
 * identifier field.
 *
 * @author Anahide Tchertchian
 */
public class TestLDAPSessionWithMissingId extends LDAPDirectoryTestCase {

    protected static final String USER_SCHEMANAME = "user";

    protected static final String GROUP_SCHEMANAME = "group";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // override default defs
        if (USE_EXTERNAL_TEST_LDAP_SERVER) {
            deployContrib("org.nuxeo.ecm.directory.ldap.tests",
                    EXTERNAL_SERVER_SETUP_OVERRIDE);
        } else {
            deployContrib("org.nuxeo.ecm.directory.ldap.tests",
                    INTERNAL_SERVER_SETUP_OVERRIDE);
            getLDAPDirectory("userDirectory").setTestServer(SERVER);
            getLDAPDirectory("groupDirectory").setTestServer(SERVER);
        }
    }

    // override tests to get specific use cases

    @SuppressWarnings("unchecked")
    public void testGetEntry() throws Exception {
        Session session = getLDAPDirectory("userDirectory").getSession();
        try {
            DocumentModel entry = session.getEntry("Administrator");
            assertNull(entry);

            entry = session.getEntry("ogrisel+Administrator@nuxeo.com");
            assertEquals("ogrisel+Administrator@nuxeo.com", entry.getId());
            assertEquals("Administrator", entry.getProperty(USER_SCHEMANAME,
                    "username"));
            assertEquals("Manager", entry.getProperty(USER_SCHEMANAME,
                    "lastName"));

            if (USE_EXTERNAL_TEST_LDAP_SERVER) {
                assertEquals(Long.valueOf(1), entry.getProperty(
                        USER_SCHEMANAME, "intField"));
            }
            // assertNull(entry.getProperty(USER_SCHEMANAME, "sn"));
            assertEquals("Administrator", entry.getProperty(USER_SCHEMANAME,
                    "firstName"));
            // assertNull(entry.getProperty(USER_SCHEMANAME, "givenName"));
            // assertNull(entry.getProperty(USER_SCHEMANAME, "cn"));
            assertNull(entry.getProperty(USER_SCHEMANAME, "password"));
            // assertNull(entry.getProperty(USER_SCHEMANAME, "userPassword"));

            List val = (List) entry.getProperty(USER_SCHEMANAME, "employeeType");
            assertTrue(val.isEmpty());

            if (USE_EXTERNAL_TEST_LDAP_SERVER) {
                // LDAP references do not work with the internal test server
                List<String> groups = (List<String>) entry.getProperty(
                        USER_SCHEMANAME, "groups");
                assertEquals(2, groups.size());
                assertTrue(groups.contains("members"));
                assertTrue(groups.contains("administrators"));
            }

            DocumentModel entry2 = session.getEntry("ogrisel+user1@nuxeo.com");
            assertNotNull(entry2);
            assertEquals("ogrisel+user1@nuxeo.com", entry2.getId());
            assertEquals("user1", entry2.getProperty(USER_SCHEMANAME,
                    "username"));
            assertEquals("One", entry2.getProperty(USER_SCHEMANAME, "lastName"));
            assertEquals("User", entry2.getProperty(USER_SCHEMANAME,
                    "firstName"));
            assertNull(entry2.getProperty(USER_SCHEMANAME, "password"));
            // assertNull(entry2.getProperty(USER_SCHEMANAME, "userPassword"));
            assertEquals(Arrays.asList("Boss"), entry2.getProperty(
                    USER_SCHEMANAME, "employeeType"));

            if (USE_EXTERNAL_TEST_LDAP_SERVER) {
                // default value for missing attribute
                assertEquals(Long.valueOf(0), entry2.getProperty(
                        USER_SCHEMANAME, "intField"));

                // LDAP references do not work with the internal test server
                if (HAS_DYNGROUP_SCHEMA) {
                    assertEquals(Arrays.asList("dyngroup1", "dyngroup2",
                            "members", "subgroup"), entry2.getProperty(
                            USER_SCHEMANAME, "groups"));
                } else {
                    assertEquals(Arrays.asList("members", "subgroup"),
                            entry2.getProperty(USER_SCHEMANAME, "groups"));
                }

            }

            DocumentModel entry3 = session.getEntry("UnexistingEntry");
            assertNull(entry3);

            // test special character escaping
            if (USE_EXTERNAL_TEST_LDAP_SERVER) {
                // for some reason this do not work with the internal
                // ApacheDS server (bug?)
                DocumentModel entry4 = session.getEntry("Admi*");
                assertNull(entry4);

                DocumentModel entry5 = session.getEntry("");
                assertNull(entry5);

                DocumentModel entry6 = session.getEntry("(objectClass=*)");
                assertNull(entry6);
            }

        } finally {
            session.close();
        }
    }

    @SuppressWarnings("unchecked")
    public void testGetEntry2() throws ClientException {
        Session session = getLDAPDirectory("groupDirectory").getSession();
        try {
            DocumentModel entry = session.getEntry("administrators");
            assertNotNull(entry);
            assertEquals("administrators", entry.getId());
            assertEquals("administrators", entry.getProperty(GROUP_SCHEMANAME,
                    "groupname"));

            if (USE_EXTERNAL_TEST_LDAP_SERVER) {
                // LDAP references do not work with the internal test server
                List<String> members = (List<String>) entry.getProperty(
                        GROUP_SCHEMANAME, "members");
                assertNotNull(members);
                assertEquals(1, members.size());
                assertTrue(members.contains("ogrisel+Administrator@nuxeo.com"));

                List<String> subGroups = (List<String>) entry.getProperty(
                        GROUP_SCHEMANAME, "subGroups");
                assertNotNull(subGroups);
                assertEquals(0, subGroups.size());

                List<String> parentGroups = (List<String>) entry.getProperty(
                        GROUP_SCHEMANAME, "parentGroups");
                assertNotNull(parentGroups);
                assertEquals(0, parentGroups.size());
            }

            entry = session.getEntry("members");
            assertNotNull(entry);
            assertEquals("members", entry.getId());
            assertEquals("members", entry.getProperty(GROUP_SCHEMANAME,
                    "groupname"));

            if (USE_EXTERNAL_TEST_LDAP_SERVER) {
                // LDAP references do not work with the internal test server
                List<String> members = (List<String>) entry.getProperty(
                        GROUP_SCHEMANAME, "members");
                assertEquals(2, members.size());
                assertTrue(members.contains("ogrisel+Administrator@nuxeo.com"));
                assertTrue(members.contains("ogrisel+user1@nuxeo.com"));

                List<String> subGroups = (List<String>) entry.getProperty(
                        GROUP_SCHEMANAME, "subGroups");
                assertEquals(1, subGroups.size());
                assertTrue(subGroups.contains("submembers"));

                List<String> parentGroups = (List<String>) entry.getProperty(
                        GROUP_SCHEMANAME, "parentGroups");
                assertEquals(0, parentGroups.size());
            }

            entry = session.getEntry("submembers");
            assertNotNull(entry);
            assertEquals("submembers", entry.getId());
            assertEquals("submembers", entry.getProperty(GROUP_SCHEMANAME,
                    "groupname"));

            if (USE_EXTERNAL_TEST_LDAP_SERVER) {
                // LDAP references do not work with the internal test server
                assertEquals(Arrays.asList(), entry.getProperty(
                        GROUP_SCHEMANAME, "members"));
                assertEquals(Arrays.asList(), entry.getProperty(
                        GROUP_SCHEMANAME, "subGroups"));

                if (HAS_DYNGROUP_SCHEMA) {
                    assertEquals(Arrays.asList("dyngroup1", "members"),
                            entry.getProperty(GROUP_SCHEMANAME, "parentGroups"));
                } else {
                    assertEquals(Arrays.asList("members"), entry.getProperty(
                            GROUP_SCHEMANAME, "parentGroups"));
                }
            }

        } finally {
            session.close();
        }
    }

    public void testQuery1() throws ClientException {
        Session session = getLDAPDirectory("userDirectory").getSession();
        try {
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            filter.put("username", "user");
            Set<String> fulltext = new HashSet<String>();
            fulltext.add("username");
            DocumentModelList entries = session.query(filter, fulltext);
            assertNotNull(entries);
            assertEquals(2, entries.size());

            List<String> entryIds = new ArrayList<String>();
            for (DocumentModel entry : entries) {
                entryIds.add(entry.getId());
            }
            Collections.sort(entryIds);
            assertEquals("ogrisel+user1@nuxeo.com", entryIds.get(0));
            assertEquals("ogrisel+user3@nuxeo.com", entryIds.get(1));

        } finally {
            session.close();
        }
    }

}
