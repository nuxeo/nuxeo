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
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
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

/**
 * This is slightly different from TestCoreDirectory because for vocabularies we have an id field and therefore need a
 * separate core schema without id for storage.
 */
@Features({ CoreFeature.class, ClientLoginFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.directory.api")
@Deploy("org.nuxeo.ecm.directory")
@Deploy("org.nuxeo.ecm.directory.core.tests:core/types-config.xml")
// subclasses need to provide the definition for the "myvoc" directory
public abstract class CoreDirectoryVocabTestCase {

    protected static final String DIR_NAME = "myvoc";

    protected static final String SCHEMA_NAME = "vocabulary";

    protected static final String ID = "id";

    protected static final String LABEL = "label";

    protected static final String ENTRY1 = "vocentry1";

    protected static final String ENTRY2 = "vocentry2";

    protected static final String LABEL1 = "Label 1";

    protected static final String LABEL2 = "Label 2";

    @Inject
    protected DirectoryService directoryService;

    protected Session session;

    @Before
    public void setUp() throws Exception {
        // be sure we don't retrieve a leaked security context
        Framework.login();
        Directory dir = directoryService.getDirectory(DIR_NAME);
        dir.initialize(); // to re-populate /directories each time
        session = dir.getSession();

        Map<String, Object> map = new HashMap<>();
        map.put(ID, ENTRY1);
        map.put(LABEL, LABEL1);
        session.createEntry(map);

        map.put(ID, ENTRY2);
        map.put(LABEL, LABEL2);
        session.createEntry(map);
    }

    @After
    public void tearDown() throws Exception {
        session.close();
    }

    @Test
    public void testGetEntry() throws Exception {
        DocumentModel entry;
        entry = session.getEntry(ENTRY1);
        assertNotNull(entry);
        assertEquals(ENTRY1, entry.getPropertyValue(ID));
        assertEquals(LABEL1, entry.getPropertyValue(LABEL));
        entry = session.getEntry("no-such-entry");
        assertNull(entry);
        entry = session.getEntry(ENTRY2);
        assertNotNull(entry);
    }

    @Test
    public void testCreateEntry() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put(ID, "vocentry3");
        map.put(LABEL, "Label 3");
        DocumentModel entry = session.createEntry(map);
        assertEquals("vocentry3", entry.getPropertyValue(ID));
        assertEquals("Label 3", entry.getPropertyValue(LABEL));
    }

    @Test
    public void testUpdateEntry() throws Exception {
        DocumentModel entry = session.getEntry(ENTRY1);
        entry.setPropertyValue(LABEL, "New Label 3");
        session.updateEntry(entry);
        entry = session.getEntry(ENTRY1);
        assertEquals("New Label 3", entry.getPropertyValue(LABEL));
    }

    @Test
    public void testDeleteEntry() throws Exception {
        session.deleteEntry("no-such-entry");
        assertNotNull(session.getEntry(ENTRY1));
        session.deleteEntry(ENTRY1);
        assertNull(session.getEntry(ENTRY1));
    }

    @Test
    public void testHasEntry() throws Exception {
        assertTrue(session.hasEntry(ENTRY1));
        assertFalse(session.hasEntry("bad-id"));
    }

    @Test
    public void testCreateFromModel() throws Exception {
        DocumentModel entry = BaseSession.createEntryModel(null, SCHEMA_NAME, null, null);
        String id = "vocentry4";
        entry.setPropertyValue(ID, id);

        assertNull(session.getEntry(id));
        DocumentModel newEntry = session.createEntry(entry);
        session.updateEntry(newEntry);
        assertNotNull(session.getEntry(id));

        // create one with existing same id, must fail
        entry.setPropertyValue(ID, ENTRY1);
        try {
            entry = session.createEntry(entry);
            fail("Should raise an exception, entry already exists");
        } catch (DirectoryException e) {
            assertEquals("Entry with id vocentry1 already exists", e.getMessage());
        }
    }

    @Test
    public void testQuery() throws Exception {
        DocumentModelList values;
        DocumentModel entry;

        values = session.query(Collections.emptyMap());
        assertEquals(2, values.size());

        values = session.query(Collections.singletonMap(ID, ENTRY1));
        assertEquals(1, values.size());
        entry = values.get(0);
        assertEquals(ENTRY1, entry.getPropertyValue(ID));
        assertEquals(LABEL1, entry.getPropertyValue(LABEL));

        values = session.query(Collections.singletonMap(LABEL, LABEL1));
        assertEquals(1, values.size());
        entry = values.get(0);
        assertEquals(ENTRY1, entry.getPropertyValue(ID));
        assertEquals(LABEL1, entry.getPropertyValue(LABEL));
    }

    @Test
    public void testQueryWithFilter() {
        Map<String, Serializable> usernamefilter = Collections.singletonMap(ID, ENTRY1);
        DocumentModelList users = session.query(usernamefilter);
        assertEquals(1, users.size());
    }

    @Test
    public void testProjection() throws Exception {
        List<String> values;

        values = session.getProjection(Collections.emptyMap(), ID);
        assertEquals(new HashSet<>(Arrays.asList(ENTRY1, ENTRY2)), new HashSet<>(values));
        values = session.getProjection(Collections.emptyMap(), LABEL);
        assertEquals(new HashSet<>(Arrays.asList(LABEL1, LABEL2)), new HashSet<>(values));

        values = session.getProjection(Collections.singletonMap(ID, ENTRY1), ID);
        assertEquals(Collections.singleton(ENTRY1), new HashSet<>(values));
        values = session.getProjection(Collections.singletonMap(ID, ENTRY1), LABEL);
        assertEquals(Collections.singleton(LABEL1), new HashSet<>(values));

        values = session.getProjection(Collections.singletonMap(LABEL, LABEL1), ID);
        assertEquals(Collections.singleton(ENTRY1), new HashSet<>(values));
        values = session.getProjection(Collections.singletonMap(LABEL, LABEL1), LABEL);
        assertEquals(Collections.singleton(LABEL1), new HashSet<>(values));
    }

}
