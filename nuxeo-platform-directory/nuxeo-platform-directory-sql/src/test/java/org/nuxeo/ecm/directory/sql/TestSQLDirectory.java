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
 * $Id$
 */

package org.nuxeo.ecm.directory.sql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.directory.AbstractDirectory;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.PasswordHelper;
import org.nuxeo.ecm.directory.Reference;
import org.nuxeo.ecm.directory.Session;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 * 
 */
public class TestSQLDirectory extends SQLDirectoryTestCase {

    private static final String SCHEMA = "user";

    public static Calendar getCalendar(int year, int month, int day, int hours,
            int minutes, int seconds, int milliseconds) {
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

    public static void assertCalendarEquals(Calendar expected, Calendar actual)
            throws Exception {
        if (expected.equals(actual)) {
            return;
        }
        if (!DatabaseHelper.DATABASE.hasSubSecondResolution()) {
            // try without milliseconds for stupid MySQL
            if (stripMillis(expected).equals(stripMillis(actual))) {
                return;
            }
        }
        assertEquals(expected, actual); // proper failure
    }

    public static Session getSession() throws Exception {
        return getSession("userDirectory");
    }

    public static SQLDirectory getSQLDirectory() throws Exception {
        Directory dir = getDirectory("userDirectory");
        if (dir instanceof SQLDirectoryProxy) {
            dir = ((SQLDirectoryProxy) dir).getDirectory();
        }
        return (SQLDirectory) dir;
    }

    @Test
    public void testTableReference() throws Exception {
        Session groupSession = getSession("groupDirectory");
        try {
            Reference membersRef = getDirectory("groupDirectory").getReference(
                    "members");

            // test initial configuration
            List<String> administrators = membersRef.getTargetIdsForSource("administrators");
            assertEquals(1, administrators.size());
            assertTrue(administrators.contains("Administrator"));

            // add user_1 to the administrators group
            membersRef.addLinks("administrators", Arrays.asList("user_1"));

            administrators = membersRef.getTargetIdsForSource("administrators");
            assertEquals(2, administrators.size());
            assertTrue(administrators.contains("Administrator"));
            assertTrue(administrators.contains("user_1"));

            // readding the same link should not duplicate it
            membersRef.addLinks("administrators",
                    Arrays.asList("user_1", "user_2"));

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

            // readd references with the set* methods

            membersRef.setTargetIdsForSource("administrators",
                    Arrays.asList("user_1", "user_2"));
            administrators = membersRef.getTargetIdsForSource("administrators");
            assertEquals(2, administrators.size());
            assertTrue(administrators.contains("user_1"));
            assertTrue(administrators.contains("user_2"));

            membersRef.setTargetIdsForSource("administrators",
                    Arrays.asList("user_1", "Administrator"));
            administrators = membersRef.getTargetIdsForSource("administrators");
            assertEquals(2, administrators.size());
            assertTrue(administrators.contains("user_1"));
            assertTrue(administrators.contains("Administrator"));

            membersRef.setSourceIdsForTarget("Administrator",
                    Arrays.asList("members"));
            administrators = membersRef.getTargetIdsForSource("administrators");
            assertEquals(1, administrators.size());
            assertTrue(administrators.contains("user_1"));

            administrators = membersRef.getSourceIdsForTarget("Administrator");
            assertEquals(1, administrators.size());
            assertTrue(administrators.contains("members"));

        } finally {
            groupSession.close();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateEntry() throws Exception {
        Session session = getSession();
        assertNotNull(session);
        try {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("username", "user_0");
            map.put("password", "pass_0");
            map.put("intField", Long.valueOf(5));
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
            assertEquals(Long.valueOf(5), dm.getProperty(SCHEMA, "intField"));
            assertCalendarEquals(getCalendar(1982, 3, 25, 16, 30, 47, 0),
                    (Calendar) dm.getProperty(SCHEMA, "dateField"));

            List<String> groups = (List<String>) dm.getProperty(SCHEMA,
                    "groups");
            assertEquals(2, groups.size());
            assertTrue(groups.contains("administrators"));
            assertTrue(groups.contains("members"));

            session.commit();
        } finally {
            session.close();
        }

        // recheck the created entries has really been created from a second
        // session
        session = getSession();
        assertNotNull(session);
        try {
            DocumentModel dm = session.getEntry("user_0");
            assertNotNull(dm);

            assertEquals("user_0", dm.getId());

            String[] schemaNames = dm.getSchemas();
            assertEquals(1, schemaNames.length);

            assertEquals(SCHEMA, schemaNames[0]);

            assertEquals("user_0", dm.getProperty(SCHEMA, "username"));
            String password = (String) dm.getProperty(SCHEMA, "password");
            assertFalse("pass_0".equals(password));
            assertTrue(PasswordHelper.verifyPassword("pass_0", password));
            assertEquals(Long.valueOf(5), dm.getProperty(SCHEMA, "intField"));
            assertCalendarEquals(getCalendar(1982, 3, 25, 16, 30, 47, 0),
                    (Calendar) dm.getProperty(SCHEMA, "dateField"));

            List<String> groups = (List<String>) dm.getProperty(SCHEMA,
                    "groups");
            assertEquals(2, groups.size());
            assertTrue(groups.contains("administrators"));
            assertTrue(groups.contains("members"));

        } finally {
            session.close();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetEntry() throws Exception {
        Session session = getSession();
        try {
            DocumentModel dm = session.getEntry("user_1");
            assertEquals("user_1", dm.getProperty(SCHEMA, "username"));
            assertEquals("pass_1", dm.getProperty(SCHEMA, "password"));
            assertEquals(Long.valueOf(3), dm.getProperty(SCHEMA, "intField"));
            assertCalendarEquals(getCalendar(2007, 9, 7, 14, 36, 28, 0),
                    (Calendar) dm.getProperty(SCHEMA, "dateField"));
            assertNull(dm.getProperty(SCHEMA, "company"));
            List<String> groups = (List<String>) dm.getProperty(SCHEMA,
                    "groups");
            assertEquals(2, groups.size());
            assertTrue(groups.contains("group_1"));
            assertTrue(groups.contains("members"));

            dm = session.getEntry("Administrator");
            assertEquals("Administrator", dm.getProperty(SCHEMA, "username"));
            assertEquals("Administrator", dm.getProperty(SCHEMA, "password"));
            assertEquals(Long.valueOf(10), dm.getProperty(SCHEMA, "intField"));
            assertCalendarEquals(getCalendar(1982, 3, 25, 16, 30, 47, 123),
                    (Calendar) dm.getProperty(SCHEMA, "dateField"));
            assertTrue((Boolean) dm.getProperty(SCHEMA, "booleanField"));
            groups = (List<String>) dm.getProperty(SCHEMA, "groups");
            assertEquals(1, groups.size());
            assertTrue(groups.contains("administrators"));
            // assertTrue(groups.contains("members"));

        } finally {
            session.close();
        }
    }

    @Test
    public void testGetEntries() throws Exception {
        Session session = getSession();
        try {
            DocumentModelList entries = session.getEntries();

            assertEquals(3, entries.size());

            Map<String, DocumentModel> entryMap = new HashMap<String, DocumentModel>();
            for (DocumentModel entry : entries) {
                entryMap.put(entry.getId(), entry);
            }

            DocumentModel dm = entryMap.get("user_1");
            assertNotNull(dm);
            assertEquals("user_1", dm.getProperty(SCHEMA, "username"));
            assertEquals("pass_1", dm.getProperty(SCHEMA, "password"));
            assertCalendarEquals(getCalendar(2007, 9, 7, 14, 36, 28, 0),
                    (Calendar) dm.getProperty(SCHEMA, "dateField"));
            assertEquals(Long.valueOf(3), dm.getProperty(SCHEMA, "intField"));
            assertTrue((Boolean) dm.getProperty(SCHEMA, "booleanField"));
            // XXX: getEntries does not fetch references anymore => groups is
            // null

            dm = entryMap.get("Administrator");
            assertNotNull(dm);
            assertEquals("Administrator", dm.getProperty(SCHEMA, "username"));
            assertEquals("Administrator", dm.getProperty(SCHEMA, "password"));
            assertEquals(Long.valueOf(10), dm.getProperty(SCHEMA, "intField"));
            assertCalendarEquals(getCalendar(1982, 3, 25, 16, 30, 47, 123),
                    (Calendar) dm.getProperty(SCHEMA, "dateField"));

            dm = entryMap.get("user_3");
            assertFalse((Boolean) dm.getProperty(SCHEMA, "booleanField"));
        } finally {
            session.close();
        }

        session = getSession("groupDirectory");
        try {
            DocumentModel doc = session.getEntry("administrators");
            assertEquals("administrators",
                    doc.getPropertyValue("group:groupname"));
            assertEquals("Administrators group",
                    doc.getPropertyValue("group:grouplabel"));

            doc = session.getEntry("group_1");
            assertEquals("group_1", doc.getPropertyValue("group:groupname"));
            Serializable label = doc.getPropertyValue("group:grouplabel");
            if (label != null) {
                // NULL for Oracle
                assertEquals("", label);
            }
        } finally {
            session.close();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateEntry() throws Exception {
        Session session = getSession();
        try {
            DocumentModel dm = session.getEntry("user_1");

            // update entry
            dm.setProperty(SCHEMA, "username", "user_2");
            dm.setProperty(SCHEMA, "password", "pass_2");
            dm.setProperty(SCHEMA, "intField", Long.valueOf(2));
            dm.setProperty(SCHEMA, "dateField",
                    getCalendar(2001, 2, 3, 4, 5, 6, 7));
            dm.setProperty(SCHEMA, "groups",
                    Arrays.asList("administrators", "members"));
            session.updateEntry(dm);
            session.commit();
            session.close();

            // retrieve entry again
            // even if we tried to change the user id (username), it should
            // not be changed

            session = getSession();
            dm = session.getEntry("user_1");
            assertEquals("user_1", dm.getProperty(SCHEMA, "username"));
            String password = (String) dm.getProperty(SCHEMA, "password");
            assertFalse("pass_2".equals(password));
            assertTrue(PasswordHelper.verifyPassword("pass_2", password));
            assertEquals(Long.valueOf(2), dm.getProperty(SCHEMA, "intField"));
            assertCalendarEquals(getCalendar(2001, 2, 3, 4, 5, 6, 7),
                    (Calendar) dm.getProperty(SCHEMA, "dateField"));
            List<String> groups = (List<String>) dm.getProperty(SCHEMA,
                    "groups");
            assertEquals(2, groups.size());
            assertTrue(groups.contains("administrators"));
            assertTrue(groups.contains("members"));

            // the user_2 username change was ignored
            assertNull(session.getEntry("user_2"));

            // change other field, check password still ok
            dm.setProperty(SCHEMA, "company", "foo");
            session.updateEntry(dm);
            session.commit();
            session.close();
            session = getSession();
            dm = session.getEntry("user_1");
            password = (String) dm.getProperty(SCHEMA, "password");
            assertFalse("pass_2".equals(password));
            assertTrue(PasswordHelper.verifyPassword("pass_2", password));
        } finally {
            session.close();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteEntry() throws Exception {
        Session session = getSession();
        try {
            DocumentModel dm = session.getEntry("user_1");
            session.deleteEntry(dm);
            session.commit();
            session.close();

            session = getSession();
            dm = session.getEntry("user_1");
            assertNull(dm);
            session.close();

            session = getSession("groupDirectory");
            DocumentModel group1 = session.getEntry("group_1");
            List<String> members = (List<String>) group1.getProperty("group",
                    "members");
            assertTrue(members.isEmpty());
            // assertFalse(members.contains("group_1"));
        } finally {
            session.close();
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
        Session session = getSession();
        try {
            // create a second entry with user_1 as key but with
            // a different email (would be "parent" in a hierarchical
            // vocabulary)
            Map<String, Object> entryMap = new HashMap<String, Object>();
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

        } finally {
            session.close();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testQuery1() throws Exception {
        Session session = getSession();
        try {
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
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
            List<String> groups = (List<String>) docModel.getProperty(SCHEMA,
                    "groups");
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

        } finally {
            session.close();
        }
    }

    @Test
    public void testQuerySubAny() throws Exception {
        Session session = getSession();
        try {
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            filter.put("username", "er_");
            Set<String> set = new HashSet<String>();
            set.add("username");
            DocumentModelList list = session.query(filter, set);
            assertEquals(2, list.size());
            DocumentModel docModel = list.get(0);
            assertNotNull(docModel);
            assertEquals("user_1", docModel.getProperty(SCHEMA, "username"));
        } finally {
            session.close();
        }
    }

    @Test
    public void testQuery2() throws Exception {
        Session session = getSession();
        try {
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            filter.put("username", "user_1");
            filter.put("password", "pass_x"); // no such password
            DocumentModelList list = session.query(filter);
            assertEquals(0, list.size());
        } finally {
            session.close();
        }
    }

    @Test
    public void testQueryCaseInsensitive() throws Exception {
        Session session = getSession();
        try {
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            // case insensitive substring search
            filter.put("username", "admini");
            DocumentModelList list = session.query(filter, filter.keySet());
            assertEquals(1, list.size());
            DocumentModel dm = list.get(0);
            assertEquals("Administrator", dm.getProperty(SCHEMA, "username"));
        } finally {
            session.close();
        }
    }

    @Test
    public void testGetProjection() throws Exception {
        Session session = getSession();
        try {
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            filter.put("username", "user_1");
            List<String> list = session.getProjection(filter, "password");
            assertEquals(1, list.size());
            assertTrue(list.contains("pass_1"));
        } finally {
            session.close();
        }
    }

    @Test
    public void testSearch() throws Exception {
        Session session = getSession();
        try {
            Map<String, Serializable> filter = new HashMap<String, Serializable>();

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

        } finally {
            session.close();
        }
    }

    @Test
    public void testIsAuthenticating() throws Exception {
        Session session = getSession();
        try {
            // by default the user directory is authenticating
            assertTrue(session.isAuthenticating());

            // by setting a password field that does not belong to the
            // user SCHEMA, we disable that feature
            SQLDirectory directory = getSQLDirectory();
            directory.getConfig().setPasswordField("SomeStrangePassordField");

            assertFalse(session.isAuthenticating());

        } finally {
            session.close();
        }
    }

    @Test
    public void testAuthenticate() throws Exception {
        Session session = getSession();
        try {
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

        } finally {
            session.close();
        }
    }

    @Test
    public void testCreateFromModel() throws Exception {
        Session session = getSession();
        try {
            String schema = "user";
            DocumentModel entry = BaseSession.createEntryModel(null, schema,
                    null, null);
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
        } finally {
            session.close();
        }
    }

    @Test
    public void testHasEntry() throws Exception {
        Session session = getSession();
        try {
            assertTrue(session.hasEntry("Administrator"));
            assertFalse(session.hasEntry("foo"));
        } finally {
            session.close();
        }
    }

    @Test
    public void testColumnCreation() throws Exception {
        deployContrib("org.nuxeo.ecm.directory.sql.tests",
                "test-sql-directories-alteration-config.xml");

        AbstractDirectory dirtmp1 = null;
        AbstractDirectory dirtmp2 = null;

        try {
            dirtmp1 = (AbstractDirectory) getDirectory("tmpdirectory1");
            assertNotNull(dirtmp1);

            Session session = dirtmp1.getSession();

            String schema1 = "tmpschema1";
            DocumentModel entry = BaseSession.createEntryModel(null, schema1,
                    null, null);
            entry.setProperty(schema1, "id", "john");
            entry.setProperty(schema1, "label", "monLabel");

            assertNull(session.getEntry("john"));
            entry = session.createEntry(entry);
            assertEquals("john", entry.getId());
            assertNotNull(session.getEntry("john"));

            // Open a new directory that uses the same table with a different
            // schema.
            // And test if the table has not been re-created, and data are there
            dirtmp2 = (AbstractDirectory) getDirectory("tmpdirectory2");
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
    public void testSchemaWithPrefix() throws Exception {
        deployContrib("org.nuxeo.ecm.directory.sql.tests",
                "test-sql-directories-schema-prefix.xml");
        Session session = getSession();
        try {
            DocumentModel dm = session.getEntry("user_1");
            assertEquals("user_1", dm.getProperty(SCHEMA, "username"));
        } finally {
            session.close();
        }
    }

}
