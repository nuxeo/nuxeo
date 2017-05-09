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
 *     George Lefter
 *     Funsho David
 *
 */

package org.nuxeo.directory.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.AbstractDirectory;
import org.nuxeo.ecm.directory.BaseDirectoryDescriptor;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.PasswordHelper;
import org.nuxeo.ecm.directory.Reference;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @since 9.2
 */
@RepositoryConfig(cleanup = Granularity.METHOD)
@LocalDeploy({ "org.nuxeo.ecm.directory.tests:test-directories-schema-override.xml",
        "org.nuxeo.ecm.directory.tests:test-directories-bundle.xml" })
public abstract class AbstractDirectoryTest {

    protected static final String USER_DIR = "userDirectory";

    protected static final String GROUP_DIR = "groupDirectory";

    protected static final String SCHEMA = "user";

    @Inject
    protected DirectoryService directoryService;

    public static Calendar getCalendar(int year, int month, int day, int hours, int minutes, int seconds,
            int milliseconds) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1); // 0-based
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hours);
        cal.set(Calendar.MINUTE, minutes);
        cal.set(Calendar.SECOND, seconds);
        cal.set(Calendar.MILLISECOND, milliseconds);
        return cal;
    }

    public static Calendar stripMillis(Calendar calendar) {
        return getCalendar(calendar.get(Calendar.YEAR), //
                calendar.get(Calendar.MONTH) + 1, //
                calendar.get(Calendar.DAY_OF_MONTH), //
                calendar.get(Calendar.HOUR_OF_DAY), //
                calendar.get(Calendar.MINUTE), //
                calendar.get(Calendar.SECOND), 0);
    }

    public static void assertCalendarEquals(Calendar expected, Calendar actual) throws Exception {
        if (expected.equals(actual)) {
            return;
        }
        // try without milliseconds for stupid MySQL
        if (stripMillis(expected).equals(stripMillis(actual))) {
            return;
        }
        assertEquals(expected, actual); // proper failure
    }

    public Session getSession() throws Exception {
        return directoryService.open(USER_DIR);
    }

    public Directory getDirectory() throws Exception {
        return directoryService.getDirectory(USER_DIR);
    }

    @Test
    public void testReference() throws Exception {
        Reference membersRef = directoryService.getDirectory(GROUP_DIR).getReference("members");

        // test initial configuration
        List<String> administrators = membersRef.getTargetIdsForSource("administrators");
        assertEquals(1, administrators.size());
        assertTrue(administrators.contains("Administrator"));

        // add user_1 to the administrators group
        membersRef.addLinks("administrators", Collections.singletonList("user_1"));

        administrators = membersRef.getTargetIdsForSource("administrators");
        assertEquals(2, administrators.size());
        assertTrue(administrators.contains("Administrator"));
        assertTrue(administrators.contains("user_1"));

        // reading the same link should not duplicate it
        membersRef.addLinks("administrators", Arrays.asList("user_1", "user_2"));

        administrators = membersRef.getTargetIdsForSource("administrators");
        assertEquals(3, administrators.size());
        assertTrue(administrators.contains("Administrator"));
        assertTrue(administrators.contains("user_1"));
        assertTrue(administrators.contains("user_2"));

        // remove the reference to Administrator
        membersRef.removeLinksForTarget("Administrator");
        administrators = membersRef.getTargetIdsForSource("administrators");
        assertEquals(2, administrators.size());
        assertTrue(administrators.contains("user_1"));
        assertTrue(administrators.contains("user_2"));

        // remove the references from administrators
        membersRef.removeLinksForSource("administrators");
        administrators = membersRef.getTargetIdsForSource("administrators");
        assertEquals(0, administrators.size());

        // read references with the set* methods

        membersRef.setTargetIdsForSource("administrators", Arrays.asList("user_1", "user_2"));
        administrators = membersRef.getTargetIdsForSource("administrators");
        assertEquals(2, administrators.size());
        assertTrue(administrators.contains("user_1"));
        assertTrue(administrators.contains("user_2"));

        membersRef.setTargetIdsForSource("administrators", Arrays.asList("user_1", "Administrator"));
        administrators = membersRef.getTargetIdsForSource("administrators");
        assertEquals(2, administrators.size());
        assertTrue(administrators.contains("user_1"));
        assertTrue(administrators.contains("Administrator"));

        membersRef.setSourceIdsForTarget("Administrator", Collections.singletonList("members"));
        administrators = membersRef.getTargetIdsForSource("administrators");
        assertEquals(1, administrators.size());
        assertTrue(administrators.contains("user_1"));

        administrators = membersRef.getSourceIdsForTarget("Administrator");
        assertEquals(1, administrators.size());
        assertTrue(administrators.contains("members"));

        // cleanup for other tests
        // (because the feature doesn't clean up everything)
        membersRef.setTargetIdsForSource("administrators", Collections.singletonList("Administrator"));
        membersRef.setTargetIdsForSource("members", Collections.singletonList("user_1"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateEntry() throws Exception {
        try (Session session = getSession()) {
            Map<String, Object> map = new HashMap<>();
            map.put("username", "user_0");
            map.put("password", "pass_0");
            map.put("intField", 5L);
            map.put("dateField", getCalendar(1982, 3, 25, 16, 30, 47, 0));
            map.put("groups", Arrays.asList("members", "administrators"));
            DocumentModel dm = session.createEntry(map);
            assertNotNull(dm);

            assertEquals("user_0", dm.getId());

            String[] schemaNames = dm.getSchemas();
            assertEquals(1, schemaNames.length);

            assertEquals(SCHEMA, schemaNames[0]);

            assertEquals("user_0", dm.getProperty(SCHEMA, "username"));
            assertEquals("pass_0", dm.getProperty(SCHEMA, "password"));
            assertEquals(5L, dm.getProperty(SCHEMA, "intField"));
            assertCalendarEquals(getCalendar(1982, 3, 25, 16, 30, 47, 0),
                    (Calendar) dm.getProperty(SCHEMA, "dateField"));

            List<String> groups = (List<String>) dm.getProperty(SCHEMA, "groups");
            assertEquals(2, groups.size());
            assertTrue(groups.contains("administrators"));
            assertTrue(groups.contains("members"));

        }

        // recheck the created entries has really been created from a second
        // session

        try (Session session = getSession()) {
            DocumentModel dm = session.getEntry("user_0");
            assertNotNull(dm);

            assertEquals("user_0", dm.getId());

            String[] schemaNames = dm.getSchemas();
            assertEquals(1, schemaNames.length);

            assertEquals(SCHEMA, schemaNames[0]);

            assertEquals("user_0", dm.getProperty(SCHEMA, "username"));
            assertEquals(5L, dm.getProperty(SCHEMA, "intField"));
            assertCalendarEquals(getCalendar(1982, 3, 25, 16, 30, 47, 0),
                    (Calendar) dm.getProperty(SCHEMA, "dateField"));

            List<String> groups = (List<String>) dm.getProperty(SCHEMA, "groups");
            assertEquals(2, groups.size());
            assertTrue(groups.contains("administrators"));
            assertTrue(groups.contains("members"));

            // password check
            assertTrue(session.authenticate("user_0", "pass_0"));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetEntry() throws Exception {
        try (Session session = getSession()) {
            DocumentModel dm = session.getEntry("user_1");
            assertEquals("user_1", dm.getProperty(SCHEMA, "username"));
            assertEquals(3L, dm.getProperty(SCHEMA, "intField"));
            assertCalendarEquals(getCalendar(2007, 9, 7, 14, 36, 28, 0), (Calendar) dm.getProperty(SCHEMA, "dateField"));
            assertNull(dm.getProperty(SCHEMA, "company"));
            List<String> groups = (List<String>) dm.getProperty(SCHEMA, "groups");
            assertEquals(2, groups.size());
            assertTrue(groups.contains("group_1"));
            assertTrue(groups.contains("members"));

            dm = session.getEntry("Administrator");
            assertEquals("Administrator", dm.getProperty(SCHEMA, "username"));
            assertEquals(10L, dm.getProperty(SCHEMA, "intField"));
            assertCalendarEquals(getCalendar(1982, 3, 25, 16, 30, 47, 123),
                    (Calendar) dm.getProperty(SCHEMA, "dateField"));
            assertTrue((Boolean) dm.getProperty(SCHEMA, "booleanField"));
            groups = (List<String>) dm.getProperty(SCHEMA, "groups");
            assertEquals(1, groups.size());
            assertTrue(groups.contains("administrators"));
            // assertTrue(groups.contains("members"));

        }
    }

    @Test
    public void testGetEntries() throws Exception {
        try (Session session = getSession()) {
            DocumentModelList entries = session.getEntries();

            assertEquals(3, entries.size());

            Map<String, DocumentModel> entryMap = new HashMap<>();
            for (DocumentModel entry : entries) {
                entryMap.put(entry.getId(), entry);
            }

            DocumentModel dm = entryMap.get("user_1");
            assertNotNull(dm);
            assertEquals("user_1", dm.getProperty(SCHEMA, "username"));
            assertCalendarEquals(getCalendar(2007, 9, 7, 14, 36, 28, 0), (Calendar) dm.getProperty(SCHEMA, "dateField"));
            assertEquals(3L, dm.getProperty(SCHEMA, "intField"));
            assertTrue((Boolean) dm.getProperty(SCHEMA, "booleanField"));
            // XXX: getEntries does not fetch references anymore => groups is
            // null

            dm = entryMap.get("Administrator");
            assertNotNull(dm);
            assertEquals("Administrator", dm.getProperty(SCHEMA, "username"));
            assertEquals(10L, dm.getProperty(SCHEMA, "intField"));
            assertCalendarEquals(getCalendar(1982, 3, 25, 16, 30, 47, 123),
                    (Calendar) dm.getProperty(SCHEMA, "dateField"));

            dm = entryMap.get("user_3");
            assertFalse((Boolean) dm.getProperty(SCHEMA, "booleanField"));
        }

        try (Session session = directoryService.open(GROUP_DIR)){
            DocumentModel doc = session.getEntry("administrators");
            assertEquals("administrators", doc.getPropertyValue("group:groupname"));
            assertEquals("Administrators group", doc.getPropertyValue("group:grouplabel"));

            doc = session.getEntry("group_1");
            assertEquals("group_1", doc.getPropertyValue("group:groupname"));
            Serializable label = doc.getPropertyValue("group:grouplabel");
            if (label != null) {
                // NULL for Oracle
                assertEquals("", label);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateEntry() throws Exception {
        try (Session session = getSession()) {
            DocumentModel dm = session.getEntry("user_1");

            // update entry
            dm.setProperty(SCHEMA, "username", "user_2");
            dm.setProperty(SCHEMA, "password", "pass_2");
            dm.setProperty(SCHEMA, "intField", 2L);
            dm.setProperty(SCHEMA, "dateField", getCalendar(2001, 2, 3, 4, 5, 6, 7));
            dm.setProperty(SCHEMA, "groups", Arrays.asList("administrators", "members"));
            session.updateEntry(dm);
        }

            // retrieve entry again
            // even if we tried to change the user id (username), it should
            // not be changed
        try (Session session = getSession()) {
            DocumentModel dm = session.getEntry("user_1");
            assertEquals("user_1", dm.getProperty(SCHEMA, "username"));

            assertEquals(2L, dm.getProperty(SCHEMA, "intField"));
            assertCalendarEquals(getCalendar(2001, 2, 3, 4, 5, 6, 7), (Calendar) dm.getProperty(SCHEMA, "dateField"));
            List<String> groups = (List<String>) dm.getProperty(SCHEMA, "groups");
            assertEquals(2, groups.size());
            assertTrue(groups.contains("administrators"));
            assertTrue(groups.contains("members"));

            // password check
            assertTrue(session.authenticate("user_1", "pass_2"));

            // the user_2 username change was ignored
            assertNull(session.getEntry("user_2"));

            // change other field, check password still ok
            dm.setProperty(SCHEMA, "company", "foo");
            session.updateEntry(dm);
        }
        try (Session session = getSession()) {
            // password check
            assertTrue(session.authenticate("user_1", "pass_2"));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteEntry() throws Exception {
        try (Session session = getSession()) {
            DocumentModel dm = session.getEntry("user_1");
            session.deleteEntry(dm);
        }

        try (Session session = getSession()) {
            DocumentModel dm = session.getEntry("user_1");
            assertNull(dm);
        }

        try (Session session = directoryService.open(GROUP_DIR)) {
            DocumentModel group1 = session.getEntry("group_1");
            List<String> members = (List<String>) group1.getProperty("group", "members");
            assertTrue(members.isEmpty());
        }
    }

    // XXX AT: disabled because SQL directories do not accept anymore creation
    // of a second entry with the same id. The goal here is to accept an entry
    // with an existing id, as long as parent id is different - e.g full id is
    // the (parent id, id) tuple. But this constraint does not appear the
    // directory configuration, so drop it for now.
    @Test
    @Ignore
    public void testDeleteEntryExtended() throws Exception {
        try (Session session = getSession()) {
            // create a second entry with user_1 as key but with
            // a different email (would be "parent" in a hierarchical
            // vocabulary)
            Map<String, Object> entryMap = new HashMap<>();
            entryMap.put("username", "user_1");
            entryMap.put("email", "second@email");
            DocumentModel dm = session.createEntry(entryMap);
            assertNotNull(dm);
            assertEquals(3, session.getEntries().size());

            // delete with nonexisting email
            Map<String, String> map = new HashMap<String, String>();
            map.put("email", "nosuchemail");
            session.deleteEntry("user_1", map);
            // still there
            assertEquals(3, session.getEntries().size());

            // delete just one
            map.put("email", "e@m");
            session.deleteEntry("user_1", map);
            // two more entries left
            assertEquals(2, session.getEntries().size());

            // other user_1 still present
            dm = session.getEntry("user_1");
            assertEquals("second@email", dm.getProperty(SCHEMA, "email"));

            // delete it with a WHERE on a null key
            map.clear();
            map.put("company", null);
            session.deleteEntry("user_1", map);
            // entry is gone, only Administrator left
            assertEquals(1, session.getEntries().size());

        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testQuery1() throws Exception {
        try (Session session = getSession()) {
            Map<String, Serializable> filter = new HashMap<>();
            filter.put("username", "user_1");
            filter.put("firstName", "f");
            DocumentModelList list = session.query(filter);
            assertEquals(1, list.size());
            DocumentModel docModel = list.get(0);
            assertNotNull(docModel);
            assertEquals("user_1", docModel.getProperty(SCHEMA, "username"));
            assertEquals("f", docModel.getProperty(SCHEMA, "firstName"));

            // simple query does not fetch references by default => restart with
            // an explicit fetch request
            List<String> groups = (List<String>) docModel.getProperty(SCHEMA, "groups");
            assertTrue(groups.isEmpty());

            list = session.query(filter, null, null, true);
            assertEquals(1, list.size());
            docModel = list.get(0);
            assertNotNull(docModel);
            assertEquals("user_1", docModel.getProperty(SCHEMA, "username"));
            assertEquals("f", docModel.getProperty(SCHEMA, "firstName"));

            // test that the groups (reference) of user_1 were fetched as well
            groups = (List<String>) docModel.getProperty(SCHEMA, "groups");
            assertEquals(2, groups.size());
            assertTrue(groups.contains("members"));
            assertTrue(groups.contains("group_1"));

        }
    }

    @Test
    public void testQuerySubAny() throws Exception {
        try (Session session = getSession()) {
            Map<String, Serializable> filter = new HashMap<>();
            filter.put("username", "er_");
            Set<String> set = new HashSet<String>();
            set.add("username");
            DocumentModelList list = session.query(filter, set);
            assertEquals(2, list.size());
            Set<String> usernames = new HashSet<>();
            for (DocumentModel docModel : list) {
                usernames.add((String) docModel.getProperty(SCHEMA, "username"));
            }
            Set<String> expectedUsernames = new HashSet<>(Arrays.asList("user_1", "user_3"));
            assertEquals(expectedUsernames, usernames);
        }
    }

    @Test
    public void testQuery2() throws Exception {
        try (Session session = getSession()) {
            Map<String, Serializable> filter = new HashMap<>();
            filter.put("username", "user_1");
            filter.put("email", "nosuchemail");
            DocumentModelList list = session.query(filter);
            assertEquals(0, list.size());
        }
    }

    @Test
    public void testQueryCaseInsensitive() throws Exception {
        try (Session session = getSession()) {
            Map<String, Serializable> filter = new HashMap<>();
            // case insensitive substring search
            filter.put("username", "admini");
            DocumentModelList list = session.query(filter, filter.keySet());
            assertEquals(1, list.size());
            DocumentModel dm = list.get(0);
            assertEquals("Administrator", dm.getProperty(SCHEMA, "username"));
        }
    }

    @Test
    public void testGetProjection() throws Exception {
        try (Session session = getSession()) {
            Map<String, Serializable> filter = new HashMap<>();
            filter.put("username", "user_1");
            List<String> list = session.getProjection(filter, "firstName");
            assertEquals(1, list.size());
            assertTrue(list.contains("f"));
        }
    }

    @Test
    public void testSearch() throws Exception {
        try (Session session = getSession()) {
            Map<String, Serializable> filter = new HashMap<>();

            // exact match
            filter.put("username", "u");
            List<String> users = session.getProjection(filter, "username");
            assertEquals(0, users.size());

            // substring match
            users = session.getProjection(filter, filter.keySet(), "username");
            assertEquals(2, users.size());
            assertTrue(users.contains("user_1"));

            filter.put("username", "v");
            users = session.getProjection(filter, filter.keySet(), "username");
            assertEquals(0, users.size());

            // trying to cheat
            filter.put("username", "*");
            users = session.getProjection(filter, "username");
            assertEquals(0, users.size());

            // substring with empty key
            filter.put("username", "");
            users = session.getProjection(filter, filter.keySet(), "username");
            assertEquals(3, users.size());
            assertTrue(users.contains("user_1"));
            assertTrue(users.contains("Administrator"));

        }
    }

    @Test
    public void testIsAuthenticating() throws Exception {
        try (Session session = getSession()) {
            // by default the user directory is authenticating
            assertTrue(session.isAuthenticating());

            // by setting a password field that does not belong to the
            // user SCHEMA, we disable that feature
            BaseDirectoryDescriptor config = ((AbstractDirectory) getDirectory()).getDescriptor();
            try {
                config.passwordField = "SomeStrangePassordField";
                assertFalse(session.isAuthenticating());
            } finally {
                config.passwordField = "password";
            }

        }
    }

    @Test
    public void testAuthenticate() throws Exception {
        try (Session session = getSession()) {
            // successful authentication
            assertTrue(session.authenticate("Administrator", "Administrator"));
            assertTrue(session.authenticate("user_1", "pass_1"));

            // authentication against encrypted password
            assertTrue(session.authenticate("user_3", "pass_3"));

            // failed authentication: bad password
            assertFalse(session.authenticate("Administrator", "WrongPassword"));
            assertFalse(session.authenticate("user", ".asdf'23423"));

            // failed authentication: not existing user
            assertFalse(session.authenticate("NonExistingUser", "whatever"));
        }
    }

    @Test
    public void testCreateFromModel() throws Exception {
        try (Session session = getSession()) {
            String schema = "user";
            DocumentModel entry = BaseSession.createEntryModel(null, schema, null, null);
            entry.setProperty("user", "username", "yo");

            assertNull(session.getEntry("yo"));
            session.createEntry(entry);
            assertNotNull(session.getEntry("yo"));

            // create one with existing same id, must fail
            entry.setProperty("user", "username", "Administrator");
            try {
                assertTrue(session.hasEntry("Administrator"));
                entry = session.createEntry(entry);
                session.getEntry("Administrator");
                fail("Should raise an error, entry already exists");
            } catch (DirectoryException e) {
            }
        }
    }

    @Test
    public void testHasEntry() throws Exception {
        try (Session session = getSession()) {
            assertTrue(session.hasEntry("Administrator"));
            assertFalse(session.hasEntry("foo"));
        }
    }

    @Ignore
    @Test
    @LocalDeploy("org.nuxeo.ecm.directory.sql.tests:test-sql-directories-alteration-config.xml")
    public void testColumnCreation() throws Exception {
        AbstractDirectory dirtmp1 = null;
        AbstractDirectory dirtmp2 = null;

        try {
            dirtmp1 = (AbstractDirectory) directoryService.getDirectory("tmpdirectory1");
            assertNotNull(dirtmp1);

            Session session = dirtmp1.getSession();

            String schema1 = "tmpschema1";
            DocumentModel entry = BaseSession.createEntryModel(null, schema1, null, null);
            entry.setProperty(schema1, "id", "john");
            entry.setProperty(schema1, "label", "monLabel");

            assertNull(session.getEntry("john"));
            entry = session.createEntry(entry);
            assertEquals("john", entry.getId());
            assertNotNull(session.getEntry("john"));

            // Open a new directory that uses the same table with a different
            // schema.
            // And test if the table has not been re-created, and data are there
            dirtmp2 = (AbstractDirectory) directoryService.getDirectory("tmpdirectory2");
            assertNotNull(dirtmp2);

            session = dirtmp2.getSession();
            assertNotNull(session.getEntry("john"));
        } finally {
            if (dirtmp1 != null) {
                dirtmp1.shutdown();
            }
            if (dirtmp2 != null) {
                dirtmp2.shutdown();
            }
        }
    }

    @Test
    public void testPasswordIgnoredInQueryFilter() throws Exception {
        try (Session session = getSession()) {
            Map<String, Serializable> filter = new HashMap<>();
            filter.put("username", "user_1");
            filter.put("password", "nosuchpassword");
            DocumentModelList list = session.query(filter);
            // returns the result of the query filtered by username, ignoring password
            assertEquals(1, list.size());
        }
    }

    @Test
    public void testPasswordNotReturned() throws Exception {
        String username = "myuser";
        String password = "MyPassword:)";
        // create entry
        try (Session session = getSession()) {
            Map<String, Object> map = new HashMap<>();
            map.put("username", username);
            map.put("password", password);
            session.createEntry(map);

            // check authentication
            assertTrue(session.authenticate(username, password));
        }
        try (Session session = getSession()) {
            // password not returned by getEntry
            DocumentModel dm = session.getEntry(username);
            String pw = (String) dm.getProperty(SCHEMA, "password");
            assertNull(pw);

            // password not returned by query
            List<DocumentModel> docs = session.query(Collections.singletonMap("username", username));
            assertEquals(1, docs.size());
            dm = docs.get(0);
            pw = (String) dm.getProperty(SCHEMA, "password");
            assertNull(pw);

        }
        // however is readAllColumns is set, the password is returned (used for test framework)
        try (Session session = getSession()) {
            session.setReadAllColumns(true);

            // password returned by getEntry
            DocumentModel dm = session.getEntry(username);
            String pw = (String) dm.getProperty(SCHEMA, "password");
            assertFalse(StringUtils.isBlank(pw)); // hashed password
            PasswordHelper.verifyPassword(password, pw);

            // password returned by query
            List<DocumentModel> docs = session.query(Collections.singletonMap("username", username));
            assertEquals(1, docs.size());
            dm = docs.get(0);
            pw = (String) dm.getProperty(SCHEMA, "password");
            assertFalse(StringUtils.isBlank(pw)); // hashed password
            PasswordHelper.verifyPassword(password, pw);
        }
    }

}
