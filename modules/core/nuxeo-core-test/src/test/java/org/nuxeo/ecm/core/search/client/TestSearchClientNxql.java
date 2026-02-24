/*
 * (C) Copyright 2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.search.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.core.query.sql.NXQL.ECM_FULLTEXT_SCORE;

import java.util.GregorianCalendar;

import org.junit.Test;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.search.IgnoreIfSearchClientDoesNotHaveIndexingCapability;
import org.nuxeo.ecm.core.search.SearchHit;
import org.nuxeo.ecm.core.search.SearchResponse;
import org.nuxeo.runtime.test.runner.ConditionalIgnore;

/**
 * @since 2025.0
 */
public class TestSearchClientNxql extends AbstractTestSearchClient {

    @Override
    protected String getSourceToIndex() {
        return "OSGI-INF/search/search-client-nxql-documents.ndjson";
    }

    @Test
    public void testBadNxl() {
        assertThrows(QueryParseException.class, () -> search("SELECT * FROM nowhere"));
    }

    @Test
    public void testNxqlFromDocumentation() {
        // Taken from https://doc.nuxeo.com/nxdoc/nxql/
        searchAndAssertHits("SELECT * FROM Document");
        searchAndAssertHits("SELECT * FROM Document WHERE ecm:primaryType IN ('File')");
        searchAndAssertNoHits("SELECT * FROM Note");
        searchAndAssertHits("SELECT * FROM File, Note");
        searchAndAssertHits("SELECT * FROM File WHERE dc:title='A file 1'", 1);
        searchAndAssertHits("SELECT * FROM File WHERE dc:title='A file 1' OR dc:title='A file 2'", 2);
        searchAndAssertHits(
                "SELECT * FROM Document WHERE (dc:title = 'blah' OR ecm:isProxy = 1) AND dc:contributors = 'bob'", 1);
        searchAndAssertHits("SELECT * FROM Document WHERE file:content/name = 'testfile.txt'", 1);
        searchAndAssertHits("SELECT * FROM Document WHERE file:content/data IS NULL");
        searchAndAssertHits("SELECT * FROM Document WHERE file:content/data IS NOT NULL", 1);
        searchAndAssertNoHits("SELECT * FROM Document WHERE uid = 'isbn1234'");
        searchAndAssertHits(
                "SELECT * FROM Document WHERE file:content/name = 'testfile.txt' OR dc:contributors = 'bob'", 2);
        searchAndAssertHits("SELECT * FROM Document WHERE dc:created BETWEEN DATE '2007-03-15' AND DATE '2008-01-01'",
                1);
        searchAndAssertHits(
                "SELECT * FROM Document WHERE dc:created NOT BETWEEN DATE '2007-01-01' AND DATE '2008-01-01'");
        searchAndAssertHits("SELECT * FROM Document WHERE dc:contributors = 'bob'");
        searchAndAssertHits("SELECT * FROM Document WHERE dc:contributors IN ('bob', 'john')");
        searchAndAssertHits("SELECT * FROM Document WHERE dc:contributors NOT IN ('bob', 'john')");
        searchAndAssertHits("SELECT * FROM Document WHERE dc:contributors <> 'pete'");
        searchAndAssertHits("SELECT * FROM Document WHERE dc:contributors <> 'blah'");
        searchAndAssertHits("SELECT * FROM Document WHERE dc:contributors <> 'blah' AND ecm:isProxy = 0");
        searchAndAssertHits("SELECT * FROM Document WHERE dc:title LIKE 'testfile%' ORDER BY dc:description", 3);
        searchAndAssertHits("SELECT * FROM Document WHERE dc:title LIKE 'testfile%' ORDER BY dc:description DESC", 3);
        searchAndAssertHits("SELECT * FROM Document ORDER BY ecm:path");
        searchAndAssertHits("SELECT * FROM Document ORDER BY ecm:path DESC");
        searchAndAssertHits("SELECT * FROM Document ORDER BY ecm:name");

        searchAndAssertNoHits("SELECT * FROM Document WHERE ecm:path STARTSWITH '/nothere'");
        searchAndAssertHits("SELECT * FROM Document WHERE dc:created >= DATE '2007-01-01'");
        searchAndAssertHits("SELECT * FROM Document WHERE dc:created >= TIMESTAMP '2007-03-15 00:00:00'");
        searchAndAssertHits(
                "SELECT * FROM Document WHERE dc:created >= DATE '2007-02-15' AND dc:created <= DATE '2007-03-15'");
        searchAndAssertHits("SELECT * FROM Document WHERE dc:expired = DATE '2023-07-13T22:00:00Z'", 1);
        searchAndAssertNoHits("SELECT * FROM Document WHERE dc:expired = DATE '2023-07-14T00:00:00+02:00'");
        searchAndAssertHits("SELECT * FROM Document WHERE dc:expired = TIMESTAMP '2023-07-14T00:00:00+02:00'", 1);
        searchAndAssertNoHits("SELECT * FROM Document WHERE my:boolean = 1");
        searchAndAssertHits("SELECT * FROM Document WHERE ecm:isProxy = 1");
        searchAndAssertHits("SELECT * FROM Document WHERE ecm:isVersion = 1");
        searchAndAssertHits("SELECT * FROM Document WHERE ecm:isProxy = 0 AND ecm:isVersion = 0", 12);
        searchAndAssertHits("SELECT * FROM Document WHERE ecm:uuid = 'a00'", 1);
        searchAndAssertHits("SELECT * FROM Document WHERE ecm:name = 'a00'", 1);
        searchAndAssertHits("SELECT * FROM Document WHERE ecm:parentId = 'f01'", 1);
        searchAndAssertNoHits("SELECT * FROM Document WHERE ecm:primaryType = 'Folder'");
        searchAndAssertHits("SELECT * FROM Document WHERE ecm:primaryType <> 'Folder'");
        searchAndAssertNoHits("SELECT * FROM Document WHERE ecm:primaryType = 'Note'");
        searchAndAssertNoHits("SELECT * FROM Document WHERE ecm:primaryType IN ('Folder', 'Note')");
        searchAndAssertHits("SELECT * FROM Document WHERE ecm:primaryType NOT IN ('Folder', 'Note')");

        searchAndAssertNoHits(
                "SELECT * FROM Document WHERE ecm:mixinType = 'Versionable' AND ecm:mixinType <> 'Downloadable'");
        searchAndAssertHits("SELECT * FROM Document WHERE ecm:mixinType <> 'Rendition'");
        searchAndAssertNoHits("SELECT * FROM Document WHERE ecm:mixinType = 'Rendition' AND dc:title NOT ILIKE '%pdf'");
        searchAndAssertNoHits("SELECT * FROM Document WHERE ecm:mixinType = 'Folderish'");
        // atlas resolves mixinType using primaryType, change to CustomDownloadable to avoid match
        searchAndAssertNoHits("SELECT * FROM Document WHERE ecm:mixinType = 'CustomDownloadable'");
        searchAndAssertNoHits("SELECT * FROM Document WHERE ecm:mixinType = 'CustomVersionable'");
        searchAndAssertNoHits("SELECT * FROM Document WHERE ecm:mixinType IN ('Folderish', 'CustomDownloadable')");
        searchAndAssertHits("SELECT * FROM Document WHERE ecm:mixinType NOT IN ('Folderish', 'CustomDownloadable')");
        searchAndAssertNoHits("SELECT * FROM Document WHERE ecm:currentLifeCycleState = 'project'");
        searchAndAssertNoHits("SELECT * FROM Document WHERE ecm:versionLabel = '1.0'");
        searchAndAssertHits("SELECT * FROM Document WHERE ecm:isTrashed = 0");
        searchAndAssertHits("SELECT * FROM Document WHERE ecm:fulltext = 'world'");
        searchAndAssertHits("SELECT * FROM Document WHERE ecm:fulltext_title = 'world'");

        searchAndAssertNoHits(
                "SELECT * FROM Document WHERE dc:title = 'hello world 1' ORDER BY ecm:currentLifeCycleState");
        searchAndAssertNoHits("SELECT * FROM Document WHERE dc:title = 'hello world 1' ORDER BY ecm:versionLabel");
        searchAndAssertNoHits(
                "SELECT * FROM Document WHERE ecm:parentId = '62cc5f29-f33e-479e-b122-e3922396e601' ORDER BY ecm:pos");
        //
        searchAndAssertHits("SELECT * FROM Document WHERE dc:expired IS NOT NULL", 1);
        searchAndAssertHits("SELECT * FROM Document WHERE dc:language = '' OR dc:language IS NULL");
        //
        searchAndAssertHits("SELECT * FROM File WHERE file:content/length > 0");
        searchAndAssertHits("SELECT * FROM File WHERE file:content/name = 'testfile.txt'");
        searchAndAssertHits("SELECT * FROM File ORDER BY file:content/length DESC");
        searchAndAssertNoHits("SELECT * FROM Document WHERE tst:couple/first/firstname = 'Steve'");
        searchAndAssertNoHits("SELECT * FROM Document WHERE tst:friends/0/firstname = 'John'");
        searchAndAssertNoHits("SELECT * FROM Document WHERE tst:friends/*/firstname = 'John'");
        searchAndAssertNoHits(
                "SELECT * FROM Document WHERE tst:friends/*1/firstname = 'John' AND tst:friends/*1/lastname = 'Smith'");
        searchAndAssertNoHits("SELECT tst:friends/*1/lastname FROM Document WHERE tst:friends/*1/firstname = 'John'");
        searchAndAssertHits("SELECT * FROM Document WHERE dc:subjects/* = 'something'");
        // limitation of opensearch, not supported
        searchAndAssertNoHits("SELECT * FROM Document WHERE dc:subjects/0 = 'something'");

        searchAndAssertHits("SELECT * FROM Document WHERE dc:created < NOW('-P1D')");
        searchAndAssertHits("SELECT * FROM Document WHERE dc:title LIKE 'test%'");
        // require a specific mapping
        searchAndAssertNoHits("SELECT * FROM Document WHERE dc:title ILIKE 'test%'");
        searchAndAssertHits("SELECT * FROM Document WHERE dc:title NOT LIKE '%oo%'");
        // ancestors
        searchAndAssertHits("SELECT * FROM Document WHERE ecm:ancestorId = 'root'", 4);
        searchAndAssertHits("SELECT * FROM Document WHERE ecm:ancestorId = 'f02'", 1);
        searchAndAssertHits("SELECT * FROM Document WHERE ecm:ancestorId <> 'f01'");
        searchAndAssertHits("SELECT * FROM Document WHERE ecm:ancestorId IN ('f02', 'f42')", 2);
        searchAndAssertHits("SELECT * FROM Document WHERE ecm:ancestorId NOT IN ('f42', 'f01', 'root')");
        searchAndAssertHits("SELECT * FROM Document WHERE ecm:ancestorId NOT IN ('f01') AND ecm:ancestorId IS NOT NULL",
                1);
    }

    // These tests require actual path materialization in a real repository and may not work with injected documents
    @Test
    @ConditionalIgnore(condition = IgnoreIfSearchClientDoesNotHaveIndexingCapability.class)
    public void testNxqlFromDocumentationAdvanced() {
        // startswith on ecm:path requires a true repository
        searchAndAssertHits("SELECT * FROM Document WHERE ecm:path STARTSWITH '/'");
        searchAndAssertHits("SELECT * FROM Document WHERE ecm:path STARTSWITH '/testfolder1'");
        searchAndAssertNoHits(
                "SELECT * FROM Document WHERE dc:title = 'testfile1_Title' AND ecm:path STARTSWITH '/foo'");
        searchAndAssertHits(
                "SELECT * FROM Document WHERE dc:title LIKE 'testfile%' AND ecm:path STARTSWITH '/testfolder1'", 3);
        // require a mapping for dc:coverage and dc:subject with .children fields like ecm:path
        searchAndAssertNoHits("SELECT * FROM Document WHERE dc:coverage STARTSWITH 'foo/bar'");
        searchAndAssertNoHits("SELECT * FROM Document WHERE dc:coverage STARTSWITH 'foo'");
        searchAndAssertNoHits("SELECT * FROM Document WHERE dc:subjects STARTSWITH 'gee'");
        searchAndAssertNoHits("SELECT * FROM Document WHERE dc:subjects STARTSWITH 'gee/moo'");
    }

    @Test
    public void testOrdering() {
        SearchResponse response;
        response = searchAndAssertHits(
                "SELECT * FROM Document WHERE ecm:uuid IN ('a00', 'a02', 'a01') ORDER BY ecm:uuid ASC", 3);
        assertEquals("a00", response.getHits().get(0).getDocId());
        assertEquals("a01", response.getHits().get(1).getDocId());
        assertEquals("a02", response.getHits().get(2).getDocId());

        response = searchAndAssertHits(
                "SELECT * FROM Document WHERE ecm:uuid IN ('a00', 'a02', 'a01') ORDER BY ecm:uuid DESC", 3);
        assertEquals("a02", response.getHits().get(0).getDocId());
        assertEquals("a01", response.getHits().get(1).getDocId());
        assertEquals("a00", response.getHits().get(2).getDocId());

        response = searchAndAssertHits(
                "SELECT * FROM Document WHERE ecm:uuid IN ('a00', 'a02', 'a01') ORDER BY dc:modified ASC", 3);
        assertEquals("a02", response.getHits().get(0).getDocId());
        assertEquals("a01", response.getHits().get(1).getDocId());
        assertEquals("a00", response.getHits().get(2).getDocId());

        response = searchAndAssertHits("SELECT ecm:fulltextScore FROM Document WHERE ecm:fulltext = 'world'", 1);
        assertTrue((double) response.getHits().get(0).getFields().get(ECM_FULLTEXT_SCORE) > 0);
    }

    @Test
    public void testSelectClauses() {
        SearchResponse response;
        response = searchAndAssertHits(
                "SELECT ecm:primaryType, dc:title, dc:modified FROM Document WHERE ecm:uuid IN ('a00', 'a02', 'a01') ORDER BY ecm:uuid ASC");
        SearchHit first = response.getHits().getFirst();
        assertNotNull("a00", first.getId());
        assertNotNull("a00", first.getDocId());
        assertEquals("default", first.getRepository());
        assertNotNull("a00", first.getFields().get("ecm:uuid"));
        assertEquals("default", first.getFields().get("ecm:repository"));
        assertEquals("File", first.getFields().get("ecm:primaryType"));
        assertEquals("A file 1", first.getFields().get("dc:title"));
        if (first.getFields().get("dc:modified") instanceof GregorianCalendar modified) {
            assertEquals("2024-08-15T14:00:00Z", modified.toInstant().toString());
        } else {
            fail("Expecting a Date prop, bug got: " + first.getFields().get("dc:modified"));
        }
    }
}
