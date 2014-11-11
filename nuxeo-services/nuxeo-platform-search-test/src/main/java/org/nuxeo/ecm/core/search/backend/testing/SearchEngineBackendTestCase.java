/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id:TestSearchEnginePluginRegistration.java 13121 2007-03-01 18:07:58Z janguenot $
 */

package org.nuxeo.ecm.core.search.backend.testing;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.query.sql.SQLQueryParser;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.search.api.backend.SearchEngineBackend;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResources;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.factory.BuiltinDocumentFields;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.common.SearchServiceDelegate;
import org.nuxeo.ecm.core.search.api.client.query.ComposedNXQuery;
import org.nuxeo.ecm.core.search.api.client.query.impl.ComposedNXQueryImpl;
import org.nuxeo.ecm.core.search.api.client.query.impl.SearchPrincipalImpl;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultItem;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;
import org.nuxeo.ecm.core.search.api.client.search.results.document.SearchPageProvider;
import org.nuxeo.ecm.core.search.api.internals.SearchServiceInternals;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * A generic test that all backends have to pass.
 * <p>
 * Derived test cases have to:
 * <ul>
 * <li>override the ENGINE_NAME field,
 * <li>put nxsearch-framework.xml, and nxsearch-<em>ENGINE_NAME</em>-contrib.xml
 * in your test resources
 * </ul>
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 * @author <a href="mailto:gr@nuxeo.com">Georges Racinet</a>
 */
public abstract class SearchEngineBackendTestCase extends NXRuntimeTestCase {

    protected SearchService service;

    protected String ENGINE_NAME;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core");
        deployContrib("org.nuxeo.ecm.platform.search.test",
                "nxsearch-backendtest-types-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.search.test",
                "nxsearch-backendtest-framework.xml");
        service = SearchServiceDelegate.getRemoteSearchService();
        assertNotNull(service);
        deployContrib("org.nuxeo.ecm.platform.search.test",
                "nxsearch-backendtest-contrib.xml");
        assertEquals("barcode",
                getSearchServiceInternals().getIndexableDataConfFor(
                        "bk:barcode").getIndexingName());
    }

    private SearchServiceInternals getSearchServiceInternals() {
        return (SearchServiceInternals) service;
    }

    public void testRegistration() {
        assertEquals(1,
                getSearchServiceInternals().getSearchEngineBackends().size());
        SearchEngineBackend backend = getBackend();
        assertNotNull(backend);
        assertEquals(ENGINE_NAME, backend.getName());
    }

    public SearchEngineBackend getBackend() {
        return getSearchServiceInternals().getSearchEngineBackendByName(
                ENGINE_NAME);
    }

    private static ComposedNXQuery composeQuery(String query) {
        SQLQuery nxqlQuery = SQLQueryParser.parse(query);
        return new ComposedNXQueryImpl(nxqlQuery);
    }

    private static ComposedNXQuery composeQuery(String query, String name,
            String... groups) {
        SQLQuery nxqlQuery = SQLQueryParser.parse(query);
        return new ComposedNXQueryImpl(nxqlQuery, new SearchPrincipalImpl(name,
                groups, false, false));
    }

    /**
     * A full cycle on one document.
     *
     * @throws Exception
     */
    public void testOneDoc() throws Exception {
        SearchEngineBackend backend = getBackend();
        ResolvedResources resources = SharedTestDataBuilder.makeAboutLifeAggregated();
        backend.index(resources);

        SQLQuery nxqlQuery = SQLQueryParser.parse("SELECT * FROM Document WHERE bk:barcode='0000'");
        ResultSet results = backend.searchQuery(new ComposedNXQueryImpl(
                nxqlQuery), 0, 100);
        assertEquals(1, results.getTotalHits());
        assertEquals(1, results.getPageHits());
        assertEquals(0, results.getOffset());
        assertEquals(100, results.getRange());
        assertFalse(results.hasNextPage());
        assertTrue(results.isFirstPage());
        ResultItem resItem = results.get(0);

        // check that we have all stored properties
        assertEquals("About Life", resItem.get("dc:title"));
        assertEquals("0000", resItem.get("bk:barcode"));
        assertEquals("Abstracts aren't indexed but stored",
                resItem.get("bk:abstract"));
        assertEquals(new PathRef("some/path"),
                resItem.get(BuiltinDocumentFields.FIELD_DOC_REF));
        assertFalse(resItem.containsKey("bk:contents"));
        assertEquals(437L, resItem.get("bk:pages"));

        assertNull(results.nextPage());

        // Now replay()
        ResultSet replayed = results.replay();
        // full assertEquals not directly possible
        assertEquals(results.getTotalHits(), replayed.getTotalHits());
        assertEquals(results.get(0).get("bk:barcode"), replayed.get(0).get(
                "bk:barcode"));

        // Delete the resource and use replay() to check
        // This actually relies on internals of the currently only
        // existing backend, that merges all schema resources into a single one
        // and use the join id as primary id for this merging
        // Disabled because doesn't make sense anymore now that the backend
        // is allowed to actually merge several atomic resources into one.
        // TODO must specify what should happen
        backend.deleteAtomicResource("agg_id");
        replayed = results.replay();
        assertEquals(0, replayed.getTotalHits());
        assertEquals(0, replayed.getPageHits());
        assertEquals(0, replayed.getOffset());
        assertEquals(100, replayed.getRange());
        assertFalse(replayed.hasNextPage());
        assertTrue(replayed.isFirstPage());
        backend.index(resources);

        // Recreate
        replayed = results.replay();
        assertEquals(1, replayed.getTotalHits());

        backend.deleteAggregatedResources("agg_id");
        replayed = results.replay();
        assertEquals(0, replayed.getTotalHits());
    }

    // disabled because loading of test resource didn't work
    // on the hudson bot. TODO
    public void xtestNonDefaultAnalyzer() throws Exception {
        SearchEngineBackend backend = getBackend();

        backend.index(SharedTestDataBuilder.makeAboutLifeAggregated());
        // tokenized
        ResultSet results = backend.searchQuery(
                composeQuery("SELECT * FROM Document WHERE bk:frenchtitle='vie'"),
                0, 100);
        assertEquals(1, results.getTotalHits());
        // accent degradation
        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document WHERE bk:frenchtitle='mechante'"),
                0, 100);
        assertEquals(1, results.getTotalHits());
    }

    public void testSecurityIndex() throws Exception {
        SearchEngineBackend backend = getBackend();
        backend.index(SharedTestDataBuilder.makeAboutLifeAggregated());
        backend.index(SharedTestDataBuilder.makeWarPeace());
        ResultSet results;

        String aboutLifeQuery = "SELECT * FROM Document "
                + "WHERE bk:barcode = '0000'";

        results = backend.searchQuery(composeQuery(aboutLifeQuery, "dupont",
                "sales"), 0, 100);
        assertEquals(1, results.getTotalHits());

        results = backend.searchQuery(composeQuery(aboutLifeQuery, "hugo",
                "authors"), 0, 100);
        assertEquals(0, results.getTotalHits());

        results = backend.searchQuery(composeQuery(aboutLifeQuery, "smith",
                "employees"), 0, 100);
        assertEquals(1, results.getTotalHits());

        results = backend.searchQuery(composeQuery(aboutLifeQuery, "smith",
                "accountants"), 0, 100);
        assertEquals(0, results.getTotalHits());

        results = backend.searchQuery(composeQuery(aboutLifeQuery, "smith"), 0,
                100);
        assertEquals(0, results.getTotalHits());

        results = backend.searchQuery(composeQuery(aboutLifeQuery, "tolstoi",
                "authors"), 0, 100);
        assertEquals(1, results.getTotalHits());

        // War and Peace is equipped with a complex ACP, involving groups of
        // permissions. This is not what's being tested here
        results = backend.searchQuery(composeQuery(
                "SELECT * FROM Document WHERE bk:barcode='0018'", "tolstoi",
                "foo", "authors"), 0, 100);
        assertEquals(0, results.getTotalHits());

        String matchBothQuery = "SELECT * FROM Document "
                + "WHERE bk:category IN ('autobio', 'novel')";

        // relies on working IN query, let's ensure it works
        results = backend.searchQuery(composeQuery(matchBothQuery), 0, 100);
        assertEquals(2, results.getTotalHits());

        results = backend.searchQuery(composeQuery(matchBothQuery, "tolstoi",
                "authors"), 0, 100);
        assertEquals(1, results.getTotalHits());
        assertEquals("About Life", results.get(0).get("dc:title"));

        // TODO write a test with batching & sorting
    }

    public void testSecurityIndexMultiplePerms() throws Exception {
        SearchEngineBackend backend = getBackend();
        backend.index(SharedTestDataBuilder.makeWarPeace());
        ResultSet results;
        String wPQuery = "SELECT * FROM Document WHERE bk:barcode='0018'";

        // Granted because "admins" group has EVERYTHING
        results = backend.searchQuery(
                composeQuery(wPQuery, "someone", "admins"), 0, 100);
        assertEquals(1, results.getTotalHits());

        // Granted because "authors" group has READ
        results = backend.searchQuery(composeQuery(wPQuery, "someone",
                "authors"), 0, 100);
        assertEquals(1, results.getTotalHits());

        // Not granted because none of both security tokens correspond to rights
        results = backend.searchQuery(composeQuery(wPQuery, "someone",
                "unknown"), 0, 100);
        assertEquals(0, results.getTotalHits());

        // Not granted because "tolstoi" has a deny on READ
        // before the grant of "admins"
        results = backend.searchQuery(
                composeQuery(wPQuery, "tolstoi", "admins"), 0, 100);
        assertEquals(0, results.getTotalHits());

        // Not granted because "durand" loses EVERYTHING.
        results = backend.searchQuery(
                composeQuery(wPQuery, "durand", "authors"), 0, 100);
        assertEquals(0, results.getTotalHits());

        // Granted because "goethe" has BROWSE before the READ deny for
        // "tolstoi" "authors"
        results = backend.searchQuery(composeQuery(wPQuery, "goethe",
                "tolstoi", "authors"), 0, 100);
        assertEquals(1, results.getTotalHits());
    }

    public void testRefQuery() throws Exception {
        // Ref queries work for IdRef only
        SearchEngineBackend backend = getBackend();
        backend.index(SharedTestDataBuilder.makeWarPeace());
        ResultSet results;

        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document WHERE "
                        + BuiltinDocumentFields.FIELD_DOC_REF
                        + " = 'war-peace'"), 0, 100);
        assertEquals(1, results.getTotalHits());
    }

    public void testIntBoolQuery() throws Exception {
        SearchEngineBackend backend = getBackend();
        backend.index(SharedTestDataBuilder.makeAboutLifeAggregated());
        backend.index(SharedTestDataBuilder.makeWarPeace());
        ResultSet results;

        /*
         *
         * results = backend.searchQuery( composeQuery("SELECT * FROM Document
         * WHERE bk:pages = 437"), 0, 100); assertEquals(1,
         * results.getTotalHits()); assertEquals("About Life",
         * results.get(0).get("dc:title"));
         *
         * results = backend.searchQuery( composeQuery("SELECT * FROM Document
         * WHERE bk:pages > 437"), 0, 100); assertEquals(1,
         * results.getTotalHits()); results = backend.searchQuery(
         * composeQuery("SELECT * FROM Document WHERE bk:pages >= 437"), 0,
         * 100); assertEquals(2, results.getTotalHits());
         *
         * results = backend.searchQuery( composeQuery("SELECT * FROM Document
         * WHERE bk:pages < 789"), 0, 100); assertEquals(1,
         * results.getTotalHits()); assertEquals("About Life",
         * results.get(0).get("dc:title"));
         *
         * results = backend.searchQuery( composeQuery("SELECT * FROM Document
         * WHERE bk:pages <= 789"), 0, 100);
         *
         */
        // BOOLEAN
        results = backend.searchQuery(composeQuery("SELECT * FROM Document "
                + "WHERE ecm:isCheckedInVersion = 1"), 0, 100);

        assertEquals(1, results.getTotalHits());
        assertEquals("About Life", results.get(0).get("dc:title"));

        results = backend.searchQuery(composeQuery("SELECT * FROM Document "
                + "WHERE ecm:isCheckedInVersion = 0"), 0, 100);

        assertEquals(1, results.getTotalHits());
        assertEquals("War and Peace", results.get(0).get("dc:title"));
    }

    /**
     * Test on several documents: "About Life" 12 variations on "Revelations".
     *
     * @throws Exception
     */
    public void testBunch() throws Exception {
        SearchEngineBackend backend = getBackend();

        // Indexations
        backend.index(SharedTestDataBuilder.makeAboutLifeAggregated());
        for (ResolvedResources rvl : SharedTestDataBuilder.revelationsBunch(12)) {
            backend.index(rvl);
        }

        // Looking for About Life
        ResultSet results = backend.searchQuery(
                composeQuery("SELECT * FROM Document WHERE bk:barcode='0000'"),
                0, 100);
        assertEquals(1, results.getTotalHits());
        assertEquals(1, results.getPageHits());
        assertEquals(0, results.getOffset());
        assertEquals(100, results.getRange());
        assertFalse(results.hasNextPage());
        assertTrue(results.isFirstPage());
        ResultItem resItem = results.get(0);

        // check that this the correct one
        assertEquals("About Life", resItem.get("dc:title"));

        // Looking for Revelations
        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document WHERE dc:title='Revelations'"),
                0, 10);
        assertEquals(12, results.getTotalHits());
        assertEquals(10, results.getPageHits());
        assertEquals(0, results.getOffset());
        assertEquals(10, results.getRange());
        assertTrue(results.hasNextPage());
        assertTrue(results.isFirstPage());

        // check that this is the correct one.
        resItem = results.get(0);
        assertEquals("Revelations", resItem.get("dc:title"));

        results = results.nextPage();
        assertEquals(12, results.getTotalHits());
        assertEquals(2, results.getPageHits());
        assertEquals(10, results.getOffset());
        assertEquals(10, results.getRange());
        assertFalse(results.hasNextPage());
        assertFalse(results.isFirstPage());

        // check document model formation through wrapping in page provider
        PagedDocumentsProvider provider = new SearchPageProvider(results);
        DocumentModelList docModels = provider.getCurrentPage();

        assertEquals("Revelations", docModels.get(0).getProperty("dublincore",
                "title"));
        assertEquals("project", docModels.get(0).getCurrentLifeCycleState());

        // Barcode is not full-text indexed
        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document WHERE bk:barcode='RVL'"),
                0, 2);
        assertEquals(0, results.getTotalHits());

        // STARTSWITH on a non text field
        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document WHERE bk:category "
                        + "STARTSWITH 'auto'"), 0, 2);
        assertEquals(1, results.getTotalHits());
        assertEquals("About Life", results.get(0).get("dc:title"));

        //
        // IN queries
        //
        results = backend.searchQuery(composeQuery("SELECT * FROM Document "
                + "WHERE bk:category IN ('autobio', 'novel')"), 0, 3);
        assertEquals(12 + 1, results.getTotalHits());
        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document WHERE "
                        + "bk:category IN ('autobio', 'junk')"), 0, 1);
        assertEquals(1, results.getTotalHits());

        // Tests with a multiple Keyword property
        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document WHERE bk:tags = 'people'"),
                0, 1);
        assertEquals(13, results.getTotalHits());
        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document WHERE bk:tags = 'philosophy'"),
                0, 1);
        assertEquals(1, results.getTotalHits());
        // IN result in intersection
        results = backend.searchQuery(composeQuery("SELECT * FROM Document "
                + "WHERE bk:tags IN ('gossip', 'philosophy')"), 0, 1);
        assertEquals(13, results.getTotalHits());
        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document WHERE bk:tags = 'gossip'"),
                0, 1);
        assertEquals(12, results.getTotalHits());

        // Boolean
        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document WHERE "
                        + "bk:category IN ('novel', 'junk') "
                        + "AND dc:title = 'Life'"), 0, 1);
        assertEquals(0, results.getTotalHits());

        // Path Queries
        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document WHERE "
                        + BuiltinDocumentFields.FIELD_DOC_PATH
                        + " STARTSWITH 'some'"), 0, 100);
        assertEquals(12 + 1, results.getTotalHits());

        // Add War and Peace for more path prefixes
        // TODO Make this more unitary
        backend.index(SharedTestDataBuilder.makeWarPeace());

        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document WHERE "
                        + BuiltinDocumentFields.FIELD_DOC_PATH
                        + " STARTSWITH 'russian'"), 0, 100);
        assertEquals(1, results.getTotalHits());
        assertEquals("War and Peace", results.get(0).get("dc:title"));

        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document WHERE "
                        + BuiltinDocumentFields.FIELD_DOC_PATH
                        + " STARTSWITH 'some/rev'"), 0, 100);
        assertEquals(12, results.getTotalHits());
        // check path is stored in appropriate way
        assertEquals("some/rev/3", results.get(3).get(
                BuiltinDocumentFields.FIELD_DOC_PATH));

        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document WHERE "
                        + BuiltinDocumentFields.FIELD_FULLTEXT
                        + " LIKE 'text full'"), 0, 100);
        assertEquals(12 + 2, results.getTotalHits());

        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document WHERE "
                        + BuiltinDocumentFields.FIELD_FULLTEXT
                        + "= 'text full'"), 0, 100);
        assertEquals(0, results.getTotalHits());

        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document WHERE "
                        + BuiltinDocumentFields.FIELD_FULLTEXT
                        + " LIKE '+about +life optional stuff'"), 0, 100);
        assertEquals(1, results.getTotalHits());
        assertEquals("About Life", results.get(0).get("dc:title"));
    }

    /*
     * Test separated from testBunch() because no way to predict inequalities
     * behaviour with null dates.
     *
     * used to be disabled because same problem on bot as testOneDoc
     */
    public void testTimeStampDateQueries() throws Exception {

        // Prerequisites
        // assertNotNull(service.getIndexableResourceConfByName("published"));

        // Preparation
        SearchEngineBackend backend = getBackend();
        for (ResolvedResources rvl : SharedTestDataBuilder.revelationsBunch(12)) {
            backend.index(rvl);
        }

        ResultSet results;

        // shift by 1 because for Calendar january is month #0
        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document WHERE "
                        + "bk:published = TIMESTAMP '2007-04-12 03:57:00'"), 0,
                100);
        assertEquals(1, results.getTotalHits());
        assertEquals("1350011", results.get(0).get("bk:barcode"));

        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document WHERE "
                        + "bk:published > TIMESTAMP '2007-04-11 03:56:00'"), 0,
                100);

        assertEquals(2, results.getTotalHits());
        assertEquals("1350010", results.get(0).get("bk:barcode"));
        assertEquals("1350011", results.get(1).get("bk:barcode"));

        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document WHERE "
                        + "bk:published >= TIMESTAMP '2007-04-11 03:57:00'"),
                0, 100);

        assertEquals(2, results.getTotalHits());
        assertEquals("1350010", results.get(0).get("bk:barcode"));
        assertEquals("1350011", results.get(1).get("bk:barcode"));

        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document WHERE "
                        + "bk:published < TIMESTAMP '2007-04-05 03:57:00'"), 0,
                100);
        assertEquals(4, results.getTotalHits());
        assertEquals("1350000", results.get(0).get("bk:barcode"));
        assertEquals("1350001", results.get(1).get("bk:barcode"));
        assertEquals("1350002", results.get(2).get("bk:barcode"));
        assertEquals("1350003", results.get(3).get("bk:barcode"));

        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document WHERE "
                        + "bk:published = DATE '2007-04-05'"), 0, 100);
        assertEquals(1, results.getTotalHits());
        assertEquals("1350004", results.get(0).get("bk:barcode"));

        // TODO test boundaries with a doc created at 0:0 AM
        // (delicate with timezone info.)

        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document WHERE "
                        + "bk:published BETWEEN DATE '2007-04-05' AND "
                        + "DATE '2007-04-07'"), 0, 100);
        assertEquals(3, results.getTotalHits());
        assertEquals("1350004", results.get(0).get("bk:barcode"));
        assertEquals("1350005", results.get(1).get("bk:barcode"));
        assertEquals("1350006", results.get(2).get("bk:barcode"));
    }

    public void testResultItem() throws Exception {
        // Index "About Life and retrieve it
        SearchEngineBackend backend = getBackend();
        backend.index(SharedTestDataBuilder.makeAboutLifeAggregated());
        ResultSet results = backend.searchQuery(
                composeQuery("SELECT * FROM Document WHERE bk:barcode='0000'"),
                0, 100);
        assertEquals(1, results.getTotalHits());
        ResultItem resItem = results.get(0);

        // Now check the stored properties
        assertEquals("About Life", resItem.get("dc:title"));
        assertEquals("La méchante vie de l'auteur",
                resItem.get("bk:frenchtitle"));
        assertEquals(Arrays.asList("philosophy", "people"),
                resItem.get("bk:tags"));
        assertEquals("0000", resItem.get("bk:barcode"));
        assertEquals("Abstracts aren't indexed but stored",
                resItem.get("bk:abstract"));
        assertFalse(resItem.containsKey("bk:contents"));
    }

    public void testResultItem2() throws Exception {
        // Index one "Revelations"
        SearchEngineBackend backend = getBackend();
        backend.index(SharedTestDataBuilder.revelationsBunch(1)[0]);
        ResultSet results = backend.searchQuery(
                composeQuery("SELECT * FROM Document "
                        + "WHERE bk:barcode='1350000'"), 0, 100);
        assertEquals(1, results.getTotalHits());
        ResultItem resItem = results.get(0);

        // Now check the stored properties, notably the date
        Calendar pub = (Calendar) resItem.get("bk:published");
        Calendar calValue = Calendar.getInstance();
        calValue.clear();
        calValue.set(2007, 3, 1, 3, 57);

        // Other means of checking fail, including compareTo !!
        assertEquals(calValue.getTimeInMillis(), pub.getTimeInMillis());
    }

    /*
     * Like all tests on LIKE queries, we assume that the LIKE syntax here is
     * Lucene's QueryParser syntax. Derived class are of course free to override
     * to adapt to a given backend natural syntax.
     *
     * TODO providing higher level API to test LIKE queries would make it easier
     * to apply to other syntaxes.
     */
    public void testNegativeLikeQueries() throws Exception {
        // indexations
        SearchEngineBackend backend = getBackend();
        backend.index(SharedTestDataBuilder.revelationsBunch(1)[0]);
        backend.index(SharedTestDataBuilder.makeWarPeace());
        backend.index(SharedTestDataBuilder.makeAboutLifeAggregated());

        ResultSet results;
        results = backend.searchQuery(composeQuery("SELECT * FROM Document "
                + "WHERE dc:title NOT LIKE 'Zorglub'"), 0, 100);
        assertEquals(3, results.getTotalHits());

        results = backend.searchQuery(composeQuery("SELECT * FROM Document "
                + "WHERE dc:title NOT LIKE 'war'"), 0, 100);
        assertEquals(2, results.getTotalHits());
        List<Serializable> titles = Arrays.asList(
                results.get(0).get("dc:title"), results.get(1).get("dc:title"));
        assertTrue(titles.contains("About Life"));
        assertTrue(titles.contains("Revelations"));

        results = backend.searchQuery(composeQuery("SELECT * FROM Document "
                + "WHERE dc:title LIKE 'war' "
                + "AND dc:title NOT LIKE 'Zorglub'"), 0, 100);
        assertEquals(1, results.getTotalHits());
        assertEquals("War and Peace", results.get(0).get("dc:title"));

        results = backend.searchQuery(composeQuery("SELECT * FROM Document "
                + "WHERE dc:title LIKE 'war' "
                + "AND dc:title NOT LIKE 'peace'"), 0, 100);
        assertEquals(0, results.getTotalHits());
    }

    public void testNegativeQueries() throws Exception {
        // indexations
        SearchEngineBackend backend = getBackend();
        for (ResolvedResources rev : SharedTestDataBuilder.revelationsBunch(2)) {
            backend.index(rev);
        }
        backend.index(SharedTestDataBuilder.makeWarPeace());
        backend.index(SharedTestDataBuilder.makeAboutLifeAggregated());

        ResultSet results;

        // NOT EQ
        results = backend.searchQuery(composeQuery("SELECT * FROM Document "
                + "WHERE bk:barcode != '0000'"), 0, 100);
        assertEquals(3, results.getTotalHits());
        List<Serializable> codes = Arrays.asList(results.get(0).get(
                "bk:barcode"), results.get(1).get("bk:barcode"),
                results.get(2).get("bk:barcode"));
        assertTrue(codes.contains("0018"));
        assertTrue(codes.contains("1350000"));
        assertTrue(codes.contains("1350001"));

        results = backend.searchQuery(composeQuery("SELECT * FROM Document "
                + "WHERE bk:barcode != '0018' " + "AND bk:category = 'novel'"),
                0, 100);
        assertEquals(2, results.getTotalHits());
        assertEquals("Revelations", results.get(0).get("dc:title"));
        assertEquals("Revelations", results.get(1).get("dc:title"));

        // NOT BETWEEN

        results = backend.searchQuery(composeQuery("SELECT * FROM Document "
                + "WHERE bk:published NOT BETWEEN DATE '2007-03-01' "
                + "AND DATE '2007-05-01'"), 0, 100);
        // reminder, published dates are null on About Life and War and Peace
        assertEquals(2, results.getTotalHits());
        assertNotSame("Revelations", results.get(0).get("dc:title"));
        assertNotSame("Revelations", results.get(1).get("dc:title"));

        results = backend.searchQuery(composeQuery("SELECT * FROM Document "
                + "WHERE bk:barcode >= '1350000' AND "
                + "bk:published NOT BETWEEN DATE '2007-03-01' "
                + "AND DATE '2007-04-01'"), 0, 100);
        assertEquals(1, results.getTotalHits());
        assertEquals("1350001", results.get(0).get("bk:barcode"));

        // NOT IN
        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document "
                        + "WHERE bk:barcode NOT IN "
                        + "('0000', '1350000', '1350001')"), 0, 100);
        assertEquals(1, results.getTotalHits());
        assertEquals("0018", results.get(0).get("bk:barcode"));

        results = backend.searchQuery(composeQuery("SELECT * FROM Document "
                + "WHERE bk:category = 'novel' AND "
                + "bk:barcode NOT IN ('1350000', '1350001')"), 0, 100);
        assertEquals(1, results.getTotalHits());
        assertEquals("0018", results.get(0).get("bk:barcode"));

        // NOT STARTSWITH
        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document where NOT bk:category STARTSWITH 'auto'"),
                0, 100);
        assertEquals(3, results.getTotalHits()); // About life doesn't match
        for (int i = 0; i < 3; i++) {
            assertFalse(((String) results.get(i).get("bk:category")).startsWith("auto"));
        }

        // cases of OR NOT
        results = backend.searchQuery(composeQuery("SELECT * FROM Document "
                + "WHERE bk:category = 'nosuch' OR NOT "
                + "bk:barcode IN ('0018', '1350000', '1350001')"), 0, 100);
        assertEquals(1, results.getTotalHits());
        assertEquals("0000", results.get(0).get("bk:barcode"));

        results = backend.searchQuery(composeQuery("SELECT * FROM Document "
                + "WHERE bk:category = 'autobio' OR NOT "
                + "bk:barcode IN ('1350000', '1350001')"), 0, 100);
        assertEquals(2, results.getTotalHits());
        codes = Arrays.asList(results.get(0).get("bk:barcode"),
                results.get(1).get("bk:barcode"));
        assertTrue(codes.contains("0018"));
        assertTrue(codes.contains("0000"));

        results = backend.searchQuery(composeQuery("SELECT * FROM Document "
                + "WHERE NOT bk:barcode = '0000' OR NOT "
                + "bk:barcode = '0018'"), 0, 100);
        assertEquals(4, results.getTotalHits());
    }

    public void xtestEmptyLists() throws Exception {
        SearchEngineBackend backend = getBackend();

        // Indexations
        backend.index(SharedTestDataBuilder.makeAboutLifeAggregated());
        backend.index(SharedTestDataBuilder.makeWarPeace());

        // Queries
        // TODO parser doesn't accept the query yet
        ResultSet results;
        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document WHERE bk:tags = ()"), 0, 2);
        assertEquals(1, results.getTotalHits());
        assertEquals("War and Peace", results.get(0).get("Title"));
    }

    public void testIntOrderClauses() throws Exception {
        SearchEngineBackend backend = getBackend();
        backend.index(SharedTestDataBuilder.makeAboutLifeAggregated());
        backend.index(SharedTestDataBuilder.makeWarPeace());

        ResultSet results;
        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document ORDER BY bk:pages"), 0,
                100);
        assertEquals(2, results.getTotalHits());
        assertEquals("About Life", results.get(0).get("dc:title"));
        assertEquals("War and Peace", results.get(1).get("dc:title"));

        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document ORDER BY bk:pages DESC"),
                0, 100);
        assertEquals(2, results.getTotalHits());
        assertEquals("War and Peace", results.get(0).get("dc:title"));
        assertEquals("About Life", results.get(1).get("dc:title"));
    }

    /*
     * With most backends, Text fields require special treatment to be sortable
     * Both directions are needed in all cases to avoid coincidences
     *
     * @see org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedData#isSortable()
     */
    public void testTextOrderClauses() throws Exception {
        SearchEngineBackend backend = getBackend();
        backend.index(SharedTestDataBuilder.makeAboutLifeAggregated());
        backend.index(SharedTestDataBuilder.makeWarPeace());

        ResultSet results;
        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document ORDER BY dc:title"), 0,
                100);
        assertEquals(2, results.getTotalHits());
        assertEquals("About Life", results.get(0).get("dc:title"));
        assertEquals("War and Peace", results.get(1).get("dc:title"));

        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document ORDER BY dc:title DESC"),
                0, 100);
        assertEquals(2, results.getTotalHits());
        assertEquals("War and Peace", results.get(0).get("dc:title"));
        assertEquals("About Life", results.get(1).get("dc:title"));

        // Now a text field that's sortable in a case-insensitive way
        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document ORDER BY bk:frenchtitle"),
                0, 100);
        assertEquals(2, results.getTotalHits());
        assertEquals("War and Peace", results.get(0).get("dc:title")); // La
                                                                        // Guerre
        assertEquals("About Life", results.get(1).get("dc:title")); // La
                                                                    // mechante
        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document ORDER BY bk:frenchtitle DESC"),
                0, 100);
        assertEquals("About Life", results.get(0).get("dc:title")); // La
                                                                    // mechante
        assertEquals("War and Peace", results.get(1).get("dc:title")); // La
                                                                        // Guerre

        // Now a text field that's not declared as sortable. We can get a
        // QueryException and nothing else

        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document ORDER BY bk:abstract"), 0,
                100);
        // don't catch other exceptions, to get the stack trace
    }

    public void testNullDateQuery() throws Exception {
        SearchEngineBackend backend = getBackend();
        backend.index(SharedTestDataBuilder.revelationsEmptyDate(2));
        ResultSet results = backend.searchQuery(
                composeQuery("SELECT * FROM Document WHERE bk:published = ''"),
                0, 100);
        assertEquals(1, results.size());
    }

    public void testDateOrderClauses() throws Exception {
        SearchEngineBackend backend = getBackend();
        // Indexation. Random order to avoid false positives
        backend.index(SharedTestDataBuilder.revelations(2));
        backend.index(SharedTestDataBuilder.revelations(5));
        backend.index(SharedTestDataBuilder.revelations(1));
        ResultSet results;

        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document ORDER BY bk:published"),
                0, 100);
        assertEquals(3, results.getTotalHits());
        assertEquals("1350001", results.get(0).get("bk:barcode"));
        assertEquals("1350002", results.get(1).get("bk:barcode"));
        assertEquals("1350005", results.get(2).get("bk:barcode"));

        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document ORDER BY bk:published DESC"),
                0, 100);
        assertEquals(3, results.getTotalHits());
        assertEquals("1350005", results.get(0).get("bk:barcode"));
        assertEquals("1350002", results.get(1).get("bk:barcode"));
        assertEquals("1350001", results.get(2).get("bk:barcode"));

        // Now with two criteria
        backend.index(SharedTestDataBuilder.makeAboutLifeAggregated());
        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document ORDER BY bk:category, bk:published"),
                0, 100);
        assertEquals(4, results.getTotalHits());
        assertEquals("0000", results.get(0).get("bk:barcode"));
        assertEquals("1350001", results.get(1).get("bk:barcode"));
        assertEquals("1350002", results.get(2).get("bk:barcode"));
        assertEquals("1350005", results.get(3).get("bk:barcode"));

        // Now with two criteria
        backend.index(SharedTestDataBuilder.makeAboutLifeAggregated());
        results = backend.searchQuery(
                composeQuery("SELECT * FROM Document ORDER BY bk:category, bk:published DESC"),
                0, 100);
        assertEquals(4, results.getTotalHits());
        assertEquals("1350005", results.get(0).get("bk:barcode"));
        assertEquals("1350002", results.get(1).get("bk:barcode"));
        assertEquals("1350001", results.get(2).get("bk:barcode"));
        assertEquals("0000", results.get(3).get("bk:barcode"));
    }

    public void testFromClause() throws Exception {
        SearchEngineBackend backend = getBackend();
        backend.index(SharedTestDataBuilder.makeAboutLifeAggregated());
        assertEquals(1, backend.searchQuery(composeQuery("SELECT * FROM Book"),
                0, 100).getTotalHits());
        // Book is declared to extend Folder
        assertEquals(1, backend.searchQuery(
                composeQuery("SELECT * FROM Folder"), 0, 100).getTotalHits());

        // XXX: fixme ?
        @SuppressWarnings("unused")
        ResultSet dummy = backend.searchQuery(
                composeQuery("SELECT * FROM Unknown"), 0, 1);
        fail("Expected a QueryException");

        // FROM and WHERE
        assertEquals(1, backend.searchQuery(
                composeQuery("SELECT * FROM Folder WHERE bk:barcode = '0000'"),
                0, 100).getTotalHits());
        assertEquals(0, backend.searchQuery(
                composeQuery("SELECT * FROM Folder WHERE bk:barcode = '0001'"),
                0, 100).getTotalHits());
    }

    public void testClear() throws Exception {
        SearchEngineBackend backend = getBackend();
        backend.index(SharedTestDataBuilder.makeAboutLifeAggregated());
        // avoid false positives
        assertEquals(1, backend.searchQuery(
                composeQuery("SELECT * FROM Document"), 0, 100).getTotalHits());

        backend.clear();
        assertEquals(0, backend.searchQuery(
                composeQuery("SELECT * FROM Document"), 0, 100).getTotalHits());
    }

}
