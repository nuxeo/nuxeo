/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 *
 */

package org.nuxeo.ecm.directory.multi;

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
import java.util.Set;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelComparator;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.DirectorySecurityException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.memory.MemoryDirectory;
import org.nuxeo.ecm.directory.memory.MemoryDirectoryDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @author Florent Guillaume
 */
@RunWith(FeaturesRunner.class)
@Features({ MultiDirectoryFeature.class })
@LocalDeploy("org.nuxeo.ecm.directory.multi.tests:directories-config.xml")
public class TestMultiDirectory {

    @Inject
    DirectoryService directoryService;

    MemoryDirectory memdir1;

    MemoryDirectory memdir2;

    MemoryDirectory memdir3;

    MultiDirectory multiDir;

    MultiDirectorySession dir;

    protected MemoryDirectoryDescriptor desc1;

    protected MemoryDirectoryDescriptor desc2;

    protected MemoryDirectoryDescriptor desc3;

    @Before
    public void setUp() throws Exception {
        // create and register mem directories
        Map<String, Object> e;

        // dir 1
        desc1 = new MemoryDirectoryDescriptor();
        desc1.name = "dir1";
        desc1.schemaName = "schema1";
        desc1.schemaSet = new HashSet<>(Arrays.asList("uid", "foo"));
        desc1.idField = "uid";
        desc1.passwordField = "foo";
        directoryService.registerDirectoryDescriptor(desc1);
        memdir1 = (MemoryDirectory) directoryService.getDirectory("dir1");

        try (Session dir1 = memdir1.getSession()) {
            e = new HashMap<>();
            e.put("uid", "1");
            e.put("foo", "foo1");
            dir1.createEntry(e);
            e = new HashMap<>();
            e.put("uid", "2");
            e.put("foo", "foo2");
            dir1.createEntry(e);
        }

        // dir 2
        desc2 = new MemoryDirectoryDescriptor();
        desc2.name = "dir2";
        desc2.schemaName = "schema2";
        desc2.schemaSet = new HashSet<>(Arrays.asList("id", "bar"));
        desc2.idField = "id";
        desc2.passwordField = null;
        directoryService.registerDirectoryDescriptor(desc2);
        memdir2 = (MemoryDirectory) directoryService.getDirectory("dir2");

        try (Session dir2 = memdir2.getSession()) {
            e = new HashMap<>();
            e.put("id", "1");
            e.put("bar", "bar1");
            dir2.createEntry(e);
            e = new HashMap<>();
            e.put("id", "2");
            e.put("bar", "bar2");
            dir2.createEntry(e);
        }

        // dir 3
        desc3 = new MemoryDirectoryDescriptor();
        desc3.name = "dir3";
        desc3.schemaName = "schema3";
        desc3.schemaSet = new HashSet<>(Arrays.asList("uid", "thefoo", "thebar"));
        desc3.idField = "uid";
        desc3.passwordField = "thefoo";
        directoryService.registerDirectoryDescriptor(desc3);
        memdir3 = (MemoryDirectory) directoryService.getDirectory("dir3");

        try (Session dir3 = memdir3.getSession()) {
            e = new HashMap<>();
            e.put("uid", "3");
            e.put("thefoo", "foo3");
            e.put("thebar", "bar3");
            dir3.createEntry(e);
            e = new HashMap<>();
            e.put("uid", "4");
            e.put("thefoo", "foo4");
            e.put("thebar", "bar4");
            dir3.createEntry(e);
        }

        // the multi directory
        multiDir = (MultiDirectory) directoryService.getDirectory("multi");
        dir = (MultiDirectorySession) multiDir.getSession();
    }

    @After
    public void tearDown() throws Exception {
        if (dir != null) {
            dir.close();
        }
        directoryService.unregisterDirectoryDescriptor(desc1);
        directoryService.unregisterDirectoryDescriptor(desc2);
        directoryService.unregisterDirectoryDescriptor(desc3);
    }

    @Test
    public void testGetEntry() throws Exception {
        DocumentModel entry;
        entry = dir.getEntry("1");
        assertEquals("1", entry.getProperty("schema3", "uid"));
        assertEquals("foo1", entry.getProperty("schema3", "thefoo"));
        assertEquals("bar1", entry.getProperty("schema3", "thebar"));
        entry = dir.getEntry("2");
        assertEquals("2", entry.getProperty("schema3", "uid"));
        assertEquals("foo2", entry.getProperty("schema3", "thefoo"));
        assertEquals("bar2", entry.getProperty("schema3", "thebar"));
        entry = dir.getEntry("3");
        assertEquals("3", entry.getProperty("schema3", "uid"));
        assertEquals("foo3", entry.getProperty("schema3", "thefoo"));
        assertEquals("bar3", entry.getProperty("schema3", "thebar"));
        entry = dir.getEntry("4");
        assertEquals("4", entry.getProperty("schema3", "uid"));
        assertEquals("foo4", entry.getProperty("schema3", "thefoo"));
        assertEquals("bar4", entry.getProperty("schema3", "thebar"));
        entry = dir.getEntry("no-such-entry");
        assertNull(entry);
    }

    @Test
    public void testGetEntries() throws Exception {
        DocumentModelList l;
        l = dir.getEntries();
        assertEquals(4, l.size());
        DocumentModel entry = null;
        for (DocumentModel e : l) {
            if (e.getId().equals("1")) {
                entry = e;
                break;
            }
        }
        assertNotNull(entry);
        assertEquals("foo1", entry.getProperty("schema3", "thefoo"));
    }

    @Test
    public void testCreate() throws Exception {
        try (Session dir1 = memdir1.getSession();
                Session dir2 = memdir2.getSession();
                Session dir3 = memdir3.getSession()) {

            // multi-subdir create
            Map<String, Object> map = new HashMap<>();
            map.put("uid", "5");
            map.put("thefoo", "foo5");
            map.put("thebar", "bar5");
            DocumentModel entry = dir.createEntry(map);
            assertEquals("5", entry.getProperty("schema3", "uid"));
            assertEquals("foo5", entry.getProperty("schema3", "thefoo"));
            assertEquals("bar5", entry.getProperty("schema3", "thebar"));
            boolean exceptionThrwon = false;
            try {
                assertNull(entry.getProperty("schema3", "xyz"));
            } catch (PropertyNotFoundException ce) {
                exceptionThrwon = true;
            }
            assertTrue(exceptionThrwon);

            // check underlying directories
            assertNotNull(dir1.getEntry("5"));
            assertEquals("foo5", dir1.getEntry("5").getProperty("schema1", "foo"));
            assertNotNull(dir2.getEntry("5"));
            assertEquals("bar5", dir2.getEntry("5").getProperty("schema2", "bar"));
            assertNull(dir3.getEntry("5"));

            // create another with colliding id
            map = new HashMap<>();
            map.put("uid", "5");
            try {
                entry = dir.createEntry(map);
                fail("Should raise an error, entry already exists");
            } catch (DirectoryException e) {
            }
        }
    }

    @Test
    public void testAuthenticate() throws Exception {
        // sub dirs
        try (Session dir1 = memdir1.getSession(); //
                Session dir3 = memdir3.getSession()) {

            assertTrue(dir1.authenticate("1", "foo1"));
            assertFalse(dir1.authenticate("1", "haha"));
            assertFalse(dir1.authenticate("3", "foo3"));
            assertFalse(dir3.authenticate("1", "foo1"));
            assertTrue(dir3.authenticate("3", "foo3"));
            assertFalse(dir3.authenticate("3", "haha"));
            // multi dir
            assertTrue(dir.authenticate("1", "foo1"));
            assertFalse(dir.authenticate("1", "haha"));
            assertTrue(dir.authenticate("3", "foo3"));
            assertFalse(dir.authenticate("3", "haha"));
        }
    }

    @Test
    public void testUpdateEntry() throws Exception {
        try (Session dir1 = memdir1.getSession();
                Session dir2 = memdir2.getSession();
                Session dir3 = memdir3.getSession()) {

            // multi-subdirs update
            DocumentModel e = dir.getEntry("1");
            assertEquals("foo1", e.getProperty("schema3", "thefoo"));
            assertEquals("bar1", e.getProperty("schema3", "thebar"));
            e.setProperty("schema3", "thefoo", "fffooo1");
            e.setProperty("schema3", "thebar", "babar1");
            dir.updateEntry(e);
            e = dir.getEntry("1");
            assertEquals("fffooo1", e.getProperty("schema3", "thefoo"));
            assertEquals("babar1", e.getProperty("schema3", "thebar"));

            // check underlying directories
            assertEquals("fffooo1", dir1.getEntry("1").getProperty("schema1", "foo"));
            assertEquals("babar1", dir2.getEntry("1").getProperty("schema2", "bar"));
            assertNull(dir3.getEntry("1"));

            // single subdir update
            e = dir.getEntry("3");
            assertEquals("foo3", e.getProperty("schema3", "thefoo"));
            assertEquals("bar3", e.getProperty("schema3", "thebar"));
            e.setProperty("schema3", "thefoo", "fffooo3");
            e.setProperty("schema3", "thebar", "babar3");
            dir.updateEntry(e);
            e = dir.getEntry("3");
            assertEquals("fffooo3", e.getProperty("schema3", "thefoo"));
            assertEquals("babar3", e.getProperty("schema3", "thebar"));

            // check underlying directories
            assertNull(dir1.getEntry("3"));
            assertNull(dir2.getEntry("3"));
            assertNotNull(dir3.getEntry("3"));
            assertEquals("fffooo3", dir3.getEntry("3").getProperty("schema3", "thefoo"));
            assertEquals("babar3", dir3.getEntry("3").getProperty("schema3", "thebar"));
        }
    }

    @Test
    public void testUpdateReadonlyMultidir() throws Exception {
        MultiDirectory readonlyMultidir = (MultiDirectory) directoryService.getDirectory("readonlymulti");
        try (MultiDirectorySession readonlyDir = (MultiDirectorySession) readonlyMultidir.getSession();
                Session dir1 = memdir1.getSession();
                Session dir2 = memdir2.getSession();
                Session dir3 = memdir3.getSession()) {

            // multi-subdirs update
            DocumentModel e = readonlyDir.getEntry("1");
            assertEquals("foo1", e.getProperty("schema3", "thefoo"));
            assertEquals("bar1", e.getProperty("schema3", "thebar"));
            e.setProperty("schema3", "thefoo", "fffooo1");
            e.setProperty("schema3", "thebar", "babar1");

            try {
                readonlyDir.updateEntry(e);
                fail("Should not be able to update entry");
            } catch (DirectorySecurityException ee) {
                // ok
            }
            e = readonlyDir.getEntry("1");
            // the multidirectory entry was not updated
            assertEquals("foo1", e.getProperty("schema3", "thefoo"));
            assertEquals("bar1", e.getProperty("schema3", "thebar"));

            // neither the underlying directories
            assertEquals("foo1", dir1.getEntry("1").getProperty("schema1", "foo"));
            assertEquals("bar1", dir2.getEntry("1").getProperty("schema2", "bar"));
            assertNull(dir3.getEntry("1"));
        }
    }

    @Test
    public void testUpdatePartialReadOnlyMultidir() throws Exception {
        try (Session dir1 = memdir1.getSession();
                Session dir2 = memdir2.getSession();
                Session dir3 = memdir3.getSession()) {

            memdir2.setReadOnly(true);
            memdir3.setReadOnly(true);

            // multi-subdirs update
            DocumentModel e = dir.getEntry("1");
            assertEquals("foo1", e.getProperty("schema3", "thefoo"));
            assertEquals("bar1", e.getProperty("schema3", "thebar"));
            e.setProperty("schema3", "thefoo", "fffooo1");
            e.setProperty("schema3", "thebar", "babar1");
            dir.updateEntry(e);
            e = dir.getEntry("1");
            assertEquals("fffooo1", e.getProperty("schema3", "thefoo"));
            assertEquals("bar1", e.getProperty("schema3", "thebar"));

            // check underlying directories
            // update not done on readonly dir2
            assertEquals("fffooo1", dir1.getEntry("1").getProperty("schema1", "foo"));
            assertEquals("bar1", dir2.getEntry("1").getProperty("schema2", "bar"));
            assertNull(dir3.getEntry("1"));

            // single subdir update
            e = dir.getEntry("3");
            assertEquals("foo3", e.getProperty("schema3", "thefoo"));
            assertEquals("bar3", e.getProperty("schema3", "thebar"));
            e.setProperty("schema3", "thefoo", "fffooo3");
            e.setProperty("schema3", "thebar", "babar3");
            dir.updateEntry(e);

            e = dir.getEntry("3");
            assertEquals("foo3", e.getProperty("schema3", "thefoo"));
            assertEquals("bar3", e.getProperty("schema3", "thebar"));

            // check underlying directories
            // check update is not effective on readonly subdir
            assertNull(dir1.getEntry("3"));
            assertNull(dir2.getEntry("3"));
            assertNotNull(dir3.getEntry("3"));
            assertEquals("foo3", dir3.getEntry("3").getProperty("schema3", "thefoo"));
            assertEquals("bar3", dir3.getEntry("3").getProperty("schema3", "thebar"));
        }
    }

    @Test
    public void testDeleteEntry() throws Exception {
        try (Session dir1 = memdir1.getSession();
                Session dir2 = memdir2.getSession();
                Session dir3 = memdir3.getSession()) {
            dir.deleteEntry("no-such-entry");
            assertEquals(4, dir.getEntries().size());
            assertEquals(2, dir1.getEntries().size());
            assertEquals(2, dir2.getEntries().size());
            assertEquals(2, dir3.getEntries().size());
            dir.deleteEntry("1");
            assertNull(dir.getEntry("1"));
            assertEquals(3, dir.getEntries().size());
            assertEquals(1, dir1.getEntries().size());
            assertEquals(1, dir2.getEntries().size());
            assertEquals(2, dir3.getEntries().size());
            dir.deleteEntry("3");
            assertNull(dir.getEntry("3"));
            assertEquals(2, dir.getEntries().size());
            assertEquals(1, dir1.getEntries().size());
            assertEquals(1, dir2.getEntries().size());
            assertEquals(1, dir3.getEntries().size());
        }
    }

    @Test
    public void testQuery() throws Exception {
        Map<String, Serializable> filter = new HashMap<>();
        DocumentModelList entries;
        DocumentModel e;

        // empty filter means everything (like getEntries)
        entries = dir.query(filter);
        assertNotNull(entries);
        assertEquals(4, entries.size());

        // no result
        filter.put("thefoo", "f");
        entries = dir.query(filter);
        assertEquals(0, entries.size());

        // query matching one source
        // source with two subdirs
        filter.put("thefoo", "foo1");
        entries = dir.query(filter);
        assertEquals(1, entries.size());
        e = entries.get(0);
        assertEquals("1", e.getId());
        assertEquals("bar1", e.getProperty("schema3", "thebar"));
        // simple source
        filter.put("thefoo", "foo3");
        entries = dir.query(filter);
        assertEquals(1, entries.size());
        e = entries.get(0);
        assertEquals("3", e.getId());
        assertEquals("bar3", e.getProperty("schema3", "thebar"));

        // query matching two subdirectories in one source
        filter.put("thefoo", "foo1");
        filter.put("thebar", "bar1");
        entries = dir.query(filter);
        assertEquals(1, entries.size());
        e = entries.get(0);
        assertEquals("1", e.getId());
        assertEquals("foo1", e.getProperty("schema3", "thefoo"));
        assertEquals("bar1", e.getProperty("schema3", "thebar"));

        // query not matching although each subdirectory in the source matches
        filter.put("thefoo", "foo1");
        filter.put("thebar", "bar2");
        entries = dir.query(filter);
        assertEquals(0, entries.size());

        // query matching two sources
        filter.clear();
        e = dir.getEntry("1");
        e.setProperty("schema3", "thefoo", "matchme");
        dir.updateEntry(e);
        e = dir.getEntry("3");
        e.setProperty("schema3", "thefoo", "matchme");
        dir.updateEntry(e);
        filter.put("thefoo", "matchme");
        entries = dir.query(filter);
        assertEquals(2, entries.size());
        e = entries.get(0);
        assertEquals("1", e.getId());
        assertEquals("bar1", e.getProperty("schema3", "thebar"));
        e = entries.get(1);
        assertEquals("3", e.getId());
        assertEquals("bar3", e.getProperty("schema3", "thebar"));
    }

    @Test
    public void testQueryWithRange() {
        // reference query with no ranges
        assertEquals(4, dir.query(Collections.emptyMap(), null).size());
        // queries with invalid/ignored ranges
        assertEquals(4, dir.query(Collections.emptyMap(), null, null, false, 0, 0).size());
        assertEquals(4, dir.query(Collections.emptyMap(), null, null, false, -1, -1).size());
        // 1 result
        assertEquals(1, dir.query(Collections.emptyMap(), null, null, false, 1, 0).size());
        assertEquals(1, dir.query(Collections.emptyMap(), null, null, false, 1, 1).size());
        assertEquals(1, dir.query(Collections.emptyMap(), null, null, false, 1, 2).size());
        assertEquals(1, dir.query(Collections.emptyMap(), null, null, false, 1, 3).size());
        // empty because offset == size
        assertEquals(0, dir.query(Collections.emptyMap(), null, null, false, 1, 4).size());
    }

    @Test
    public void testQueryFulltext() throws Exception {
        Map<String, Serializable> filter = new HashMap<>();
        Set<String> fulltext = new HashSet<>();
        DocumentModelList entries;
        entries = dir.query(filter, fulltext);
        assertEquals(4, entries.size());
        // null fulltext set should be equivalent to empty one
        entries = dir.query(filter, null);
        assertEquals(4, entries.size());
    }

    @Test
    public void testGetProjection() throws Exception {
        Map<String, Serializable> filter = new HashMap<>();
        List<String> list;

        // empty filter means everything (like getEntries)
        list = dir.getProjection(filter, "uid");
        Collections.sort(list);
        assertEquals(Arrays.asList("1", "2", "3", "4"), list);
        list = dir.getProjection(filter, "thefoo");
        Collections.sort(list);
        assertEquals(Arrays.asList("foo1", "foo2", "foo3", "foo4"), list);
        list = dir.getProjection(filter, "thebar");
        Collections.sort(list);
        assertEquals(Arrays.asList("bar1", "bar2", "bar3", "bar4"), list);

        // XXX test projection on unknown column

        // no result
        filter.put("thefoo", "f");
        list = dir.getProjection(filter, "uid");
        assertEquals(0, list.size());
        list = dir.getProjection(filter, "thefoo");
        assertEquals(0, list.size());
        list = dir.getProjection(filter, "thebar");
        assertEquals(0, list.size());

        // query matching one source
        // source with two subdirs
        filter.put("thefoo", "foo1");
        list = dir.getProjection(filter, "uid");
        assertEquals(Arrays.asList("1"), list);
        list = dir.getProjection(filter, "thefoo");
        assertEquals(Arrays.asList("foo1"), list);
        list = dir.getProjection(filter, "thebar");
        assertEquals(Arrays.asList("bar1"), list);
        // simple source
        filter.put("thefoo", "foo3");
        list = dir.getProjection(filter, "uid");
        assertEquals(Arrays.asList("3"), list);
        list = dir.getProjection(filter, "thefoo");
        assertEquals(Arrays.asList("foo3"), list);
        list = dir.getProjection(filter, "thebar");
        assertEquals(Arrays.asList("bar3"), list);

        // query matching two subdirectories in one source
        filter.put("thefoo", "foo1");
        filter.put("thebar", "bar1");
        list = dir.getProjection(filter, "uid");
        assertEquals(Arrays.asList("1"), list);
        list = dir.getProjection(filter, "thefoo");
        assertEquals(Arrays.asList("foo1"), list);
        list = dir.getProjection(filter, "thebar");
        assertEquals(Arrays.asList("bar1"), list);

        // query not matching although each subdirectory in the source matches
        filter.put("thefoo", "foo1");
        filter.put("thebar", "bar2");
        list = dir.getProjection(filter, "uid");
        assertEquals(0, list.size());
        list = dir.getProjection(filter, "thefoo");
        assertEquals(0, list.size());
        list = dir.getProjection(filter, "thebar");
        assertEquals(0, list.size());

        // query matching two sources
        DocumentModel e;
        filter.clear();
        e = dir.getEntry("1");
        e.setProperty("schema3", "thefoo", "matchme");
        dir.updateEntry(e);
        e = dir.getEntry("3");
        e.setProperty("schema3", "thefoo", "matchme");
        dir.updateEntry(e);
        filter.put("thefoo", "matchme");
        list = dir.getProjection(filter, "uid");
        Collections.sort(list);
        assertEquals(Arrays.asList("1", "3"), list);
        list = dir.getProjection(filter, "thefoo");
        assertEquals(Arrays.asList("matchme", "matchme"), list);
        list = dir.getProjection(filter, "thebar");
        Collections.sort(list);
        assertEquals(Arrays.asList("bar1", "bar3"), list);
    }

    @Test
    public void testCreateFromModel() throws Exception {
        String schema = "schema3";
        DocumentModel entry = BaseSession.createEntryModel(null, schema, null, null);
        entry.setProperty("schema3", "uid", "yo");

        assertNull(dir.getEntry("yo"));
        dir.createEntry(entry);
        assertNotNull(dir.getEntry("yo"));

        // create one with existing same id, must fail
        entry.setProperty("schema3", "uid", "1");
        try {
            entry = dir.createEntry(entry);
            fail("Should raise an error, entry already exists");
        } catch (DirectoryException e) {
        }
    }

    @Test
    public void testHasEntry() throws Exception {
        assertTrue(dir.hasEntry("1"));
        assertFalse(dir.hasEntry("foo"));
    }

    @Test
    public void testReadOnlyEntryFromMultidirectory() throws Exception {
        MultiDirectory readonlyMultidir = (MultiDirectory) directoryService.getDirectory("readonlymulti");
        try (MultiDirectorySession readonlyDir = (MultiDirectorySession) readonlyMultidir.getSession()) {
            // all should be readonly
            assertTrue(BaseSession.isReadOnlyEntry(readonlyDir.getEntry("1")));
            assertTrue(BaseSession.isReadOnlyEntry(readonlyDir.getEntry("2")));
            assertTrue(BaseSession.isReadOnlyEntry(readonlyDir.getEntry("3")));
            assertTrue(BaseSession.isReadOnlyEntry(readonlyDir.getEntry("4")));
        }
    }

    @Test
    public void testReadOnlyEntryFromGetEntry() throws Exception {

        // by default no backing dir is readonly
        assertFalse(BaseSession.isReadOnlyEntry(dir.getEntry("1")));
        assertFalse(BaseSession.isReadOnlyEntry(dir.getEntry("2")));
        assertFalse(BaseSession.isReadOnlyEntry(dir.getEntry("3")));
        assertFalse(BaseSession.isReadOnlyEntry(dir.getEntry("4")));

        memdir1.setReadOnly(true);
        memdir2.setReadOnly(false);
        memdir3.setReadOnly(false);
        assertFalse(BaseSession.isReadOnlyEntry(dir.getEntry("1")));
        assertFalse(BaseSession.isReadOnlyEntry(dir.getEntry("2")));
        assertFalse(BaseSession.isReadOnlyEntry(dir.getEntry("3")));
        assertFalse(BaseSession.isReadOnlyEntry(dir.getEntry("4")));

        memdir1.setReadOnly(false);
        memdir2.setReadOnly(true);
        memdir3.setReadOnly(true);
        assertFalse(BaseSession.isReadOnlyEntry(dir.getEntry("1")));
        assertFalse(BaseSession.isReadOnlyEntry(dir.getEntry("2")));
        assertTrue(BaseSession.isReadOnlyEntry(dir.getEntry("3")));
        assertTrue(BaseSession.isReadOnlyEntry(dir.getEntry("4")));

        memdir1.setReadOnly(false);
        memdir2.setReadOnly(false);
        memdir3.setReadOnly(true);
        assertFalse(BaseSession.isReadOnlyEntry(dir.getEntry("1")));
        assertFalse(BaseSession.isReadOnlyEntry(dir.getEntry("2")));
        assertTrue(BaseSession.isReadOnlyEntry(dir.getEntry("3")));
        assertTrue(BaseSession.isReadOnlyEntry(dir.getEntry("4")));

        memdir1.setReadOnly(true);
        memdir2.setReadOnly(true);
        memdir3.setReadOnly(false);
        assertTrue(BaseSession.isReadOnlyEntry(dir.getEntry("1")));
        assertTrue(BaseSession.isReadOnlyEntry(dir.getEntry("2")));
        assertFalse(BaseSession.isReadOnlyEntry(dir.getEntry("3")));
        assertFalse(BaseSession.isReadOnlyEntry(dir.getEntry("4")));
    }

    @Test
    public void testReadOnlyEntryInQueryResults() throws Exception {
        Map<String, String> orderBy = new HashMap<>();
        orderBy.put("schema3:uid", "asc");
        DocumentModelComparator comp = new DocumentModelComparator(orderBy);

        Map<String, Serializable> filter = new HashMap<>();
        DocumentModelList results = dir.query(filter);
        Collections.sort(results, comp);

        // by default no backing dir is readonly
        assertFalse(BaseSession.isReadOnlyEntry(results.get(0)));
        assertFalse(BaseSession.isReadOnlyEntry(results.get(1)));
        assertFalse(BaseSession.isReadOnlyEntry(results.get(2)));
        assertFalse(BaseSession.isReadOnlyEntry(results.get(3)));

        memdir1.setReadOnly(true);
        memdir2.setReadOnly(false);
        memdir3.setReadOnly(false);
        results = dir.query(filter);
        Collections.sort(results, comp);
        assertTrue(BaseSession.isReadOnlyEntry(results.get(0)));
        assertTrue(BaseSession.isReadOnlyEntry(results.get(1)));
        assertFalse(BaseSession.isReadOnlyEntry(results.get(2)));
        assertFalse(BaseSession.isReadOnlyEntry(results.get(3)));

        memdir1.setReadOnly(false);
        memdir2.setReadOnly(false);
        memdir3.setReadOnly(true);
        results = dir.query(filter);
        Collections.sort(results, comp);
        assertFalse(BaseSession.isReadOnlyEntry(results.get(0)));
        assertFalse(BaseSession.isReadOnlyEntry(results.get(1)));
        assertTrue(BaseSession.isReadOnlyEntry(results.get(2)));
        assertTrue(BaseSession.isReadOnlyEntry(results.get(3)));
    }

    @Test
    public void testReadOnlyEntryInGetEntriesResults() throws Exception {
        Map<String, String> orderBy = new HashMap<>();
        orderBy.put("schema3:uid", "asc");
        DocumentModelComparator comp = new DocumentModelComparator(orderBy);

        DocumentModelList results = dir.getEntries();
        Collections.sort(results, comp);

        // by default no backing dir is readonly
        assertFalse(BaseSession.isReadOnlyEntry(results.get(0)));
        assertFalse(BaseSession.isReadOnlyEntry(results.get(1)));
        assertFalse(BaseSession.isReadOnlyEntry(results.get(2)));
        assertFalse(BaseSession.isReadOnlyEntry(results.get(3)));

        memdir1.setReadOnly(true);
        memdir2.setReadOnly(false);
        memdir3.setReadOnly(false);
        results = dir.getEntries();
        Collections.sort(results, comp);
        assertTrue(BaseSession.isReadOnlyEntry(results.get(0)));
        assertTrue(BaseSession.isReadOnlyEntry(results.get(1)));
        assertFalse(BaseSession.isReadOnlyEntry(results.get(2)));
        assertFalse(BaseSession.isReadOnlyEntry(results.get(3)));

        memdir1.setReadOnly(false);
        memdir2.setReadOnly(false);
        memdir3.setReadOnly(true);
        results = dir.getEntries();
        Collections.sort(results, comp);
        assertFalse(BaseSession.isReadOnlyEntry(results.get(0)));
        assertFalse(BaseSession.isReadOnlyEntry(results.get(1)));
        assertTrue(BaseSession.isReadOnlyEntry(results.get(2)));
        assertTrue(BaseSession.isReadOnlyEntry(results.get(3)));
    }

}
