/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.directory.ldap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.management.api.ProbeStatus;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.ldap.management.LDAPDirectoriesProbe;

/**
 * @author <a href="mailto:ogrisel@nuxeo.com">Olivier Grisel</a>
 */
public class TestLDAPSession extends LDAPDirectoryTestCase {

    private static final String USER_SCHEMANAME = "user";

    private static final String GROUP_SCHEMANAME = "group";

    @SuppressWarnings("unchecked")
    public void testGetEntry() throws Exception {
        Session session = getLDAPDirectory("userDirectory").getSession();
        try {
            DocumentModel entry = session.getEntry("Administrator");
            assertNotNull(entry);
            assertEquals("Administrator", entry.getId());
            assertEquals("Manager", entry.getProperty(USER_SCHEMANAME,
                    "lastName"));

            if (USE_EXTERNAL_TEST_LDAP_SERVER) {
                assertEquals(Long.valueOf(1), entry.getProperty(
                        USER_SCHEMANAME, "intField"));
                assertEquals("uid=Administrator,ou=people,dc=example,dc=com",
                        entry.getProperty(USER_SCHEMANAME, "dn"));
            }
            assertEquals("Administrator", entry.getProperty(USER_SCHEMANAME,
                    "firstName"));
            assertNull(entry.getProperty(USER_SCHEMANAME, "password"));

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

            DocumentModel entry2 = session.getEntry("user1");
            assertNotNull(entry2);
            assertEquals("user1", entry2.getId());
            assertEquals("One", entry2.getProperty(USER_SCHEMANAME, "lastName"));
            assertEquals("User", entry2.getProperty(USER_SCHEMANAME,
                    "firstName"));
            assertNull(entry2.getProperty(USER_SCHEMANAME, "password"));

            try {
                entry2.getProperty(USER_SCHEMANAME, "userPassword");
                fail();
            } catch (ClientException ce) {
                // expected
            }
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
                assertTrue(members.contains("Administrator"));

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
                assertEquals(3, members.size());
                assertTrue(members.contains("Administrator"));
                assertTrue(members.contains("user1"));

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
                assertEquals(Arrays.asList("user2"), entry.getProperty(
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

    @SuppressWarnings("unchecked")
    public void testGetEntry3() throws ClientException {
        if (!HAS_DYNGROUP_SCHEMA) {
            return;
        }
        Session session = getLDAPDirectory("groupDirectory").getSession();
        try {
            DocumentModel entry = session.getEntry("dyngroup1");
            assertNotNull(entry);
            assertEquals("dyngroup1", entry.getId());
            assertEquals("dyngroup1", entry.getProperty(GROUP_SCHEMANAME,
                    "groupname"));

            List<String> members = (List<String>) entry.getProperty(
                    GROUP_SCHEMANAME, "members");
            assertEquals(Arrays.asList("user1", "user3"), members);

            List<String> subGroups = (List<String>) entry.getProperty(
                    GROUP_SCHEMANAME, "subGroups");
            assertEquals(Arrays.asList("subgroup", "submembers", "subsubgroup",
                    "subsubsubgroup"), subGroups);

            List<String> parentGroups = (List<String>) entry.getProperty(
                    GROUP_SCHEMANAME, "parentGroups");
            assertNotNull(parentGroups);
            assertEquals(0, parentGroups.size());

            entry = session.getEntry("dyngroup2");
            assertNotNull(entry);
            assertEquals("dyngroup2", entry.getId());
            assertEquals("dyngroup2", entry.getProperty(GROUP_SCHEMANAME,
                    "groupname"));

            members = (List<String>) entry.getProperty(GROUP_SCHEMANAME,
                    "members");
            assertEquals(Arrays.asList("user1", "user3"), members);
            // user4 is not there since userDirectory is scoped 'onelevel'

            subGroups = (List<String>) entry.getProperty(GROUP_SCHEMANAME,
                    "subGroups");
            assertNotNull(subGroups);
            assertEquals(0, subGroups.size());

            parentGroups = (List<String>) entry.getProperty(GROUP_SCHEMANAME,
                    "parentGroups");
            assertNotNull(parentGroups);
            assertEquals(0, parentGroups.size());

            // test that submembers is a subgroup of dyngroup1 (inverse
            // reference resolution)
            entry = session.getEntry("submembers");
            assertNotNull(entry);
            assertEquals("submembers", entry.getId());
            assertEquals("submembers", entry.getProperty(GROUP_SCHEMANAME,
                    "groupname"));

            members = (List<String>) entry.getProperty(GROUP_SCHEMANAME,
                    "members");
            assertEquals(Arrays.asList("user2"), members);

            subGroups = (List<String>) entry.getProperty(GROUP_SCHEMANAME,
                    "subGroups");
            assertNotNull(subGroups);
            assertEquals(0, subGroups.size());

            parentGroups = (List<String>) entry.getProperty(GROUP_SCHEMANAME,
                    "parentGroups");
            assertEquals(Arrays.asList("dyngroup1", "members"), parentGroups);
        } finally {
            session.close();
        }
    }

    // NXP-2730: ldap queries are case-insensitive => test entry retrieval is ok
    // when using other cases (lower or upper)
    @SuppressWarnings("unchecked")
    public void testGetEntryWithIdInDifferentCase() throws ClientException {
        Session session = getLDAPDirectory("userDirectory").getSession();
        try {
            DocumentModel entry = session.getEntry("Administrator");
            assertNotNull(entry);
            assertEquals("Administrator", entry.getId());
            List<String> profiles = (List) entry.getProperty(USER_SCHEMANAME,
                    "profiles");
            assertNotNull(profiles);
            assertEquals(1, profiles.size());
            assertEquals("FUNCTIONAL_ADMINISTRATOR", profiles.get(0));

            // retrieve again in upper case
            entry = session.getEntry("ADMINISTRATOR");
            assertNotNull(entry);
            assertEquals("Administrator", entry.getId());
            profiles = (List) entry.getProperty(USER_SCHEMANAME, "profiles");
            assertNotNull(profiles);
            assertEquals(1, profiles.size());
            assertEquals("FUNCTIONAL_ADMINISTRATOR", profiles.get(0));

        } finally {
            session.close();
        }
    }

    @SuppressWarnings("unchecked")
    public void testGetEntryWithLdapTreeRef() throws ClientException {
        if (!USE_EXTERNAL_TEST_LDAP_SERVER) {
            return;
        }
        Session session = getLDAPDirectory("groupDirectory").getSession();
        Session unitSession = getLDAPDirectory("unitDirectory").getSession();
        try {
            DocumentModel entry = session.getEntry("subgroup");
            assertNotNull(entry);
            assertEquals("subgroup", entry.getId());
            assertEquals("subgroup", entry.getProperty(GROUP_SCHEMANAME,
                    "groupname"));

            // LDAP references do not work with the internal test server
            List<String> members = (List<String>) entry.getProperty(
                    GROUP_SCHEMANAME, "members");
            assertNotNull(members);
            assertEquals(1, members.size());

            List<String> subGroups = (List<String>) entry.getProperty(
                    GROUP_SCHEMANAME, "subGroups");
            assertNotNull(subGroups);
            assertEquals(0, subGroups.size());

            List<String> parentGroups = (List<String>) entry.getProperty(
                    GROUP_SCHEMANAME, "parentGroups");
            assertNotNull(parentGroups);
            if (HAS_DYNGROUP_SCHEMA) {
                assertEquals(1, parentGroups.size());
                assertEquals(Arrays.asList("dyngroup1"), parentGroups);
            } else {
                assertEquals(0, parentGroups.size());
            }

            List<String> ldapDirectChildren = (List<String>) entry.getProperty(
                    GROUP_SCHEMANAME, "ldapDirectChildren");
            assertNotNull(ldapDirectChildren);
            assertEquals(1, ldapDirectChildren.size());
            assertTrue(ldapDirectChildren.contains("subsubgroup"));

            List<String> ldapChildren = (List<String>) entry.getProperty(
                    GROUP_SCHEMANAME, "ldapChildren");
            assertNotNull(ldapChildren);
            assertEquals(2, ldapChildren.size());
            assertTrue(ldapChildren.contains("subsubgroup"));
            assertTrue(ldapChildren.contains("subsubsubgroup"));

            List<String> ldapDirectParents = (List<String>) entry.getProperty(
                    GROUP_SCHEMANAME, "ldapDirectParents");
            assertNotNull(ldapDirectParents);
            assertEquals(1, ldapDirectParents.size());
            assertTrue(ldapDirectParents.contains("group"));

            List<String> ldapParents = (List<String>) entry.getProperty(
                    GROUP_SCHEMANAME, "ldapParents");
            assertNotNull(ldapParents);
            assertEquals(1, ldapParents.size());
            assertTrue(ldapParents.contains("group"));

            List<String> ldapUnitDirectChildren = (List<String>) entry.getProperty(
                    GROUP_SCHEMANAME, "ldapUnitDirectChildren");
            assertNotNull(ldapUnitDirectChildren);
            assertEquals(1, ldapUnitDirectChildren.size());
            assertTrue(ldapUnitDirectChildren.contains("subunit"));

            List<String> ldapUnitDirectParents = (List<String>) entry.getProperty(
                    GROUP_SCHEMANAME, "ldapUnitDirectParents");
            assertNotNull(ldapUnitDirectParents);
            assertEquals(0, ldapUnitDirectParents.size());

            DocumentModel unitEntry = unitSession.getEntry("subunit");
            assertNotNull(unitEntry);
            assertEquals("subunit", unitEntry.getId());
            assertEquals("subunit", unitEntry.getProperty(GROUP_SCHEMANAME,
                    "groupname"));

            ldapUnitDirectChildren = (List<String>) unitEntry.getProperty(
                    GROUP_SCHEMANAME, "ldapUnitDirectChildren");
            assertNotNull(ldapUnitDirectChildren);
            assertEquals(0, ldapUnitDirectChildren.size());

            ldapUnitDirectParents = (List<String>) unitEntry.getProperty(
                    GROUP_SCHEMANAME, "ldapUnitDirectParents");
            assertNotNull(ldapUnitDirectParents);
            assertEquals(1, ldapUnitDirectParents.size());
            assertTrue(ldapUnitDirectParents.contains("subgroup"));

        } finally {
            session.close();
            unitSession.close();
        }
    }

    public void testGetEntries() throws ClientException {
        Session session = getLDAPDirectory("userDirectory").getSession();
        try {
            DocumentModelList entries = session.getEntries();
            assertNotNull(entries);
            assertEquals(4, entries.size());
            List<String> entryIds = new ArrayList<String>();
            for (DocumentModel entry : entries) {
                entryIds.add(entry.getId());
            }
            Collections.sort(entryIds);
            assertEquals("Administrator", entryIds.get(0));
            assertEquals("user1", entryIds.get(1));
            assertEquals("user2", entryIds.get(2));
            assertEquals("user3", entryIds.get(3));
        } finally {
            session.close();
        }
    }

    public void testCreateEntry() throws Exception {
        if (USE_EXTERNAL_TEST_LDAP_SERVER) {
            Session session = getLDAPDirectory("userDirectory").getSession();
            try {

                assertNotNull(session);
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("username", "user0");
                map.put("password", "pass0");
                map.put("firstName", "User");
                map.put("lastName", "");
                map.put("intField", Long.valueOf(0));

                // special DN read only field should be ignored
                map.put("dn", "cn=this,ou=is,ou=a,ou=fake,o=dn");

                map.put("email", "nobody@nowhere.com");
                map.put("employeeType", Arrays.asList("item1", "item2"));
                if (USE_EXTERNAL_TEST_LDAP_SERVER) {
                    map.put("groups",
                            Arrays.asList("members", "administrators"));
                }
                DocumentModel dm = session.createEntry(map);
                session.commit(); // doesn't do anything

                dm = session.getEntry("user0");
                assertNotNull(dm);

                String id = dm.getId();
                assertNotNull(id);

                String[] schemaNames = dm.getSchemas();
                assertEquals(1, schemaNames.length);

                assertEquals(USER_SCHEMANAME, schemaNames[0]);

                assertEquals("user0", dm.getProperty(USER_SCHEMANAME,
                        "username"));
                assertEquals("User", dm.getProperty(USER_SCHEMANAME,
                        "firstName"));
                assertEquals("", dm.getProperty(USER_SCHEMANAME, "lastName"));
                assertEquals(Long.valueOf(0), dm.getProperty(USER_SCHEMANAME,
                        "intField"));
                assertEquals("uid=user0,ou=people,dc=example,dc=com",
                        dm.getProperty(USER_SCHEMANAME, "dn"));
                assertEquals("nobody@nowhere.com", dm.getProperty(
                        USER_SCHEMANAME, "email"));
                assertEquals(Arrays.asList("item1", "item2"), dm.getProperty(
                        USER_SCHEMANAME, "employeeType"));
                assertEquals(Arrays.asList("administrators", "members"),
                        dm.getProperty(USER_SCHEMANAME, "groups"));
                assertTrue(session.authenticate("user0", "pass0"));
            } finally {
                session.close();
            }
            session = getLDAPDirectory("groupDirectory").getSession();
            try {
                DocumentModel entry = session.getEntry("administrators");
                assertNotNull(entry);
                assertEquals(Arrays.asList("Administrator", "user0"),
                        entry.getProperty(GROUP_SCHEMANAME, "members"));
                entry = session.getEntry("members");
                assertNotNull(entry);
                assertEquals(Arrays.asList("Administrator", "user0", "user1",
                        "user2"),
                        entry.getProperty(GROUP_SCHEMANAME, "members"));
            } finally {
                session.close();
            }
        }
    }

    public void testCreateEntry2() throws Exception {
        if (USE_EXTERNAL_TEST_LDAP_SERVER) {
            Session session = getLDAPDirectory("groupDirectory").getSession();
            try {
                assertNotNull(session);
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("groupname", "group2");
                map.put("members", Arrays.asList("user1", "user2"));
                DocumentModel dm = session.createEntry(map);
                session.commit();
                dm = session.getEntry("group2");
                assertNotNull(dm);
                assertEquals(Arrays.asList("user1", "user2"), dm.getProperty(
                        GROUP_SCHEMANAME, "members"));

                map = new HashMap<String, Object>();
                map.put("groupname", "group1");
                map.put("members", Arrays.asList("Administrator"));
                map.put("subGroups", Arrays.asList("group2"));
                dm = session.createEntry(map);
                session.commit(); // doesn't do anything
                dm = session.getEntry("group1");
                assertNotNull(dm);
                assertEquals(Arrays.asList("Administrator"), dm.getProperty(
                        GROUP_SCHEMANAME, "members"));
                assertEquals(Arrays.asList("group2"), dm.getProperty(
                        GROUP_SCHEMANAME, "subGroups"));

                dm = session.getEntry("group2");
                assertNotNull(dm);
                assertEquals(Arrays.asList("group1"), dm.getProperty(
                        GROUP_SCHEMANAME, "parentGroups"));

                map = new HashMap<String, Object>();
                map.put("groupname", "emptygroup");
                map.put("members", new ArrayList<String>());
                dm = session.createEntry(map);
                dm = session.getEntry("emptygroup");
                assertNotNull(dm);
                assertEquals("emptygroup", dm.getId());
                assertEquals("emptygroup", dm.getProperty(GROUP_SCHEMANAME,
                        "groupname"));

                assertEquals(Arrays.asList(), dm.getProperty(GROUP_SCHEMANAME,
                        "members"));

                assertEquals(Arrays.asList(), dm.getProperty(GROUP_SCHEMANAME,
                        "subGroups"));

                assertEquals(Arrays.asList(), dm.getProperty(GROUP_SCHEMANAME,
                        "parentGroups"));
            } finally {
                session.close();
            }
        }
    }

    public void testUpdateEntry() throws Exception {
        if (USE_EXTERNAL_TEST_LDAP_SERVER) {
            Session session = getLDAPDirectory("userDirectory").getSession();
            Session groupSession = getLDAPDirectory("groupDirectory").getSession();
            try {
                DocumentModel entry = session.getEntry("user1");
                assertNotNull(entry);

                // check that this entry is editable:
                assertFalse(BaseSession.isReadOnlyEntry(entry));

                entry.setProperty(USER_SCHEMANAME, "firstName", "toto");
                entry.setProperty(USER_SCHEMANAME, "lastName", "");
                entry.setProperty(USER_SCHEMANAME, "password", "toto");
                entry.setProperty(USER_SCHEMANAME, "intField",
                        Long.valueOf(123));

                // try to tweak the DN read-only field
                entry.setProperty(USER_SCHEMANAME, "dn",
                        "cn=this,ou=is,ou=a,ou=fake,o=dn");

                entry.setProperty(USER_SCHEMANAME, "employeeType",
                        Arrays.asList("item3", "item4"));
                List<String> groups = Arrays.asList("administrators", "members");
                entry.setProperty(USER_SCHEMANAME, "groups", groups);
                session.updateEntry(entry);

                entry = session.getEntry("user1");
                assertNotNull(entry);
                assertEquals("toto", entry.getProperty(USER_SCHEMANAME,
                        "firstName"));
                assertEquals("", entry.getProperty(USER_SCHEMANAME, "lastName"));
                assertEquals(Long.valueOf(123), entry.getProperty(
                        USER_SCHEMANAME, "intField"));
                assertEquals(Arrays.asList("item3", "item4"),
                        entry.getProperty(USER_SCHEMANAME, "employeeType"));
                if (HAS_DYNGROUP_SCHEMA) {
                    assertEquals(Arrays.asList("administrators", "dyngroup1",
                            "dyngroup2", "members"), entry.getProperty(
                            USER_SCHEMANAME, "groups"));
                } else {
                    assertEquals(Arrays.asList("administrators", "members"),
                            entry.getProperty(USER_SCHEMANAME, "groups"));
                }

                // check that the referenced groups where edited properly
                entry = groupSession.getEntry("administrators");
                assertNotNull(entry);
                assertEquals(Arrays.asList("Administrator", "user1"),
                        entry.getProperty(GROUP_SCHEMANAME, "members"));
                assertEquals(Arrays.asList(), entry.getProperty(
                        GROUP_SCHEMANAME, "subGroups"));

                entry = groupSession.getEntry("members");
                assertNotNull(entry);
                assertEquals(Arrays.asList("Administrator", "user1", "user2"),
                        entry.getProperty(GROUP_SCHEMANAME, "members"));
                assertEquals(Arrays.asList("submembers"), entry.getProperty(
                        GROUP_SCHEMANAME, "subGroups"));

            } finally {
                session.close();
                groupSession.close();
            }
            session = getLDAPDirectory("groupDirectory").getSession();
            try {
                DocumentModel entry = session.getEntry("administrators");
                assertNotNull(entry);
                assertEquals(Arrays.asList("Administrator", "user1"),
                        entry.getProperty(GROUP_SCHEMANAME, "members"));
            } finally {
                session.close();
            }
        }
    }

    public void testUpdateEntry2() throws Exception {
        if (USE_EXTERNAL_TEST_LDAP_SERVER) {
            Session session = getLDAPDirectory("groupDirectory").getSession();
            try {
                DocumentModel entry = session.getEntry("members");
                assertNotNull(entry);

                // check that this entry is editable:
                assertFalse(BaseSession.isReadOnlyEntry(entry));
                assertEquals(
                        "cn=members,ou=editable,ou=groups,dc=example,dc=com",
                        entry.getProperty(GROUP_SCHEMANAME, "dn"));

                assertEquals(Arrays.asList("submembers"), entry.getProperty(
                        GROUP_SCHEMANAME, "subGroups"));

                // edit description and members but not subGroups
                entry.setProperty(GROUP_SCHEMANAME, "description", "blablabla");
                entry.setProperty(GROUP_SCHEMANAME, "members", Arrays.asList(
                        "user1", "user2"));
                session.updateEntry(entry);

                entry = session.getEntry("members");
                assertNotNull(entry);
                assertEquals("blablabla", (String) entry.getProperty(
                        GROUP_SCHEMANAME, "description"));
                assertEquals(Arrays.asList("user1", "user2"),
                        entry.getProperty(GROUP_SCHEMANAME, "members"));
                assertEquals(Arrays.asList("submembers"), entry.getProperty(
                        GROUP_SCHEMANAME, "subGroups"));

                // edit both members and subGroups at the same time
                entry.setProperty(GROUP_SCHEMANAME, "members", Arrays.asList(
                        "user1", "user3"));
                entry.setProperty(GROUP_SCHEMANAME, "subGroups", Arrays.asList(
                        "submembers", "administrators"));
                session.updateEntry(entry);

                entry = session.getEntry("members");
                assertNotNull(entry);
                assertEquals("blablabla", (String) entry.getProperty(
                        GROUP_SCHEMANAME, "description"));
                assertEquals(Arrays.asList("user1", "user3"),
                        entry.getProperty(GROUP_SCHEMANAME, "members"));
                assertEquals(Arrays.asList("administrators", "submembers"),
                        entry.getProperty(GROUP_SCHEMANAME, "subGroups"));
            } finally {
                session.close();
            }
        }
    }

    public void testUpdateEntry3() throws Exception {
        if (USE_EXTERNAL_TEST_LDAP_SERVER && HAS_DYNGROUP_SCHEMA) {
            Session session = getLDAPDirectory("groupDirectory").getSession();
            try {
                DocumentModel entry = session.getEntry("dyngroup1");

                // check that this entry is editable:
                assertFalse(BaseSession.isReadOnlyEntry(entry));
                assertEquals(
                        "cn=dyngroup1,ou=dyngroups,ou=editable,ou=groups,dc=example,dc=com",
                        entry.getProperty(GROUP_SCHEMANAME, "dn"));

                assertNotNull(entry);
                assertEquals(Arrays.asList("user1", "user3"),
                        entry.getProperty(GROUP_SCHEMANAME, "members"));
                if (HAS_DYNGROUP_SCHEMA) {
                    assertEquals(Arrays.asList("subgroup", "submembers",
                            "subsubgroup", "subsubsubgroup"),
                            entry.getProperty(GROUP_SCHEMANAME, "subGroups"));
                } else {
                    assertEquals(Arrays.asList("submembers"),
                            entry.getProperty(GROUP_SCHEMANAME, "subGroups"));
                }

                // try to edit dynamic references values along with regular
                // fields
                entry.setProperty(GROUP_SCHEMANAME, "description", "blablabla");
                entry.setProperty(GROUP_SCHEMANAME, "members", Arrays.asList(
                        "user1", "user2"));
                entry.setProperty(GROUP_SCHEMANAME, "subGroups",
                        Arrays.asList());
                session.updateEntry(entry);

                entry = session.getEntry("dyngroup1");
                assertNotNull(entry);

                // the stored field has been edited
                assertEquals("blablabla", (String) entry.getProperty(
                        GROUP_SCHEMANAME, "description"));

                // dynamically resolved references have not been edited
                assertEquals(Arrays.asList("user1", "user3"),
                        entry.getProperty(GROUP_SCHEMANAME, "members"));
                if (HAS_DYNGROUP_SCHEMA) {
                    assertEquals(Arrays.asList("subgroup", "submembers",
                            "subsubgroup", "subsubsubgroup"),
                            entry.getProperty(GROUP_SCHEMANAME, "subGroups"));
                } else {
                    assertEquals(Arrays.asList("submembers"),
                            entry.getProperty(GROUP_SCHEMANAME, "subGroups"));
                }

                // edit both members and subGroups at the same time
                entry.setProperty(GROUP_SCHEMANAME, "members", Arrays.asList(
                        "user1", "user3"));
                entry.setProperty(GROUP_SCHEMANAME, "subGroups", Arrays.asList(
                        "submembers", "administrators"));
                session.updateEntry(entry);
            } finally {
                session.close();
            }
        }
    }

    public void testUpdateEntry4() throws Exception {
        if (USE_EXTERNAL_TEST_LDAP_SERVER && HAS_DYNGROUP_SCHEMA) {
            Session userSession = getLDAPDirectory("userDirectory").getSession();
            Session groupSession = getLDAPDirectory("groupDirectory").getSession();
            try {
                DocumentModel entry = groupSession.getEntry("readonlygroup1");
                assertNotNull(entry);

                // check that this entry is NOT editable:
                assertTrue(BaseSession.isReadOnlyEntry(entry));
                assertEquals(
                        "cn=readonlygroup1,ou=readonly,ou=groups,dc=example,dc=com",
                        entry.getProperty(GROUP_SCHEMANAME, "dn"));

                assertEquals("Statically defined group that is not editable",
                        entry.getProperty(GROUP_SCHEMANAME, "description"));
                assertEquals(Arrays.asList("user2"), entry.getProperty(
                        GROUP_SCHEMANAME, "members"));

                // check that updates to a readonly entry are not taken into
                // account

                // edit description and members but not subGroups
                entry.setProperty(GROUP_SCHEMANAME, "description", "blablabla");
                entry.setProperty(GROUP_SCHEMANAME, "members", Arrays.asList(
                        "user1", "user2"));
                groupSession.updateEntry(entry);

                // fetch the entry again
                entry = groupSession.getEntry("readonlygroup1");
                assertNotNull(entry);

                // values should not have changed
                assertEquals("Statically defined group that is not editable",
                        entry.getProperty(GROUP_SCHEMANAME, "description"));
                assertEquals(Arrays.asList("user2"), entry.getProperty(
                        GROUP_SCHEMANAME, "members"));

                // check that we cannot edit readonlygroup1 indirectly by adding
                // it as a group of user1
                DocumentModel user1 = userSession.getEntry("user1");
                user1.setProperty(USER_SCHEMANAME, "groups",
                        Arrays.asList("readonlygroup1"));
                userSession.updateEntry(user1);

                // fetch the group entry again
                entry = groupSession.getEntry("readonlygroup1");
                assertNotNull(entry);

                // values should not have changed
                assertEquals("Statically defined group that is not editable",
                        entry.getProperty(GROUP_SCHEMANAME, "description"));
                assertEquals(Arrays.asList("user2"), entry.getProperty(
                        GROUP_SCHEMANAME, "members"));

            } finally {
                userSession.close();
                groupSession.close();
            }
        }
    }

    public void testDeleteEntry() throws ClientException {
        if (!USE_EXTERNAL_TEST_LDAP_SERVER) {
            // this does not work with the internal server which has
            // a suffixed context that prevent it from looking up
            // the entry to delete
            return;
        }
        Session session = getLDAPDirectory("userDirectory").getSession();
        try {
            session.deleteEntry("user1");
            DocumentModel entry = session.getEntry("user1");
            assertNull(entry);

            DocumentModelList entries = session.getEntries();
            assertEquals(3, entries.size());

            session.deleteEntry("user2");
            entry = session.getEntry("user2");
            assertNull(entry);

            entries = session.getEntries();
            assertEquals(2, entries.size());

            session.deleteEntry("Administrator");
            entry = session.getEntry("Administrator");
            assertNull(entry);

            entries = session.getEntries();
            assertEquals(1, entries.size());

            session.deleteEntry("user3");
            entry = session.getEntry("user3");
            assertNull(entry);

            entries = session.getEntries();
            assertEquals(0, entries.size());
        } finally {
            session.close();
        }
    }

    public void testDeleteEntry2() throws ClientException {
        if (!USE_EXTERNAL_TEST_LDAP_SERVER) {
            // this does not work with the internal server which has
            // a suffixed context that prevent it from looking up
            // the entry to delete
            return;
        }
        Session session = getLDAPDirectory("groupDirectory").getSession();
        try {
            session.deleteEntry("submembers");
            DocumentModel entry = session.getEntry("submembers");
            assertNull(entry);

            DocumentModelList entries = session.getEntries();
            if (HAS_DYNGROUP_SCHEMA) {
                // 2 dynamic groups
                assertEquals(9, entries.size());
            } else {
                assertEquals(7, entries.size());
            }
        } finally {
            session.close();
        }
    }

    public void testRollback() throws ClientException {
        // As a LDAP is not transactional, rollbacking is useless
        // this is just a smoke test
        Session session = getLDAPDirectory("userDirectory").getSession();
        try {
            session.getEntries();
            session.rollback();
        } finally {
            session.close();
        }
    }

    public void testQuery1() throws ClientException {
        Session session = getLDAPDirectory("userDirectory").getSession();
        try {
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            DocumentModelList entries;

            // empty filter means everything (like getEntries)
            if (USE_EXTERNAL_TEST_LDAP_SERVER) {
                // empty filters do not work with ApacheDS
                entries = session.query(filter);
                assertNotNull(entries);
                assertEquals(4, entries.size());
                List<String> entryIds = new ArrayList<String>();
                for (DocumentModel entry : entries) {
                    entryIds.add(entry.getId());
                }
                Collections.sort(entryIds);
                assertEquals("Administrator", entryIds.get(0));
                assertEquals("user1", entryIds.get(1));
                assertEquals("user2", entryIds.get(2));
                assertEquals("user3", entryIds.get(3));

                // trying to query users with no surnames
                filter.put("lastName", "");
                entries = session.query(filter);
                assertEquals(0, entries.size());

                // trying to cheat on the search engine
                filter.put("lastName", "Man*");
                entries = session.query(filter);
                assertEquals(0, entries.size());
            }

            // adding some filter that try to do unauthorized fullext
            filter.put("lastName", "Man");
            entries = session.query(filter);
            assertEquals(0, entries.size());

            // same request without cheating
            filter.put("lastName", "Manager");
            entries = session.query(filter);
            assertEquals(1, entries.size());
            assertEquals("Administrator", entries.get(0).getId());

            // impossible request (too restrictive)
            filter.put("firstName", "User");
            entries = session.query(filter);
            assertEquals(0, entries.size());
        } finally {
            session.close();
        }
    }

    public void testQuery2() throws ClientException {
        if (!USE_EXTERNAL_TEST_LDAP_SERVER) {
            // query does not work at all with internal apache
            return;
        }
        Session session = getLDAPDirectory("userDirectory").getSession();
        try {
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            Set<String> fulltext = new HashSet<String>();

            // empty filter means everything (like getEntries)
            DocumentModelList entries = session.query(filter, fulltext);
            assertNotNull(entries);
            assertEquals(4, entries.size());

            // trying to do fulltext without the permission
            filter.put("firstName", "Use");
            entries = session.query(filter, fulltext);
            assertEquals(0, entries.size());

            // trying to do fulltext with the permission
            fulltext.add("firstName");
            entries = session.query(filter, fulltext);
            assertEquals(3, entries.size());
            assertEquals("user1", entries.get(0).getId());
            assertEquals("user2", entries.get(1).getId());
            assertEquals("user3", entries.get(2).getId());

            // more fulltext without the permission
            filter.put("lastName", "n");
            entries = session.query(filter, fulltext);
            assertEquals(0, entries.size());

            if (USE_EXTERNAL_TEST_LDAP_SERVER) {
                // trying to cheat
                filter.put("lastName", "*");
                entries = session.query(filter, fulltext);
                assertEquals(0, entries.size());
            }

            // more fulltext with the permission
            filter.put("lastName", "on");
            fulltext.add("lastName");
            entries = session.query(filter, fulltext);
            assertEquals(2, entries.size());
            assertEquals("user1", entries.get(0).getId());
            assertEquals("user2", entries.get(1).getId());

            // empty filter marked fulltext should match all
            filter.clear();
            fulltext.clear();
            filter.put("lastName", "");
            fulltext.add("lastName");
            entries = session.query(filter, fulltext);
            assertEquals(4, entries.size());
        } finally {
            session.close();
        }
    }

    public void testQueryWithNullFilter() throws ClientException {
        Session session = getLDAPDirectory("userDirectory").getSession();
        try {
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            DocumentModelList entries;

            // negative filter
            filter.put("initials", null);
            entries = session.query(filter);
            assertEquals(1, entries.size());
            assertEquals("user3", entries.get(0).getId());

            filter.put("employeeType", null);
            filter.put("employeeNumber", null);
            entries = session.query(filter);
            assertEquals(1, entries.size());
            assertEquals("user3", entries.get(0).getId());

        } finally {
            session.close();
        }
    }

    public void testQueryOrderBy() throws ClientException {
        Session session = getLDAPDirectory("userDirectory").getSession();
        try {
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            Map<String, String> orderBy = new HashMap<String, String>();
            DocumentModelList entries;

            orderBy.put("company", "asc");
            entries = session.query(filter, Collections.<String> emptySet(),
                    orderBy);
            assertEquals(4, entries.size());
            // user3: creole
            // Administrator: nuxeo
            // user2: super
            // user1: viral prod
            assertEquals("user3", entries.get(0).getId());
            assertEquals("Administrator", entries.get(1).getId());
            assertEquals("user2", entries.get(2).getId());
            assertEquals("user1", entries.get(3).getId());
        } finally {
            session.close();
        }
    }

    public void testAuthenticate() throws ClientException {
        if (!USE_EXTERNAL_TEST_LDAP_SERVER) {
            // authenticate does not work at all with internal apache
            return;
        }
        Session session = getLDAPDirectory("userDirectory").getSession();
        try {
            assertTrue(session.authenticate("Administrator", "Administrator"));
            assertTrue(session.authenticate("user1", "user1"));

            assertFalse(session.authenticate("Administrator", "BAD password"));
            assertFalse(session.authenticate("user1", "*"));

            assertFalse(session.authenticate("NotExistingUser", "whatever"));
            assertFalse(session.authenticate("*", "*"));

            // ensure workaround to avoid anonymous binding is setup
            // NXP-1980
            assertFalse(session.authenticate("Administrator", ""));
            assertFalse(session.authenticate("user1", ""));
        } finally {
            session.close();
        }
    }

    public void testGetMandatoryAttributes() throws ClientException {
        if (USE_EXTERNAL_TEST_LDAP_SERVER) {
            LDAPSession session = (LDAPSession) getLDAPDirectory(
                    "userDirectory").getSession();
            try {
                List<String> mandatoryAttributes = session.getMandatoryAttributes();
                assertEquals(Arrays.asList("sn", "cn"), mandatoryAttributes);
            } finally {
                session.close();
            }
            session = (LDAPSession) getLDAPDirectory("groupDirectory").getSession();
            try {
                List<String> mandatoryAttributes = session.getMandatoryAttributes();
                assertEquals(Arrays.asList("uniqueMember", "cn"),
                        mandatoryAttributes);
            } finally {
                session.close();
            }
        }
    }

    public void disable_testDateField() throws ClientException {
        /*
         * To make this test working, you must : - rename this method to
         * testDateField() - in the file slapd.conf, uncomment the line include
         * /etc/ldap/schema/testdateperson.schema - copy the file
         * ldaptools/testdateperson.schema in the folder /etc/ldap/schema - in
         * the file TestDirectoriesWithExternalOpenLDAP.xml, uncomment the lines
         * <creationClass>testDatePerson</creationClass> and <fieldMapping
         * name="dateField">mydate</fieldMapping>
         */
        if (USE_EXTERNAL_TEST_LDAP_SERVER) {
            Session session = getLDAPDirectory("userDirectory").getSession();
            try {
                assertNotNull(session);
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("username", "user3");
                map.put("password", "pass3");
                map.put("firstName", "User");
                map.put("lastName", "Three");
                Calendar cal1 = Calendar.getInstance();
                cal1.set(2007, 2, 25, 12, 34, 56);
                map.put("dateField", cal1);
                session.createEntry(map);

                DocumentModel entry = session.getEntry("user3");
                assertNotNull(entry);
                assertEquals("user3", entry.getId());
                Calendar cal2 = (Calendar) entry.getProperty(USER_SCHEMANAME,
                        "dateField");
                assertEquals(cal1.getTimeInMillis() / 1000,
                        cal2.getTimeInMillis() / 1000);

                cal2.add(Calendar.HOUR, 1);
                entry.setProperty(USER_SCHEMANAME, "dateField", cal2);
                session.updateEntry(entry);
                assertTrue(((Calendar) entry.getProperty(USER_SCHEMANAME,
                        "dateField")).after(cal1));
            } finally {
                session.close();
            }
        }
    }

    public void testCreateFromModel() throws Exception {
        if (USE_EXTERNAL_TEST_LDAP_SERVER) {
            Session dir = getLDAPDirectory("userDirectory").getSession();
            try {
                String schema = "user";
                DocumentModel entry = BaseSession.createEntryModel(null,
                        schema, null, null);
                entry.setProperty(schema, "username", "omar");
                // XXX: some values are mandatory on real LDAP
                entry.setProperty(schema, "password", "sesame");
                entry.setProperty(schema, "employeeType",
                        new String[] { "Slave" });

                assertNull(dir.getEntry("omar"));
                dir.createEntry(entry);
                assertNotNull(dir.getEntry("omar"));

                // create one with existing same id, must fail
                entry.setProperty(schema, "username", "Administrator");
                try {
                    entry = dir.createEntry(entry);
                    fail("Should raise an error, entry already exists");
                } catch (DirectoryException e) {
                }

            } finally {
                dir.close();
            }
        }
    }

    public void testHasEntry() throws Exception {
        Session dir = getLDAPDirectory("userDirectory").getSession();
        try {
            assertTrue(dir.hasEntry("Administrator"));
            assertFalse(dir.hasEntry("foo"));
        } finally {
            dir.close();
        }
    }

    public void testQueryEmptyString() throws Exception {
        Session session = getLDAPDirectory("userDirectory").getSession();
        Map<String, Serializable> filter = new HashMap<String, Serializable>();
        filter.put("cn", "");
        List<DocumentModel> docs = session.query(filter);
        assertNotNull(docs);
    }

    public void testProbe() {
        LDAPDirectoriesProbe probe = new LDAPDirectoriesProbe();
        ProbeStatus status = probe.run();
        assertTrue(status.isSuccess());
    }

}
