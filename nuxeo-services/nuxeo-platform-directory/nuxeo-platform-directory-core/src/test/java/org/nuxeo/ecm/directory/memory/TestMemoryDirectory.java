/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.directory.memory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.local.WithUser;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.model.OrderByExprs;
import org.nuxeo.ecm.core.query.sql.model.Predicates;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author Florent Guillaume
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.core.schema")
@Deploy("org.nuxeo.ecm.core.cache")
@Deploy("org.nuxeo.ecm.directory")
@Deploy("org.nuxeo.ecm.directory.core.tests:test-schema.xml")
@WithUser("Administrator")
public class TestMemoryDirectory {

    protected MemoryDirectory memDir;

    protected MemoryDirectorySession dir;

    protected DocumentModel entry;

    static final String SCHEMA_NAME = "myschema";

    @Before
    public void before() {

        MemoryDirectoryDescriptor descr = new MemoryDirectoryDescriptor();
        descr.name = "mydir";
        descr.schemaName = SCHEMA_NAME;
        descr.idField = "i";
        descr.passwordField = "pw";
        descr.schemaSet = new HashSet<>(Arrays.asList("i", "pw", "a", "int", "b"));
        memDir = new MemoryDirectory(descr);
        dir = memDir.getSession();
        Map<String, Object> e1 = new HashMap<>();
        e1.put("i", "1");
        e1.put("pw", "secr");
        e1.put("a", "AAA");
        e1.put("b", "BCD");
        e1.put("int", 3);
        e1.put("x", "XYZ"); // shouldn't be put in storage
        entry = dir.createEntry(e1);
    }

    @Test
    public void testSchemaIntrospection() {
        MemoryDirectoryDescriptor descr = new MemoryDirectoryDescriptor();
        descr.name = "adir";
        descr.schemaName = SCHEMA_NAME;
        descr.idField = "i";
        descr.passwordField = "pw";
        MemoryDirectory md = new MemoryDirectory(descr);
        assertEquals(new HashSet<>(Arrays.asList("i", "pw", "a", "int", "b", "x")), md.schemaSet);
    }

    @Test
    public void testCreate() {
        // created in setUp
        assertEquals("1", entry.getProperty(SCHEMA_NAME, "i"));
        assertNull(entry.getProperty(SCHEMA_NAME, "pw"));
        assertEquals("AAA", entry.getProperty(SCHEMA_NAME, "a"));
        assertEquals("BCD", entry.getProperty(SCHEMA_NAME, "b"));
        assertNull(entry.getProperty(SCHEMA_NAME, "x"));

        // create one with the same id, must fail
        Map<String, Object> e2 = new HashMap<>();
        e2.put("i", "1");
        try {
            entry = dir.createEntry(e2);
            fail("Should raise an error, entry already exists");
        } catch (DirectoryException e) {
            assertEquals("Entry with id 1 already exists in directory mydir", e.getMessage());
        }
    }

    @Test
    public void testCreateFromModel() {
        entry = BaseSession.createEntryModel(null, SCHEMA_NAME, null, null);
        entry.setProperty(SCHEMA_NAME, "i", "yo");

        assertNull(dir.getEntry("yo"));
        dir.createEntry(entry);
        assertNotNull(dir.getEntry("yo"));

        // create one with existing same id, must fail
        entry.setProperty(SCHEMA_NAME, "i", "1");
        try {
            entry = dir.createEntry(entry);
            fail("Should raise an error, entry already exists");
        } catch (DirectoryException e) {
            assertEquals("Entry with id 1 already exists in directory mydir", e.getMessage());
        }
    }

    @Test
    public void testHasEntry() {
        assertTrue(dir.hasEntry("1"));
        assertFalse(dir.hasEntry("foo"));
    }

    @Test
    public void testAuthenticate() {
        assertTrue(dir.authenticate("1", "secr"));
        assertFalse(dir.authenticate("1", "haha"));
        assertFalse(dir.authenticate("2", "any"));
    }

    @Test
    public void testGetEntry() {
        DocumentModel entry = dir.getEntry("1");
        assertEquals("AAA", entry.getProperty(SCHEMA_NAME, "a"));
        assertNull(dir.getEntry("no-such-entry"));
    }

    @Test
    public void testGetEntries() {
        Map<String, Object> e2 = new HashMap<>();
        e2.put("i", "2");
        entry = dir.createEntry(e2);
        DocumentModelList l = dir.getEntries();
        assertEquals(2, l.size());
        assertEquals("1", l.get(0).getId());
        assertEquals("2", l.get(1).getId());
    }

    @Test
    public void testUpdateEntry() {
        DocumentModel e = dir.getEntry("1");
        assertEquals("BCD", e.getProperty(SCHEMA_NAME, "b"));
        e.setProperty(SCHEMA_NAME, "b", "babar");
        dir.updateEntry(e);
        e = dir.getEntry("1");
        assertEquals("babar", e.getProperty(SCHEMA_NAME, "b"));

        String id = "no-such-entry";
        Map<String, Object> map = new HashMap<>();
        map.put("i", id);
        DocumentModel entry = BaseSession.createEntryModel(null, SCHEMA_NAME, id, map);
        try {
            dir.updateEntry(entry);
        } catch (DirectoryException de) {
            assertEquals("UpdateEntry failed: entry 'no-such-entry' not found", de.getMessage());
        }
    }

    @Test
    public void testDeleteEntry() {
        DocumentModelList l = dir.getEntries();
        assertEquals(1, l.size());
        dir.deleteEntry("1");
        l = dir.getEntries();
        assertEquals(0, l.size());
    }

    @Test
    public void testQuery() {
        Map<String, Object> e2 = new HashMap<>();
        e2.put("i", "2");
        e2.put("pw", "guess");
        e2.put("a", "AAA222");
        e2.put("b", "BCD");
        dir.createEntry(e2);

        Map<String, Serializable> filter = new HashMap<>();
        DocumentModelList entries;
        DocumentModel e;

        // empty filter means everything (like getEntries)
        entries = dir.query(filter);
        assertNotNull(entries);
        assertEquals(2, entries.size());
        // filter with no known field is same as empty
        filter.put("bobo", "bibi");
        entries = dir.query(filter);
        assertNotNull(entries);
        assertEquals(2, entries.size());

        // no result
        filter.clear();
        filter.put("a", "gaga");
        entries = dir.query(filter);
        assertEquals(0, entries.size());

        // no fulltext
        filter.put("a", "A");
        entries = dir.query(filter);
        assertEquals(0, entries.size());

        // simple query
        filter.put("a", "AAA");
        entries = dir.query(filter);
        assertEquals(1, entries.size());

        e = entries.get(0);
        assertEquals("1", e.getId());
        assertEquals("BCD", e.getProperty(SCHEMA_NAME, "b"));

        // add unknown field
        filter.put("bobo", "bibi");
        entries = dir.query(filter);
        assertEquals(1, entries.size());
        assertEquals("1", entries.get(0).getId());

        // two criteria
        filter.clear();
        filter.put("a", "AAA");
        filter.put("b", "BCD");
        entries = dir.query(filter);
        assertEquals(1, entries.size());

        e = entries.get(0);
        assertEquals("1", e.getId());
        assertNull(e.getProperty(SCHEMA_NAME, "pw"));

        // query not matching although each criterion matches one entry
        filter.put("a", "AAA");
        filter.put("pw", "guess");
        entries = dir.query(filter);
        assertEquals(0, entries.size());
    }

    @Test
    public void testQueryFts() {
        Map<String, Serializable> filter = new HashMap<>();
        Set<String> fulltext = new HashSet<>();

        // trying to cheat
        filter.put("a", "*");
        assertEquals(0, dir.query(filter, fulltext).size());

        // fulltext
        filter.clear();
        fulltext.add("b");
        // only initial match
        filter.put("b", "c");
        assertEquals(0, dir.query(filter, fulltext).size());
        // lowercase initial match
        filter.put("b", "b");
        assertEquals(1, dir.query(filter, fulltext).size());

        // 2nd criterion not matching
        filter.put("a", "a");
        assertEquals(0, dir.query(filter, fulltext).size());

        // 2nd criterion matching
        filter.put("a", "AAA");
        assertEquals(1, dir.query(filter, fulltext).size());

        // 2nd criterion matching as fulltext
        filter.put("a", "a");
        fulltext.add("a");
        assertEquals(1, dir.query(filter, fulltext).size());

        // empty filter marked fulltext should match all
        filter.clear();
        fulltext.clear();
        filter.put("a", "");
        fulltext.add("a");
        assertEquals(1, dir.query(filter, fulltext).size());
    }

    @Test
    public void testQueryWithBuilder() {
        Map<String, Object> map;
        map = new HashMap<>();
        map.put("i", "2");
        dir.createEntry(map);
        map = new HashMap<>();
        map.put("i", "3");
        dir.createEntry(map);

        try (Session session = memDir.getSession()) {
            // everything (empty predicates)
            QueryBuilder queryBuilder = new QueryBuilder();
            checkQueryResult(session, queryBuilder, "1", "2", "3");

            // i = '1'
            queryBuilder = new QueryBuilder().predicate(Predicates.eq("i", "1"));
            checkQueryResult(session, queryBuilder, "1");

            // i = '1' OR i = '2'
            queryBuilder = new QueryBuilder().predicate(Predicates.eq("i", "1")).or(Predicates.eq("i", "2"));
            checkQueryResult(session, queryBuilder, "1", "2");

            // i = '1' AND a = 'NotMe'
            queryBuilder = new QueryBuilder().predicate(Predicates.eq("i", "1")).and(Predicates.eq("a", "NotMe"));
            checkQueryResult(session, queryBuilder); // empty

            // order/paging/totalSize

            // no count total
            queryBuilder = new QueryBuilder().order(OrderByExprs.asc("i")).limit(1);
            checkQueryResult(session, queryBuilder, "1");

            // count total
            queryBuilder = new QueryBuilder().order(OrderByExprs.desc("i")).limit(1).countTotal(true);
            checkQueryResult(session, queryBuilder, 3, "3");

            // offset
            queryBuilder = new QueryBuilder().order(OrderByExprs.desc("i")).limit(1).offset(1).countTotal(true);
            checkQueryResult(session, queryBuilder, 3, "2");

            // error cases

            // cannot filter on password
            queryBuilder = new QueryBuilder().predicate(Predicates.eq("pw", "secreet"));
            try {
                session.query(queryBuilder, false);
                fail("should throw");
            } catch (DirectoryException e) {
                assertEquals("Cannot filter on password", e.getMessage());
            }
            try {
                session.queryIds(queryBuilder);
                fail("should throw");
            } catch (DirectoryException e) {
                assertEquals("Cannot filter on password", e.getMessage());
            }

            // no such column
            queryBuilder = new QueryBuilder().predicate(Predicates.eq("notAProperty", "foo"));
            try {
                session.query(queryBuilder, false);
                fail("should throw");
            } catch (QueryParseException e) {
                assertEquals("No column: notAProperty for directory: mydir", e.getMessage());
            }
            try {
                session.queryIds(queryBuilder);
                fail("should throw");
            } catch (QueryParseException e) {
                assertEquals("No column: notAProperty for directory: mydir", e.getMessage());
            }
        }
    }

    protected static void checkQueryResult(Session session, QueryBuilder queryBuilder, String... expected) {
        checkQueryResult(session, queryBuilder, -99, expected);
    }

    protected static void checkQueryResult(Session session, QueryBuilder queryBuilder, int expectedTotalSize,
            String... expected) {
        DocumentModelList list = session.query(queryBuilder, false);
        List<String> ids = session.queryIds(queryBuilder);
        assertIds(list, ids, expected);
        if (queryBuilder.countTotal()) {
            assertEquals(expectedTotalSize, list.totalSize());
        }
    }

    protected static void assertIds(DocumentModelList list, List<String> ids, String... expected) {
        Set<String> expectedIds = new HashSet<>(Arrays.asList(expected));
        assertEquals(expectedIds, new HashSet<>(ids));
        Set<String> fromList = list.stream()
                                   .map(doc -> (String) doc.getProperty(SCHEMA_NAME, "i"))
                                   .collect(Collectors.toSet());
        assertEquals(expectedIds, fromList);
    }

    @Test
    public void testGetProjection() {
        List<String> list;
        Map<String, Object> e2 = new HashMap<>();
        e2.put("i", "2");
        e2.put("pw", "guess");
        e2.put("a", "AAA222");
        e2.put("b", "BCD");
        dir.createEntry(e2);

        // empty filter
        list = dir.getProjection(Map.of(), "a");
        assertEquals(2, list.size());

        // XXX test projection on unknown column

        // simple query
        list = dir.getProjection(Map.of("a", "AAA"), "b");
        assertEquals(1, list.size());
        assertEquals("BCD", list.get(0));

        // add unknown field
        list = dir.getProjection(Map.of("a", "AAA", "bobo", "bibi"), "a");
        assertEquals(1, list.size());
        assertEquals("AAA", list.get(0));

        // two criteria
        list = dir.getProjection(Map.of("a", "AAA", "b", "BCD"), "a");
        assertEquals(1, list.size());
        assertEquals("AAA", list.get(0));

        // query not matching although each criterion matches one entry
        list = dir.getProjection(Map.of("a", "AAA", "b", "BCD", "pw", "guess"), "a");
        assertEquals(0, list.size());
    }

    protected static List<String> entryIds(List<DocumentModel> entries) {
        List<String> ids = new ArrayList<>(entries.size());
        for (DocumentModel entry : entries) {
            ids.add(entry.getId());
        }
        return ids;
    }

    // actually tests AbstractDirectory.orderEntry
    @Test
    public void testOrderBy() {
        Map<String, Object> e2 = new HashMap<>();
        e2.put("i", "2");
        e2.put("pw", "guess");
        e2.put("a", "ZZZ");
        e2.put("b", "AAA");
        dir.createEntry(e2);

        Map<String, Serializable> filter = Collections.emptyMap();
        Set<String> fulltext = Collections.emptySet();
        Map<String, String> orderBy = new LinkedHashMap<>();

        // our data:
        // 1 -> AAA, BCD
        // 2 -> ZZZ, AAA

        // a

        orderBy.clear();
        orderBy.put("a", "asc");
        DocumentModelList entries = dir.query(filter, fulltext, orderBy);
        assertEquals(Arrays.asList("1", "2"), entryIds(entries));

        orderBy.put("a", "desc");
        entries = dir.query(filter, fulltext, orderBy);
        assertEquals(Arrays.asList("2", "1"), entryIds(entries));

        // b

        orderBy.clear();
        orderBy.put("b", "asc");
        entries = dir.query(filter, fulltext, orderBy);
        assertEquals(Arrays.asList("2", "1"), entryIds(entries));

        orderBy.put("b", "desc");
        entries = dir.query(filter, fulltext, orderBy);
        assertEquals(Arrays.asList("1", "2"), entryIds(entries));

        // a then b

        orderBy.clear();
        orderBy.put("a", "asc");
        orderBy.put("b", "asc");
        entries = dir.query(filter, fulltext, orderBy);
        assertEquals(Arrays.asList("1", "2"), entryIds(entries));

        orderBy.clear();
        orderBy.put("a", "asc");
        orderBy.put("b", "desc");
        entries = dir.query(filter, fulltext, orderBy);
        assertEquals(Arrays.asList("1", "2"), entryIds(entries));

        orderBy.clear();
        orderBy.put("a", "desc");
        orderBy.put("b", "asc");
        entries = dir.query(filter, fulltext, orderBy);
        assertEquals(Arrays.asList("2", "1"), entryIds(entries));

        orderBy.clear();
        orderBy.put("a", "desc");
        orderBy.put("b", "desc");
        entries = dir.query(filter, fulltext, orderBy);
        assertEquals(Arrays.asList("2", "1"), entryIds(entries));

        // b then a

        orderBy.clear();
        orderBy.put("b", "asc");
        orderBy.put("a", "asc");
        entries = dir.query(filter, fulltext, orderBy);
        assertEquals(Arrays.asList("2", "1"), entryIds(entries));

        orderBy.clear();
        orderBy.put("b", "asc");
        orderBy.put("a", "desc");
        entries = dir.query(filter, fulltext, orderBy);
        assertEquals(Arrays.asList("2", "1"), entryIds(entries));

        orderBy.clear();
        orderBy.put("b", "desc");
        orderBy.put("a", "asc");
        entries = dir.query(filter, fulltext, orderBy);
        assertEquals(Arrays.asList("1", "2"), entryIds(entries));

        orderBy.clear();
        orderBy.put("b", "desc");
        orderBy.put("a", "desc");
        entries = dir.query(filter, fulltext, orderBy);
        assertEquals(Arrays.asList("1", "2"), entryIds(entries));

        // with an equality case

        DocumentModel entry = dir.getEntry("2");
        entry.setProperty(SCHEMA_NAME, "a", "AAA");
        dir.updateEntry(entry);

        // our data:
        // 1 -> AAA, BCD
        // 2 -> AAA, AAA

        orderBy.clear();
        orderBy.put("a", "asc");
        orderBy.put("b", "asc");
        entries = dir.query(filter, fulltext, orderBy);
        assertEquals(Arrays.asList("2", "1"), entryIds(entries));

        orderBy.clear();
        orderBy.put("a", "asc");
        orderBy.put("b", "desc");
        entries = dir.query(filter, fulltext, orderBy);
        assertEquals(Arrays.asList("1", "2"), entryIds(entries));

        orderBy.clear();
        orderBy.put("a", "desc");
        orderBy.put("b", "asc");
        entries = dir.query(filter, fulltext, orderBy);
        assertEquals(Arrays.asList("2", "1"), entryIds(entries));

        orderBy.clear();
        orderBy.put("a", "desc");
        orderBy.put("b", "desc");
        entries = dir.query(filter, fulltext, orderBy);
        assertEquals(Arrays.asList("1", "2"), entryIds(entries));

        // check number ordering

        entry = dir.getEntry("1");
        entry.setProperty(SCHEMA_NAME, "int", 2);
        dir.updateEntry(entry);
        entry = dir.getEntry("2");
        entry.setProperty(SCHEMA_NAME, "int", 10);
        dir.updateEntry(entry);

        orderBy.clear();
        orderBy.put("a", "asc");
        entries = dir.query(filter, fulltext, orderBy);
        assertEquals(Arrays.asList("1", "2"), entryIds(entries));
    }

    @Test
    public void testServiceUnregistration() {
        MemoryDirectoryDescriptor descr = new MemoryDirectoryDescriptor();
        descr.name = "mydir";
        descr.schemaName = SCHEMA_NAME;
        descr.idField = "i";
        descr.passwordField = "pw";
        descr.schemaSet = Set.of("i");

        DirectoryService service = Framework.getService(DirectoryService.class);
        service.registerDirectoryDescriptor(descr);

        Directory dir = service.getDirectory("mydir");
        assertNotNull(dir);
        List<Directory> dirs = service.getDirectories();
        assertEquals(1, dirs.size());
        assertNotNull(dirs.get(0));
        assertEquals(dir, dirs.get(0));

        service.unregisterDirectoryDescriptor(descr);
        dir = service.getDirectory("mydir");
        assertNull(dir);
        dirs = service.getDirectories();
        assertEquals(0, dirs.size());
    }

}
