/*
 * (C) Copyright 2014-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Maxime Hilaire
 *     Florent Guillaume
 */
package org.nuxeo.ecm.directory.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.directory.core.CoreDirectory.DEFAULT_DIRECTORIES_PATH;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, ClientLoginFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.directory.api")
@Deploy("org.nuxeo.ecm.directory")
@Deploy("org.nuxeo.ecm.directory.core.tests:core/types-config.xml")
@Deploy("org.nuxeo.ecm.directory.core.tests:core/directory-user-config.xml")
public class TestCoreDirectory {

    public static final String SCHEMA_NAME = "schema1";

    public static final String USER1 = "user1";

    public static final String USER2 = "user2";

    public static final String PWD1 = "pwd1";

    public static final String PWD2 = "pwd2";

    public static final String PWD2_HASHED = "{SSHA}FPVrD4t6mLB13LxX3V30odJ+ZnUL8PMbkRn/wQ==";

    public static final String FOO1 = "foo1";

    public static final String FOO2 = "foo2";

    public static final String BAR1 = "bar1";

    public static final String BAR2 = "bar2";

    public static final String DIR_NAME = "userCoreDirectory";

    public static final String DIR_PATH = DEFAULT_DIRECTORIES_PATH + '/' + DIR_NAME;

    public final static String UID = "sch1:uid";

    public final static String PWD = "sch1:password";

    public final static String FOO = "sch1:foo";

    public final static String BAR = "sch1:bar";

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected DirectoryService directoryService;

    @Inject
    protected CoreSession coreSession;

    protected Session session;

    @Before
    public void setUp() throws Exception {
        // be sure we don't retrieve a leaked security context
        Framework.login();
        Directory dir = directoryService.getDirectory(DIR_NAME);
        dir.initialize(); // to re-populate /directories each time
        session = dir.getSession();
        populate();
    }

    @After
    public void tearDown() throws Exception {
        session.close();
    }

    public void populate() {
        // create user1 entry
        DocumentModel user1 = createDocument(coreSession, DIR_PATH, USER1, "CoreDirDoc");
        user1.setPropertyValue(UID, USER1);
        user1.setPropertyValue(PWD, PWD1);
        user1.setPropertyValue(FOO, FOO1);
        user1.setPropertyValue(BAR, BAR1);
        coreSession.saveDocument(user1);

        // create user2 entry
        DocumentModel user2 = createDocument(coreSession, DIR_PATH, USER2, "CoreDirDoc");
        user2.setPropertyValue(UID, USER2);
        user2.setPropertyValue(PWD, PWD2_HASHED);
        user2.setPropertyValue(FOO, FOO2);
        user2.setPropertyValue(BAR, BAR2);
        coreSession.saveDocument(user2);

        coreSession.save();
    }

    @Test
    public void testIdEscaping() {
        assertEquals("foo", CoreDirectorySession.idToName("foo"));
        assertEquals("foo=2fbar", CoreDirectorySession.idToName("foo/bar"));
        assertEquals("foo=3dbar", CoreDirectorySession.idToName("foo=bar"));
        assertEquals("foo=2fbar=3dbaz", CoreDirectorySession.idToName("foo/bar=baz"));
        assertEquals("foo=3d", CoreDirectorySession.idToName("foo="));
        for (String name : Arrays.asList("foo", "foo/bar", "foo=bar", "foo/bar=baz", "foo=")) {
            assertEquals(name, CoreDirectorySession.nameToId(CoreDirectorySession.idToName(name)));
        }
        for (String name : Arrays.asList("=", "=2", "=2X", "=XYZ", "a=", "a=2", "a=X", "a=2X")) {
            try {
                CoreDirectorySession.nameToId(name);
                fail("Name should fail unescaping: " + name);
            } catch (NuxeoException e) {
                assertTrue(e.getMessage(), e.getMessage().startsWith("Illegal name, bad encoding"));
            }
        }
    }

    protected DocumentModel createDocument(CoreSession session, String parentPath, String docName, String docType) {
        DocumentModel doc = session.createDocumentModel(parentPath, docName, docType);
        return session.createDocument(doc);
    }
    @Test
    public void testGetEntry() throws Exception {
        DocumentModel entry;
        entry = session.getEntry(USER1);
        assertNotNull(entry);
        assertEquals(FOO1, entry.getPropertyValue(FOO));
        entry = session.getEntry("no-such-entry");
        assertNull(entry);
        entry = session.getEntry(USER2);
        assertNotNull(entry);
    }

    @Test
    public void testCreateEntry() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put(UID, "2");
        map.put(FOO, "foo3");
        map.put(BAR, "bar3");
        DocumentModel entry = session.createEntry(map);
        assertEquals("bar3", entry.getPropertyValue(BAR));
    }

    @Test
    public void testUpdateEntry() throws Exception {
        DocumentModel entry = session.getEntry(USER1);
        Map<String, Object> map = new HashMap<>();
        map.put(UID, USER1);
        map.put(FOO, "foo3");
        map.put(BAR, "bar3");
        entry.setProperties(SCHEMA_NAME, map);

        session.updateEntry(entry);

        entry = session.getEntry(USER1);
        assertEquals("foo3", entry.getPropertyValue(FOO));
    }

    @Test
    public void testAuthenticate() throws Exception {
        assertTrue(session.authenticate(USER1, PWD1));
        assertFalse(session.authenticate(USER1, "bad-pwd"));
        assertFalse(session.authenticate("bad-id", "haha"));
        assertTrue(session.authenticate(USER2, PWD2));
        assertFalse(session.authenticate(USER2, "bad-pwd"));
        // can't authenticate with the hash itself
        assertFalse(session.authenticate(USER2, PWD2_HASHED));
        // null password (avoid NPE)
        assertFalse(session.authenticate(USER2, null));
    }

    @Test
    public void testDeleteEntry() throws Exception {
        session.deleteEntry("no-such-entry");

        assertNotNull(session.getEntry(USER1));
        session.deleteEntry(USER1);
        assertNull(session.getEntry(USER1));
    }

    @Test
    public void testHasEntry() throws Exception {
        assertTrue(session.hasEntry(USER1));
        assertFalse(session.hasEntry("bad-id"));
    }

    @Test
    public void testCreateFromModel() throws Exception {
        DocumentModel entry = BaseSession.createEntryModel(null, SCHEMA_NAME, null, null);
        String id = "newId";
        entry.setPropertyValue(UID, id);

        assertNull(session.getEntry(id));
        DocumentModel newEntry = session.createEntry(entry);
        session.updateEntry(newEntry);
        assertNotNull(session.getEntry(id));

        // create one with existing same id, must fail
        entry.setPropertyValue(UID, USER1);
        try {
            entry = session.createEntry(entry);
            fail("Should raise an exception, entry already exists");
        } catch (DirectoryException e) {
            assertEquals("Entry with id user1 already exists", e.getMessage());
        }
    }

    @Test
    public void testQuery() throws Exception {
        DocumentModelList values;
        DocumentModel entry;

        values = session.query(Collections.emptyMap());
        assertEquals(2, values.size());

        values = session.query(Collections.singletonMap(UID, USER1));
        assertEquals(1, values.size());
        entry = values.get(0);
        assertEquals(USER1, entry.getPropertyValue(UID));
        assertEquals(FOO1, entry.getPropertyValue(FOO));
        assertEquals(BAR1, entry.getPropertyValue(BAR));

        values = session.query(Collections.singletonMap(FOO, FOO1));
        assertEquals(1, values.size());
        entry = values.get(0);
        assertEquals(USER1, entry.getPropertyValue(UID));
        assertEquals(FOO1, entry.getPropertyValue(FOO));
        assertEquals(BAR1, entry.getPropertyValue(BAR));
    }

    @Test
    public void testQueryWithFilter() {
        Map<String, Serializable> usernamefilter = Collections.singletonMap(UID, USER1);
        DocumentModelList users = session.query(usernamefilter);
        assertEquals(1, users.size());
    }

    @Test
    public void testProjection() throws Exception {
        List<String> values;

        values = session.getProjection(Collections.emptyMap(), UID);
        assertEquals(new HashSet<>(Arrays.asList(USER1, USER2)), new HashSet<>(values));
        values = session.getProjection(Collections.emptyMap(), FOO);
        assertEquals(new HashSet<>(Arrays.asList(FOO1, FOO2)), new HashSet<>(values));
        values = session.getProjection(Collections.emptyMap(), BAR);
        assertEquals(new HashSet<>(Arrays.asList(BAR1, BAR2)), new HashSet<>(values));

        values = session.getProjection(Collections.singletonMap(UID, USER1), UID);
        assertEquals(Collections.singleton(USER1), new HashSet<>(values));
        values = session.getProjection(Collections.singletonMap(UID, USER1), FOO);
        assertEquals(Collections.singleton(FOO1), new HashSet<>(values));
        values = session.getProjection(Collections.singletonMap(UID, USER1), BAR);
        assertEquals(Collections.singleton(BAR1), new HashSet<>(values));

        values = session.getProjection(Collections.singletonMap(FOO, FOO1), UID);
        assertEquals(Collections.singleton(USER1), new HashSet<>(values));
        values = session.getProjection(Collections.singletonMap(FOO, FOO1), FOO);
        assertEquals(Collections.singleton(FOO1), new HashSet<>(values));
        values = session.getProjection(Collections.singletonMap(FOO, FOO1), BAR);
        assertEquals(Collections.singleton(BAR1), new HashSet<>(values));

        // filter on password is ignored
        values = session.getProjection(Collections.singletonMap(PWD, PWD1), UID);
        assertEquals(new HashSet<>(Arrays.asList(USER1, USER2)), new HashSet<>(values));
    }

}
