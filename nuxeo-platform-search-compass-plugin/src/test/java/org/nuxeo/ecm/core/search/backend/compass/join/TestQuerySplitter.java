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
 * $Id$
 */

package org.nuxeo.ecm.core.search.backend.compass.join;

import java.util.Arrays;

import org.nuxeo.ecm.core.query.sql.SQLQueryParser;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.search.api.client.common.SearchServiceDelegate;
import org.nuxeo.ecm.core.search.api.client.query.QueryException;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceDataConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.ResourceType;
import org.nuxeo.ecm.core.search.api.internals.SearchServiceInternals;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 *
 */
public class TestQuerySplitter extends NXRuntimeTestCase {

    protected SearchServiceInternals service;

    protected String ENGINE_NAME;

    private static class IntrospectableSplitter extends QuerySplitter {

        IntrospectableSplitter(SQLQuery query) {
            super(query);
        }

        @Override
        public void extractSubQuery() throws QueryException {
            super.extractSubQuery();
        }

        public SubQuery getSubQuery(int i) {
            return subQueries.get(i);
        }

        public String[] getJoinFor(String resourceName) {
            return joins.get(resourceName);
        }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployContrib("org.nuxeo.runtime", "OSGI-INF/EventService.xml");
        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "CoreService.xml");
        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "SecurityService.xml");
        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "RepositoryService.xml");
        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "test-CoreExtensions.xml");
        deployContrib("org.nuxeo.ecm.platform.search.test",
                "nxsearch-backendtest-types-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "DemoRepository.xml");
        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "LifeCycleService.xml");
        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "LifeCycleServiceExtensions.xml");
        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "CoreEventListenerService.xml");
        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "PlatformService.xml");
        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "DefaultPlatform.xml");

        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "nxtransform-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "nxtransform-platform-contrib.xml");

        deployContrib("org.nuxeo.ecm.platform.search.test",
                "nxsearch-backendtest-framework.xml");
        service = (SearchServiceInternals)
            SearchServiceDelegate.getRemoteSearchService();
        assertNotNull(service);

        deployContrib("org.nuxeo.ecm.platform.search.test",
                "nxsearch-backendtest-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "nxsearch-jointest-contrib.xml");

        checkConf();
    }

    /**
     * shortcut to include parsing of incoming query string *
     * @return the split query.
     * @throws QueryException
     * */
    private SplitQuery split(String query) throws QueryException {
        return makeSplitter(query).split();
    }

    private static SQLQuery parse(String query) {
        return SQLQueryParser.parse(query);
    }

    private static IntrospectableSplitter makeSplitter(String query) {
        return new IntrospectableSplitter(SQLQueryParser.parse(query));
    }

    public void checkConf() {
        IndexableResourceDataConf conf;
        conf = service.getIndexableDataConfFor("bk:frenchtitle");
        assertNotNull(conf);
        assertEquals("frenchtitle", conf.getIndexingName());

        conf = service.getIndexableDataConfFor("ecm:id");
        assertNotNull(conf);
        assertEquals("id", conf.getIndexingName());

        assertEquals("other",
                service.getIndexableResourceConfByName("join", false).getType());

        IndexableResourceConf rConf;
        rConf = service.getIndexableResourceConfByPrefix("bk", false);
        assertNotNull(rConf);
        rConf = service.getIndexableResourceConfByPrefix("ecm", false);
        assertNotNull(rConf);
    }

    //
    // TODO Disabled tests: work individually but not one after the other
    // probably a tearDown problem
    //

    public void xtestPureDocQueries() throws Exception {
        for (String qs: Arrays.asList(
                "SELECT * FROM Document",
                "SELECT * FROM Document WHERE bk:barcode='1212'",
                "SELECT * FROM Book WHERE bk:barcode='1212'",
                "SELECT * FROM Document WHERE ecm:id='adasd'")) {
            SplitQuery split = split(qs);
            assertNotNull(split);
            assertFalse(split.isJoinQuery());
            assertEquals(parse(qs), split.getMainQuery());
        }
    }

    public void testSimpleMixedQuery() throws Exception {
        for (String query : Arrays.asList(
                "SELECT * FROM Document WHERE bk:barcode='000' "
                + "AND ecm:id=jn:oid AND jn:foo='bar'",

                "SELECT * FROM Document WHERE jn:foo='bar' "
                + "AND ecm:id=jn:oid AND bk:barcode='000'")) {
            SplitQuery split = split(query);

            assertNotNull(split);
            assertTrue(split.isJoinQuery());
            assertEquals(parse("SELECT * FROM Document WHERE bk:barcode='000'"),
                    split.getMainQuery());

            assertEquals(1, split.getSubQueries().size());
            SubQuery subQuery;
            subQuery = split.getSubQueries().get(0);

            // GR Document is a hack
            assertEquals(parse("SELECT * FROM Document WHERE jn:foo='bar'"),
                    subQuery.getQuery());
            assertEquals("ecm:id", subQuery.getJoinFieldInMain());
            assertEquals("jn:oid", subQuery.getJoinFieldInSub());
        }
    }

    // Join with two auxiliary resources of the same type
    public void testDoubleMixedQuery() throws Exception {
        for (String query : Arrays.asList(
                "SELECT * FROM Document WHERE bk:barcode='000' "
                + "AND ecm:id=jn:oid AND jn:foo='bar'"
                + "AND bk:frenchtitle=jn2:title AND jn2:foo='pa'"
                )) {
            SplitQuery split = split(query);

            assertNotNull(split);
            assertTrue(split.isJoinQuery());
            assertEquals(parse("SELECT * FROM Document WHERE bk:barcode='000'"),
                    split.getMainQuery());

            assertEquals(2, split.getSubQueries().size());
            SubQuery subQuery;

            subQuery = split.getSubQueries().get(0);
            // GR Document is a hack
            assertEquals(parse("SELECT * FROM Document WHERE jn2:foo='pa'"),
                    subQuery.getQuery());
            assertEquals("bk:frenchtitle", subQuery.getJoinFieldInMain());
            assertEquals("jn2:title", subQuery.getJoinFieldInSub());

            subQuery = split.getSubQueries().get(1);
            // GR Document is a hack
            assertEquals(parse("SELECT * FROM Document WHERE jn:foo='bar'"),
                    subQuery.getQuery());
            assertEquals("ecm:id", subQuery.getJoinFieldInMain());
            assertEquals("jn:oid", subQuery.getJoinFieldInSub());
        }
    }

    // Join with two auxiliary resources of different types
    public void testDoubleMixedQueryHetero() throws Exception {
        for (String query : Arrays.asList(
                "SELECT * FROM Document WHERE bk:barcode='000' "
                + "AND ecm:id=jn:oid AND jn:foo='bar'"
                + "AND bk:frenchtitle=jnm:title AND jnm:foo='pa'"
                )) {
            SplitQuery split = split(query);

            assertNotNull(split);
            assertTrue(split.isJoinQuery());
            assertEquals(parse("SELECT * FROM Document WHERE bk:barcode='000'"),
                    split.getMainQuery());

            assertEquals(2, split.getSubQueries().size());
            SubQuery subQuery;

            subQuery = split.getSubQueries().get(0);
            // GR Document is a hack
            assertEquals(parse("SELECT * FROM Document WHERE jnm:foo='pa'"),
                    subQuery.getQuery());
            assertEquals("bk:frenchtitle", subQuery.getJoinFieldInMain());
            assertEquals("jnm:title", subQuery.getJoinFieldInSub());

            subQuery = split.getSubQueries().get(1);
            // GR Document is a hack
            assertEquals(parse("SELECT * FROM Document WHERE jn:foo='bar'"),
                    subQuery.getQuery());
            assertEquals("ecm:id", subQuery.getJoinFieldInMain());
            assertEquals("jn:oid", subQuery.getJoinFieldInSub());
        }
    }

    // Join with three subqueries of mixed types
    public void testTripleMixedQuery() throws Exception {
        for (String query : Arrays.asList(
                "SELECT * FROM Document WHERE bk:barcode='000' "
                + "AND bk:category=jn2:title " +
                        "AND jn2:foo='bla'"
                + "AND ecm:id=jn:oid " +
                        "AND jn:foo='bar'"
                + "AND bk:frenchtitle=jnm:title AND jnm:foo='pa'",
            "SELECT * FROM Document WHERE bk:barcode='000' "
                + "AND ecm:id=jn:oid " + // swapped line
                    "AND jn2:foo='bla'"
                + "AND bk:category=jn2:title " + // with this one
                    "AND jn:foo='bar'"
            + "AND bk:frenchtitle=jnm:title AND jnm:foo='pa'"
            )) {
            SplitQuery split = split(query);

            assertNotNull(split);
            assertTrue(split.isJoinQuery());
            assertEquals(parse("SELECT * FROM Document WHERE bk:barcode='000'"),
                    split.getMainQuery());

            assertEquals(3, split.getSubQueries().size());
            SubQuery subQuery;

            subQuery = split.getSubQueries().get(0);
            // GR Document is a hack
            assertEquals(parse("SELECT * FROM Document WHERE jnm:foo='pa'"),
                    subQuery.getQuery());
            assertEquals("bk:frenchtitle", subQuery.getJoinFieldInMain());
            assertEquals("jnm:title", subQuery.getJoinFieldInSub());

            subQuery = split.getSubQueries().get(1);
            // GR Document is a hack
            assertEquals(parse("SELECT * FROM Document WHERE jn:foo='bar'"),
                    subQuery.getQuery());
            assertEquals("ecm:id", subQuery.getJoinFieldInMain());
            assertEquals("jn:oid", subQuery.getJoinFieldInSub());

            subQuery = split.getSubQueries().get(2);
            // GR Document is a hack
            assertEquals(parse("SELECT * FROM Document WHERE jn2:foo='bla'"),
                    subQuery.getQuery());
            assertEquals("bk:category", subQuery.getJoinFieldInMain());
            assertEquals("jn2:title", subQuery.getJoinFieldInSub());
        }
    }

    // Internal methods test
    public void testExtractSubQuery() throws Exception {
        SubQuery subQuery;
        for (String query : Arrays.asList(
                "SELECT * FROM Document WHERE bk:barcode='0000' " +
                "AND ecm:id=jn:oid AND jn:foo='bar'",

                "SELECT * FROM Document WHERE bk:barcode='0000' " +
                "AND jn:oid=ecm:id AND jn:foo='bar'")) {
            IntrospectableSplitter splitter = makeSplitter(query);
            splitter.extractSubQuery();
            subQuery = splitter.getSubQuery(0);

            assertEquals("jn:foo = 'bar'",
                    subQuery.getQuery().getWhereClause().toString());
            assertEquals("other", subQuery.getResourceType());
            assertEquals("join", subQuery.getResourceName());
            assertEquals("ecm:id", splitter.getJoinFor("join")[0]);
            assertEquals("jn:oid", splitter.getJoinFor("join")[1]);
        }

        for (String query : Arrays.asList(
                "SELECT * FROM Document WHERE bk:barcode='0000' " +
                "AND jn:oid=bk:barcode AND jn:foo='bar'",

                "SELECT * FROM Document WHERE bk:barcode='0000' " +
                "AND bk:barcode=jn:oid AND jn:foo='bar'")) {
            IntrospectableSplitter splitter = makeSplitter(query);
            splitter.extractSubQuery();
            subQuery = splitter.getSubQuery(0);

            assertEquals("other", subQuery.getResourceType());
            assertEquals("bk:barcode", splitter.getJoinFor("join")[0]);
            assertEquals("jn:oid", splitter.getJoinFor("join")[1]);

            splitter.extractSubQuery();
            subQuery = splitter.getSubQuery(1);
            assertEquals("bk:barcode = '0000'",
                    subQuery.getQuery().getWhereClause().toString());
        }
    }

    public void testExtractSubQueryDocPart() throws Exception {
        for (String query : Arrays.asList(
                "SELECT * FROM Document WHERE bk:barcode='0000'",
                "SELECT * FROM Document WHERE jn:foo='bar' AND jn:oid=ecm:id "
                + "AND bk:barcode='0000'")) {
            IntrospectableSplitter splitter = makeSplitter(query);
            splitter.extractSubQuery();
            SubQuery subQuery = splitter.getSubQuery(0);

            assertEquals(ResourceType.SCHEMA, subQuery.getResourceType());
        }
    }

}
