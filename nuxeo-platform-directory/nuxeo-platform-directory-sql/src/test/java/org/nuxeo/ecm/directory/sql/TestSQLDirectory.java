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

import javax.inject.Named;
import javax.security.auth.login.LoginContext;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.model.FrameworkMethod;
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
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature.Identity;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ ClientLoginFeature.class, SQLDirectoryFeature.class })
@LocalDeploy({"org.nuxeo.ecm.directory.sql.tests:test-sql-directories-schema-override.xml",
        "org.nuxeo.ecm.directory.sql.tests:test-sql-directories-schema-override.xml",
        "org.nuxeo.ecm.directory.sql.tests:test-sql-directories-bundle.xml" })
@ClientLoginFeature.Opener(TestSQLDirectory.Opener.class)
@Identity(administrator = true)
public class TestSQLDirectory {

    private static final String SCHEMA = "user";

    @Inject
    @Named(SQLDirectoryFeature.USER_DIRECTORY_NAME)
    Directory userDir;

    @Inject
    @Named(SQLDirectoryFeature.GROUP_DIRECTORY_NAME)
    Directory groupDir;

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

    protected Session userDirSession;

    protected Session groupDirSession;

    protected void open() {
        userDirSession = userDir.getSession();
        groupDirSession = groupDir.getSession();
    }

    protected void close() {
        userDirSession.close();
        groupDirSession.close();
    }

    public class Opener implements ClientLoginFeature.Listener {

        @Override
        public void onLogin(FeaturesRunner runner, FrameworkMethod method,
                LoginContext context) {
            open();
        }

        @Override
        public void onLogout(FeaturesRunner runner, FrameworkMethod method,
                LoginContext context) {
            close();
        }

    }

    @Test
    public void testTableReference() throws Exception {
        Reference membersRef = groupDir.getReference("members");

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
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateEntry() throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("username", "user_0");
        map.put("password", "pass_0");
        map.put("intField", Long.valueOf(5));
        map.put("dateField", getCalendar(1982, 3, 25, 16, 30, 47, 0));
        map.put("groups", Arrays.asList("members", "administrators"));
        DocumentModel dm = userDirSession.createEntry(map);
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

        List<String> groups = (List<String>) dm.getProperty(SCHEMA, "groups");
        assertEquals(2, groups.size());
        assertTrue(groups.contains("administrators"));
        assertTrue(groups.contains("members"));

        DocumentModel dm1 = userDirSession.getEntry("user_0");
        assertNotNull(dm1);

        assertEquals("user_0", dm1.getId());

        schemaNames = dm1.getSchemas();
        assertEquals(1, schemaNames.length);

        assertEquals(SCHEMA, schemaNames[0]);

        assertEquals("user_0", dm1.getProperty(SCHEMA, "username"));
        String password = (String) dm1.getProperty(SCHEMA, "password");
        assertFalse("pass_0".equals(password));
        assertTrue(PasswordHelper.verifyPassword("pass_0", password));
        assertEquals(Long.valueOf(5), dm1.getProperty(SCHEMA, "intField"));
        assertCalendarEquals(getCalendar(1982, 3, 25, 16, 30, 47, 0),
                (Calendar) dm1.getProperty(SCHEMA, "dateField"));

        groups = (List<String>) dm1.getProperty(SCHEMA, "groups");
        assertEquals(2, groups.size());
        assertTrue(groups.contains("administrators"));
        assertTrue(groups.contains("members"));

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetEntry() throws Exception {
        DocumentModel dm = userDirSession.getEntry("user_1");
        assertEquals("user_1", dm.getProperty(SCHEMA, "username"));
        //XXX the password has been hash during restoring in session
        //Authenticate test should be enough
        //assertEquals("pass_1", dm.getProperty(SCHEMA, "password"));
        assertEquals(Long.valueOf(3), dm.getProperty(SCHEMA, "intField"));
        assertCalendarEquals(getCalendar(2007, 9, 7, 14, 36, 28, 0),
                (Calendar) dm.getProperty(SCHEMA, "dateField"));
        assertNull(dm.getProperty(SCHEMA, "company"));
        List<String> groups = (List<String>) dm.getProperty(SCHEMA, "groups");
        assertEquals(2, groups.size());
        assertTrue(groups.contains("group_1"));
        assertTrue(groups.contains("members"));

        dm = userDirSession.getEntry("Administrator");
        assertEquals("Administrator", dm.getProperty(SCHEMA, "username"));
        //XXX the password has been hash during restoring in session
        //Authenticate test should be enough

        //assertEquals("Administrator", dm.getProperty(SCHEMA, "password"));
        assertEquals(Long.valueOf(10), dm.getProperty(SCHEMA, "intField"));
        assertCalendarEquals(getCalendar(1982, 3, 25, 16, 30, 47, 123),
                (Calendar) dm.getProperty(SCHEMA, "dateField"));
        assertTrue((Boolean) dm.getProperty(SCHEMA, "booleanField"));
        groups = (List<String>) dm.getProperty(SCHEMA, "groups");
        assertEquals(1, groups.size());
        assertTrue(groups.contains("administrators"));
        // assertTrue(groups.contains("members"));

    }

    @Test
    public void testGetEntries() throws Exception {
        DocumentModelList entries = userDirSession.getEntries();

        assertEquals(3, entries.size());

        Map<String, DocumentModel> entryMap = new HashMap<String, DocumentModel>();
        for (DocumentModel entry : entries) {
            entryMap.put(entry.getId(), entry);
        }

        DocumentModel dm = entryMap.get("user_1");
        assertNotNull(dm);
        assertEquals("user_1", dm.getProperty(SCHEMA, "username"));
        //XXX the password has been hash during restoring in session
        //Authenticate test should be enough
        //assertEquals("pass_1", dm.getProperty(SCHEMA, "password"));
        assertCalendarEquals(getCalendar(2007, 9, 7, 14, 36, 28, 0),
                (Calendar) dm.getProperty(SCHEMA, "dateField"));
        assertEquals(Long.valueOf(3), dm.getProperty(SCHEMA, "intField"));
        assertTrue((Boolean) dm.getProperty(SCHEMA, "booleanField"));
        // XXX: getEntries does not fetch references anymore => groups is
        // null

        dm = entryMap.get("Administrator");
        assertNotNull(dm);
        assertEquals("Administrator", dm.getProperty(SCHEMA, "username"));
        //XXX the password has been hash during restoring in session
        //Authenticate test should be enough
        //assertEquals("Administrator", dm.getProperty(SCHEMA, "password"));
        assertEquals(Long.valueOf(10), dm.getProperty(SCHEMA, "intField"));
        assertCalendarEquals(getCalendar(1982, 3, 25, 16, 30, 47, 123),
                (Calendar) dm.getProperty(SCHEMA, "dateField"));

        dm = entryMap.get("user_3");
        assertFalse((Boolean) dm.getProperty(SCHEMA, "booleanField"));

        DocumentModel doc = groupDirSession.getEntry("administrators");
        assertEquals("administrators", doc.getPropertyValue("group:groupname"));
        assertEquals("Administrators group",
                doc.getPropertyValue("group:grouplabel"));

        doc = groupDirSession.getEntry("group_1");
        assertEquals("group_1", doc.getPropertyValue("group:groupname"));
        Serializable label = doc.getPropertyValue("group:grouplabel");
        if (label != null) {
            // NULL for Oracle
            assertEquals("", label);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateEntry() throws Exception {
        DocumentModel dm = userDirSession.getEntry("user_1");

        // update entry
        dm.setProperty(SCHEMA, "username", "user_2");
        dm.setProperty(SCHEMA, "password", "pass_2");
        dm.setProperty(SCHEMA, "intField", Long.valueOf(2));
        dm.setProperty(SCHEMA, "dateField", getCalendar(2001, 2, 3, 4, 5, 6, 7));
        dm.setProperty(SCHEMA, "groups",
                Arrays.asList("administrators", "members"));
        userDirSession.updateEntry(dm);
        userDirSession.close();

        // retrieve entry again
        // even if we tried to change the user id (username), it should
        // not be changed

        dm = userDirSession.getEntry("user_1");
        assertEquals("user_1", dm.getProperty(SCHEMA, "username"));
        String password = (String) dm.getProperty(SCHEMA, "password");
        assertFalse("pass_2".equals(password));
        assertTrue(PasswordHelper.verifyPassword("pass_2", password));
        assertEquals(Long.valueOf(2), dm.getProperty(SCHEMA, "intField"));
        assertCalendarEquals(getCalendar(2001, 2, 3, 4, 5, 6, 7),
                (Calendar) dm.getProperty(SCHEMA, "dateField"));
        List<String> groups = (List<String>) dm.getProperty(SCHEMA, "groups");
        assertEquals(2, groups.size());
        assertTrue(groups.contains("administrators"));
        assertTrue(groups.contains("members"));

        // the user_2 username change was ignored
        assertNull(userDirSession.getEntry("user_2"));

        // change other field, check password still ok
        dm.setProperty(SCHEMA, "company", "foo");
        userDirSession.updateEntry(dm);
        userDirSession.close();
        dm = userDirSession.getEntry("user_1");
        password = (String) dm.getProperty(SCHEMA, "password");
        assertFalse("pass_2".equals(password));
        assertTrue(PasswordHelper.verifyPassword("pass_2", password));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteEntry() throws Exception {
        DocumentModel dm = userDirSession.getEntry("user_1");
        userDirSession.deleteEntry(dm);
        userDirSession.close();

        dm = userDirSession.getEntry("user_1");
        assertNull(dm);

        DocumentModel group1 = groupDirSession.getEntry("group_1");
        List<String> members = (List<String>) group1.getProperty("group",
                "members");
        assertTrue(members.isEmpty());
    }

    // XXX AT: disabled because SQL directories do not accept anymore creation
    // of a second entry with the same id. The goal here is to accept an entry
    // with an existing id, as long as parent id is different - e.g full id is
    // the (parent id, id) tuple. But this constraint does not appear the
    // directory configuration, so drop it for now.
    @Test
    @Ignore
    public void testDeleteEntryExtended() throws Exception {
        // create a second entry with user_1 as key but with
        // a different email (would be "parent" in a hierarchical
        // vocabulary)
        Map<String, Object> entryMap = new HashMap<String, Object>();
        entryMap.put("username", "user_1");
        entryMap.put("email", "second@email");
        DocumentModel dm = userDirSession.createEntry(entryMap);
        assertNotNull(dm);
        assertEquals(3, userDirSession.getEntries().size());

        // delete with nonexisting email
        Map<String, String> map = new HashMap<String, String>();
        map.put("email", "nosuchemail");
        userDirSession.deleteEntry("user_1", map);
        // still there
        assertEquals(3, userDirSession.getEntries().size());

        // delete just one
        map.put("email", "e@m");
        userDirSession.deleteEntry("user_1", map);
        // two more entries left
        assertEquals(2, userDirSession.getEntries().size());

        // other user_1 still present
        dm = userDirSession.getEntry("user_1");
        assertEquals("second@email", dm.getProperty(SCHEMA, "email"));

        // delete it with a WHERE on a null key
        map.clear();
        map.put("company", null);
        userDirSession.deleteEntry("user_1", map);
        // entry is gone, only Administrator left
        assertEquals(1, userDirSession.getEntries().size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testQuery1() throws Exception {
        Map<String, Serializable> filter = new HashMap<String, Serializable>();
        filter.put("username", "user_1");
        filter.put("firstName", "f");
        DocumentModelList list = userDirSession.query(filter);
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

        list = userDirSession.query(filter, null, null, true);
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

    @Test
    public void testQuerySubAny() throws Exception {
        Map<String, Serializable> filter = new HashMap<String, Serializable>();
        filter.put("username", "er_");
        Set<String> set = new HashSet<String>();
        set.add("username");
        DocumentModelList list = userDirSession.query(filter, set);
        assertEquals(2, list.size());
        DocumentModel docModel = list.get(0);
        assertNotNull(docModel);
    }

    @Test
    public void testQuery2() throws Exception {
        Map<String, Serializable> filter = new HashMap<String, Serializable>();
        filter.put("username", "user_1");
        filter.put("password", "pass_x"); // no such password
        DocumentModelList list = userDirSession.query(filter);
        assertEquals(0, list.size());
    }

    @Test
    public void testQueryCaseInsensitive() throws Exception {
        Map<String, Serializable> filter = new HashMap<String, Serializable>();
        // case insensitive substring search
        filter.put("username", "admini");
        DocumentModelList list = userDirSession.query(filter, filter.keySet());
        assertEquals(1, list.size());
        DocumentModel dm = list.get(0);
        assertEquals("Administrator", dm.getProperty(SCHEMA, "username"));
    }

    @Test
    public void testGetProjection() throws Exception {
        Map<String, Serializable> filter = new HashMap<String, Serializable>();
        filter.put("username", "user_1");
        List<String> list = userDirSession.getProjection(filter, "password");
        assertEquals(1, list.size());
        //XXX the password has been hash during restoring in session
        //Authenticate test should be enough
        //assertTrue(list.contains("pass_1"));
    }

    @Test
    public void testSearch() throws Exception {
        Map<String, Serializable> filter = new HashMap<String, Serializable>();

        // exact match
        filter.put("username", "u");
        List<String> users = userDirSession.getProjection(filter, "username");
        assertEquals(0, users.size());

        // substring match
        users = userDirSession.getProjection(filter, filter.keySet(),
                "username");
        assertEquals(2, users.size());
        assertTrue(users.contains("user_1"));

        filter.put("username", "v");
        users = userDirSession.getProjection(filter, filter.keySet(),
                "username");
        assertEquals(0, users.size());

        // trying to cheat
        filter.put("username", "*");
        users = userDirSession.getProjection(filter, "username");
        assertEquals(0, users.size());

        // substring with empty key
        filter.put("username", "");
        users = userDirSession.getProjection(filter, filter.keySet(),
                "username");
        assertEquals(3, users.size());
        assertTrue(users.contains("user_1"));
        assertTrue(users.contains("Administrator"));
    }

    @Test
    public void testIsAuthenticating() throws Exception {
        // by default the user directory is authenticating
        assertTrue(userDirSession.isAuthenticating());

        // by setting a password field that does not belong to the
        // user SCHEMA, we disable that feature
        ((SQLDirectory) userDir).getConfig().setPasswordField(
                "SomeStrangePassordField");
        Session session = userDir.getSession();
        assertFalse(session.isAuthenticating());
        session.close();
        ((SQLDirectory) userDir).getConfig().setPasswordField(
                "password");

    }

    @Test
    public void testAuthenticate() throws Exception {
        // successful authentication
        assertTrue(userDirSession.authenticate("Administrator", "Administrator"));
        assertTrue(userDirSession.authenticate("user_1", "pass_1"));

        // authentication against encrypted password
        assertTrue(userDirSession.authenticate("user_3", "pass_3"));

        // failed authentication: bad password
        assertFalse(userDirSession.authenticate("Administrator",
                "WrongPassword"));
        assertFalse(userDirSession.authenticate("user", ".asdf'23423"));

        // failed authentication: not existing user
        assertFalse(userDirSession.authenticate("NonExistingUser", "whatever"));
    }

    @Test
    public void testCreateFromModel() throws Exception {
        String schema = "user";
        DocumentModel entry = BaseSession.createEntryModel(null, schema, null,
                null);
        entry.setProperty("user", "username", "yo");

        assertNull(userDirSession.getEntry("yo"));
        userDirSession.createEntry(entry);
        assertNotNull(userDirSession.getEntry("yo"));

        // create one with existing same id, must fail
        entry.setProperty("user", "username", "Administrator");
        try {
            assertTrue(userDirSession.hasEntry("Administrator"));
            entry = userDirSession.createEntry(entry);
            userDirSession.getEntry("Administrator");
            fail("Should raise an error, entry already exists");
        } catch (DirectoryException e) {
        }
    }

    @Test
    public void testHasEntry() throws Exception {
        assertTrue(userDirSession.hasEntry("Administrator"));
        assertFalse(userDirSession.hasEntry("foo"));
    }

}
