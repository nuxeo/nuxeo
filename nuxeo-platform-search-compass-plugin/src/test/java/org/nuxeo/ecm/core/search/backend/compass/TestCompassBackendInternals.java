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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.search.backend.compass;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.compass.core.CompassHits;
import org.compass.core.CompassQuery;
import org.compass.core.CompassSession;
import org.compass.core.CompassTransaction;
import org.compass.core.Property;
import org.compass.core.Resource;
import org.compass.core.CompassQueryBuilder.CompassBooleanQueryBuilder;
import org.compass.core.lucene.util.LuceneHelper;
import org.nuxeo.common.utils.SerializableHelper;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.query.sql.SQLQueryParser;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResource;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.factory.BuiltinDocumentFields;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.impl.ResolvedResourceImpl;
import org.nuxeo.ecm.core.search.api.backend.security.SecurityFiltering;
import org.nuxeo.ecm.core.search.api.client.SearchException;
import org.nuxeo.ecm.core.search.api.client.query.ComposedNXQuery;
import org.nuxeo.ecm.core.search.api.client.query.impl.ComposedNXQueryImpl;
import org.nuxeo.ecm.core.search.api.client.query.impl.NativeQueryImpl;
import org.nuxeo.ecm.core.search.api.client.query.impl.NativeQueryStringImpl;
import org.nuxeo.ecm.core.search.api.client.query.impl.SearchPrincipalImpl;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultItem;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceDescriptor;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.ResourceType;
import org.nuxeo.ecm.core.search.backend.compass.lucene.MatchBeforeQuery;
import org.nuxeo.ecm.core.search.backend.compass.lucene.ProofConceptQuery;
import org.nuxeo.ecm.core.search.backend.testing.SharedTestDataBuilder;

public class TestCompassBackendInternals extends TestCase {

    IntrospectableCompassBackend backend;

    Map<String, FakeIndexableResourceDataDescriptor> dataConfs;

    @Override
    public void setUp() {
        backend = new IntrospectableCompassBackend("/testcompass.cfg.xml");
        dataConfs = backend.getSearchService().dataConfs;
        dataConfs.put("dc:title",
                new FakeIndexableResourceDataDescriptor("dc:title", "standard", "Text", false, false));
        dataConfs.put("join_id",
                new FakeIndexableResourceDataDescriptor("join_id", null, "Keyword", false, false));
        dataConfs.put("bk:barcode",
                new FakeIndexableResourceDataDescriptor("bk:barcode", "lowerWhitespace", "Text", false, false));
        dataConfs.put("bk:tags",
                new FakeIndexableResourceDataDescriptor("bk:tags", null, "Keyword", true, false));
        dataConfs.put("bk:theme",
                new FakeIndexableResourceDataDescriptor("bk:theme", null, "Path", false, false));
        dataConfs.put("a:complex:title",
                new FakeIndexableResourceDataDescriptor("a:complex:title", null, "Keyword", true, false));
        dataConfs.put("a:complex:author",
                new FakeIndexableResourceDataDescriptor("a:complex:author", null, "Keyword", true, false));
        dataConfs.put("a:nolist:author",
                new FakeIndexableResourceDataDescriptor("a:nolist:author", null, "Keyword", false, false));
        dataConfs.put("a:nolist:title",
                new FakeIndexableResourceDataDescriptor("a:nolist:title", null, "Keyword", false, false));
    }

    private static ComposedNXQuery composeQuery(String query) {
        SQLQuery nxqlQuery = SQLQueryParser.parse(query);
        return new ComposedNXQueryImpl(nxqlQuery);
    }


    /**
     * Tests instantiation of compass session, perform the simplest "Lucene
     * Direct" query on it.
     *
     * @throws Exception
     */
    public void testCompassConfig() throws Exception {
        CompassSession session = backend.openSession();

        Resource r = session.createResource("nxdoc");
        Property id = session.createProperty("nxdoc_id", "the_id",
                Property.Store.YES, Property.Index.UN_TOKENIZED);

        try {
            CompassTransaction tx = session.beginTransaction();
            r.addProperty(id);
            session.save(r);
            tx.commit();


            tx = session.beginTransaction();
            TermQuery lQuery = new TermQuery(new Term("nxdoc_id", "the_id"));
            CompassQuery cQuery = LuceneHelper.createCompassQuery(session,
                    lQuery);
            CompassHits hits = cQuery.hits();
            tx.commit();
            assertEquals(1, hits.length());
        } finally {
            session.close();
        }
    }

    public void testBuildResourceSingle() throws Exception {
        CompassSession session = backend.openSession();
        try {
            ResolvedResource iResource = SharedTestDataBuilder
                .aboutLifeIndexableBookSchemaResource();

            Resource r = IntrospectableCompassBackend.buildResource(session, iResource,
                    SharedTestDataBuilder.aboutLifeCommon(), null, null,
                    SharedTestDataBuilder.makeAboutLifeACP());

            assertEquals("bk_about_life", r.getIdProperty().getStringValue());

            Property prop = r.getProperty("dc:title");
//            assertEquals(prop.getStringValue(), "About Life");
//            assertTrue(prop.isIndexed());
//            assertTrue(prop.isTokenized());

            prop = r.getProperty("bk:barcode");
            assertEquals("0000", prop.getStringValue());
            assertTrue(prop.isIndexed());
            // actually the mappings file configuration takes over
            // and we have testLowerWhiteSpace that wants the field to
            // be tokenized...
            // TODO either test lower white space analyzer on another field
            // or sync SharedTestDataBuilder with the mappings file.
            //assertFalse(prop.isTokenized());

            prop = r.getProperty("bk:category");
            assertFalse(prop.isTokenized());

            prop = r.getProperty("bk:contents");
            assertTrue(prop.isIndexed());
            assertFalse(prop.isStored());

            prop = r.getProperty("bk:abstract");
            assertFalse(prop.isIndexed());
            assertTrue(prop.isStored());

            prop = r.getProperty("bk:pages");
            assertTrue(prop.isIndexed());
            assertTrue(prop.isStored());
            assertEquals(437L, prop.getObjectValue());

            assertEquals(Arrays.asList("some", "some/path"),
                    ResourceHelper.getListProperty(r, BuiltinDocumentFields.FIELD_DOC_PATH));

            assertEquals(Arrays.asList("philosophy", "people"),
                    ResourceHelper.getListProperty(r, "bk:tags"));

            String sepPerm = SecurityFiltering.SEPARATOR
                + SecurityConstants.BROWSE;
            assertEquals(Arrays.asList(
                    "+dupont" + sepPerm, "-hugo" + sepPerm, "+authors" + sepPerm,
                    "-sales" + sepPerm, "+employees" + sepPerm
                    ),
                    ResourceHelper.getListProperty(r, BuiltinDocumentFields.FIELD_ACP_INDEXED));

            prop = r.getProperty(
                    BuiltinDocumentFields.FIELD_DOC_LIFE_CYCLE);
            assertTrue(prop.isStored());
            assertTrue(prop.isIndexed());
            assertEquals("project", prop.getStringValue());

            // Same thing with a join id
            r = IntrospectableCompassBackend.buildResource(session, iResource,
                    SharedTestDataBuilder.aboutLifeCommon(),
                    "join_id", "0101", null);
            prop = r.getProperty("join_id");
            assertEquals("0101", prop.getStringValue());
            assertFalse(prop.isTokenized());
            assertTrue(prop.isStored());

        } finally {
            session.close();
        }
    }

    public void testBuildResultItem() throws Exception {
        CompassSession session = backend.openSession();
        try {
            Resource r = session.createResource("nxdoc");
            Property p = session.createProperty("nxdoc_id", "docid",
                    Property.Store.YES, Property.Index.UN_TOKENIZED);
            r.addProperty(p);
            p = session.createProperty("dc:title", "About Life",
                    Property.Store.YES, Property.Index.UN_TOKENIZED);
            r.addProperty(p);
            p = session.createProperty("bk:tags", Util.EMPTY_MARKER,
                    Property.Store.YES, Property.Index.UN_TOKENIZED);
            r.addProperty(p);
            p = session.createProperty("bk:theme", "arts/music/haydn",
                    Property.Store.YES, Property.Index.UN_TOKENIZED);
            r.addProperty(p);
            p = session.createProperty(
                    BuiltinDocumentFields.FIELD_DOC_LIFE_CYCLE,
                    "project",
                    Property.Store.YES, Property.Index.UN_TOKENIZED);
            r.addProperty(p);

            ResultItem item = backend.buildResultItem(r);

            // We have all properties, save the id (internal to the backend)
            assertEquals("About Life", item.get("dc:title"));
            assertEquals("project", item.get(BuiltinDocumentFields.FIELD_DOC_LIFE_CYCLE));
            assertEquals("arts/music/haydn", item.get("bk:theme"));
            assertEquals(Collections.EMPTY_LIST, item.get("bk:tags"));
            assertFalse(item.containsKey("nxdoc_id"));

        } finally {
            session.close();
        }
    }

    // TODO test this also in SearchBackendTestCase
    public void testBuildResultItemComplexProp() throws Exception {
        CompassSession session = backend.openSession();
        try {
            Resource r = session.createResource("nxdoc");
            Property p = session.createProperty("nxdoc_id", "docid",
                    Property.Store.YES, Property.Index.UN_TOKENIZED);
            r.addProperty(p);

            p = session.createProperty("a:complex:title", "About Life",
                    Property.Store.YES, Property.Index.UN_TOKENIZED);
            r.addProperty(p);
            p = session.createProperty("a:complex:author", "gr",
                    Property.Store.YES, Property.Index.UN_TOKENIZED);
            r.addProperty(p);

            ResultItem item;
            item = backend.buildResultItem(r);

            assertTrue(item.get("a:complex") instanceof List);
            List<Map<String, Object>> l;
            l = (List<Map<String, Object>>) item.get("a:complex");
            assertEquals(1, l.size());

            Map<String, Object> oc = l.get(0);
            assertEquals("About Life", oc.get("title"));
            assertEquals("gr", oc.get("author"));

            // let's add a second entry
            p = session.createProperty("a:complex:title", "Another",
                    Property.Store.YES, Property.Index.UN_TOKENIZED);
            r.addProperty(p);
            p = session.createProperty("a:complex:author", "ja",
                    Property.Store.YES, Property.Index.UN_TOKENIZED);
            r.addProperty(p);
            item = backend.buildResultItem(r);

            l = (List<Map<String, Object>>) item.get("a:complex");

            assertEquals(2, l.size());
            oc = l.get(1);
            assertEquals("Another", oc.get("title"));
            assertEquals("ja", oc.get("author"));

            // now let's play with null with two new entries
            p = session.createProperty("a:complex:title", "White",
                    Property.Store.YES, Property.Index.UN_TOKENIZED);
            r.addProperty(p);
            p = session.createProperty("a:complex:author", Util.NULL_MARKER,
                    Property.Store.YES, Property.Index.UN_TOKENIZED);
            r.addProperty(p);
            p = session.createProperty("a:complex:title", "Invisibles",
                    Property.Store.YES, Property.Index.UN_TOKENIZED);
            r.addProperty(p);
            p = session.createProperty("a:complex:author", "gm",
                    Property.Store.YES, Property.Index.UN_TOKENIZED);
            r.addProperty(p);
            item = backend.buildResultItem(r);

            l = (List<Map<String, Object>>) item.get("a:complex");
            assertEquals(4, l.size());

            oc = l.get(2);
            assertEquals("White", oc.get("title"));
            assertNull(oc.get("author"));

            oc = l.get(3);
            assertEquals("Invisibles", oc.get("title"));
            assertEquals("gm", oc.get("author"));

        } finally {
            session.close();
        }
    }

    public void testBuildResultItemComplexPropNolist() throws Exception {
        CompassSession session = backend.openSession();
        try {
            Resource r = session.createResource("nxdoc");
            Property p = session.createProperty("nxdoc_id", "docid",
                    Property.Store.YES, Property.Index.UN_TOKENIZED);
            r.addProperty(p);

            p = session.createProperty("a:nolist:title", "About Life",
                    Property.Store.YES, Property.Index.UN_TOKENIZED);
            r.addProperty(p);
            p = session.createProperty("a:nolist:author", "gr",
                    Property.Store.YES, Property.Index.UN_TOKENIZED);
            r.addProperty(p);

            ResultItem item = backend.buildResultItem(r);

            assertTrue(item.get("a:nolist") instanceof Map);
            Map<String, Object> m = (Map<String, Object>) item.get("a:nolist");

            assertEquals("About Life", m.get("title"));
            assertEquals("gr", m.get("author"));

            // Same thing with null
            r = session.createResource("nxdoc");
            p = session.createProperty("nxdoc_id", "docid2",
                    Property.Store.YES, Property.Index.UN_TOKENIZED);
            r.addProperty(p);

            p = session.createProperty("a:nolist:title", "About Life",
                    Property.Store.YES, Property.Index.UN_TOKENIZED);
            r.addProperty(p);
            p = session.createProperty("a:nolist:author", Util.NULL_MARKER,
                    Property.Store.YES, Property.Index.UN_TOKENIZED);
            r.addProperty(p);
            item = backend.buildResultItem(r);

            assertTrue(item.get("a:nolist") instanceof Map);
            m = (Map<String, Object>) item.get("a:nolist");

            assertEquals("About Life", m.get("title"));
            assertNull(m.get("author"));
            assertTrue(m.containsKey("author"));

        } finally {
            session.close();
        }
    }

    // Check that it works in out of order cases (these are actually
    // more common)
    public void testBuildResultItemComplexPropOutorder() throws Exception {
        CompassSession session = backend.openSession();
        try {
            Resource r = session.createResource("nxdoc");
            Property p = session.createProperty("nxdoc_id", "docid",
                    Property.Store.YES, Property.Index.UN_TOKENIZED);
            r.addProperty(p);

            p = session.createProperty("a:complex:title", "About Life",
                    Property.Store.YES, Property.Index.UN_TOKENIZED);
            r.addProperty(p);
            p = session.createProperty("a:complex:title", "Another",
                    Property.Store.YES, Property.Index.UN_TOKENIZED);
            r.addProperty(p);
            p = session.createProperty("a:complex:title", "White",
                    Property.Store.YES, Property.Index.UN_TOKENIZED);
            r.addProperty(p);
            p = session.createProperty("a:complex:title", "Invisibles",
                    Property.Store.YES, Property.Index.UN_TOKENIZED);
            r.addProperty(p);

            p = session.createProperty("a:complex:author", "gr",
                    Property.Store.YES, Property.Index.UN_TOKENIZED);
            r.addProperty(p);
            p = session.createProperty("a:complex:author", "ja",
                    Property.Store.YES, Property.Index.UN_TOKENIZED);
            r.addProperty(p);
            p = session.createProperty("a:complex:author", Util.NULL_MARKER,
                    Property.Store.YES, Property.Index.UN_TOKENIZED);
            r.addProperty(p);
            p = session.createProperty("a:complex:author", "gm",
                    Property.Store.YES, Property.Index.UN_TOKENIZED);
            r.addProperty(p);

            ResultItem item = backend.buildResultItem(r);

            assertTrue(item.get("a:complex") instanceof List);
            List<Map<String, Object>> l;
            l = (List<Map<String, Object>>) item.get("a:complex");
            assertEquals(4, l.size());

            Map<String, Object> oc = l.get(0);
            assertEquals("About Life", oc.get("title"));
            assertEquals("gr", oc.get("author"));

            oc = l.get(1);
            assertEquals("Another", oc.get("title"));
            assertEquals("ja", oc.get("author"));

            oc = l.get(2);
            assertEquals("White", oc.get("title"));
            assertNull(oc.get("author"));

            oc = l.get(3);
            assertEquals("Invisibles", oc.get("title"));
            assertEquals("gm", oc.get("author"));

        } finally {
            session.close();
        }
    }

    public void testBuildResultItemNullBuiltin() throws Exception {
        CompassSession session = backend.openSession();
        try {
            Resource r = session.createResource("nxdoc");
            Property p = session.createProperty("nxdoc_id", "docid",
                    Property.Store.YES, Property.Index.UN_TOKENIZED);
            r.addProperty(p);
            p = session.createProperty(
                    BuiltinDocumentFields.FIELD_DOC_LIFE_CYCLE,
                    Util.NULL_MARKER,
                    Property.Store.YES, Property.Index.UN_TOKENIZED);
            r.addProperty(p);

            ResultItem item = backend.buildResultItem(r);

            // We have all properties, save the id (internal to the backend)
            assertNull(item.get(BuiltinDocumentFields.FIELD_DOC_LIFE_CYCLE));
        } finally {
            session.close();
        }
    }

    public void testBuildResourceSerializatoin() throws Exception {
        CompassSession session = backend.openSession();
        try {
            ResolvedResource iResource = SharedTestDataBuilder
                .aboutLifeIndexableBookSchemaResource();
            Resource r = IntrospectableCompassBackend.buildResource(session, iResource,
                    SharedTestDataBuilder.aboutLifeCommon(),
                    null, null, null);
            ResultItem item = backend.buildResultItem(r);
            PathRef docRef = new PathRef("some/path");
            assertTrue(SerializableHelper.isSerializable(docRef));
            assertEquals(docRef,
                    item.get(BuiltinDocumentFields.FIELD_DOC_REF));

        } finally {
            session.close();
        }
    }

    /**
     * Compass specific counterpart to service-side testOneDoc. Purposes: search
     * on internal id and join id some traps, typical of Lucene.
     */
    public void testOneDoc() throws Exception {
        CompassSession session = backend.openSession();
        CompassTransaction tx = session.beginTransaction();
        try {
            ResolvedResource iResource = SharedTestDataBuilder
                .aboutLifeIndexableBookSchemaResource();

            Resource r = IntrospectableCompassBackend.buildResource(session, null, iResource,
                    SharedTestDataBuilder.aboutLifeCommon(),
                    "join_id",
                    "the_join", null);
            session.save(r);
            tx.commit();
            tx = session.beginTransaction();
            TermQuery lQuery = new TermQuery(new Term("nxdoc_id", "bk_about_life"));
            CompassQuery cQuery = LuceneHelper.createCompassQuery(session,
                  lQuery);
            CompassHits hits = cQuery.hits();
            assertEquals(1, hits.getLength());
            lQuery = new TermQuery(new Term("join_id", "the_join"));
            cQuery = LuceneHelper.createCompassQuery(session, lQuery);
            assertEquals(1, cQuery.hits().getLength());
        } finally {
            session.close();
        }
    }

    /**
     * Low level check that the security index works.
     */
    public void testOneDocSecurity() throws Exception {
        backend.index(SharedTestDataBuilder.makeAboutLifeAggregated());
        CompassSession session = backend.openSession();
        try {
            CompassTransaction tx = session.beginTransaction();
            Query lQuery;
            CompassQuery cQuery;
            CompassHits hits;
            String sepperm = SecurityFiltering.SEPARATOR
            + SecurityConstants.BROWSE;
            String sepperm2 = SecurityFiltering.SEPARATOR
            + SecurityConstants.READ;

            // dupont, sales checked on two perms although one is enough here.
            lQuery = new MatchBeforeQuery(BuiltinDocumentFields.FIELD_ACP_INDEXED,
                    Arrays.asList("+dupont" + sepperm, "+sales" + sepperm),
                    Arrays.asList("-dupont" + sepperm, "-sales" + sepperm));
            CompassBooleanQueryBuilder bbuilder = session.queryBuilder().bool();
            cQuery = LuceneHelper.createCompassQuery(session, lQuery);
            bbuilder.addShould(cQuery);
            lQuery = new MatchBeforeQuery(BuiltinDocumentFields.FIELD_ACP_INDEXED,
                    Arrays.asList("+dupont" + sepperm2, "+sales" + sepperm2),
                    Arrays.asList("-dupont" + sepperm2, "-sales" + sepperm2));
            cQuery = LuceneHelper.createCompassQuery(session, lQuery);
            bbuilder.addShould(cQuery);

            cQuery = bbuilder.toQuery();
            hits = cQuery.hits();
            assertEquals(1, hits.getLength());

            // smith, sales
            lQuery = new MatchBeforeQuery(BuiltinDocumentFields.FIELD_ACP_INDEXED,
                    Arrays.asList("+smith" + sepperm, "+sales" + sepperm),
                    Arrays.asList("-smith" + sepperm, "-sales" + sepperm));
            cQuery = LuceneHelper.createCompassQuery(session, lQuery);
            hits = cQuery.hits();
            assertEquals(0, hits.getLength());

            // hugo, author
            lQuery = new MatchBeforeQuery(BuiltinDocumentFields.FIELD_ACP_INDEXED,
                    Arrays.asList("+hugo" + sepperm, "+authors" + sepperm),
                    Arrays.asList("-hugo" + sepperm, "-authors" + sepperm));
            cQuery = LuceneHelper.createCompassQuery(session, lQuery);
            hits = cQuery.hits();
            assertEquals(0, hits.getLength());

            // someone nobody knows about
            lQuery = new MatchBeforeQuery(BuiltinDocumentFields.FIELD_ACP_INDEXED,
                    Arrays.asList("+nobody" + sepperm),
                    Arrays.asList("-nobody" + sepperm));
            cQuery = LuceneHelper.createCompassQuery(session, lQuery);
            hits = cQuery.hits();
            assertEquals(0, hits.getLength());

            tx.commit();
        } finally {
            session.close();
            }
    }

    /**
     * Here we check what security queries give with docs without security
     * (bug-driven test).
     */
    public void testOneDocLackingSecurity() throws Exception {
        backend.index(SharedTestDataBuilder.revelationsBunch(1)[0]);
        CompassSession session = backend.openSession();
        try {
            CompassTransaction tx = session.beginTransaction();
            Query lQuery;
            CompassQuery cQuery;
            CompassHits hits;

            // warm up
            lQuery = new TermQuery(new Term(
                    BuiltinDocumentFields.FIELD_ACP_INDEXED,
                    "dupont"));
            cQuery = LuceneHelper.createCompassQuery(session, lQuery);
            hits = cQuery.hits();
            assertEquals(0, hits.getLength());


            // dupont, sales
            lQuery = new MatchBeforeQuery(BuiltinDocumentFields.FIELD_ACP_INDEXED,
                    Arrays.asList("+dupont", "+sales"),
                    Arrays.asList("-dupont", "-sales"));
            cQuery = LuceneHelper.createCompassQuery(session, lQuery);
            hits = cQuery.hits();
            assertEquals(0, hits.getLength());
            tx.commit();

        } finally {
            session.close();
        }
    }

    public void testLowerWhitespaceAnalyzer() throws Exception {
        backend.index(SharedTestDataBuilder.makeAboutLifeAggregated2());
        ResultSet results = backend.searchQuery(composeQuery(
            "SELECT * FROM Document WHERE bk:barcode LIKE 'ext0000_1'"),
            0, 100);
        assertEquals(1, results.getTotalHits());

        results = backend.searchQuery(composeQuery(
            "SELECT * FROM Document WHERE bk:barcode LIKE 'EXT0000_1'"),
            0, 100);

        results = backend.searchQuery(composeQuery(
            "SELECT * FROM Document WHERE bk:barcode LIKE 'eXt0000_1'"),
            0, 100);

        results = backend.searchQuery(composeQuery(
        "SELECT * FROM Document WHERE bk:barcode LIKE 'ext0001'"),
    0, 100);
        assertEquals(0, results.getTotalHits());
    }


    public void testStringQuery() throws Exception {
        backend.index(SharedTestDataBuilder.makeAboutLifeAggregated());

        ResultSet results = backend.searchQuery(new NativeQueryStringImpl(
                "compass", "bk\\:barcode:0000"), 0, 100);
        assertEquals(1, results.getTotalHits());

        // wildcards
        results = backend.searchQuery(new NativeQueryStringImpl(
                "compass", "bk\\:barcode:0*"), 0, 100);
        assertEquals(1, results.getTotalHits());

        results = backend.searchQuery(new NativeQueryStringImpl(
                "compass", "bk\\:barcode:1*"), 0, 100);
        assertEquals(0, results.getTotalHits());

        // Now with security
        results = backend.searchQuery(new NativeQueryStringImpl(
                "compass", "bk\\:barcode:0000",
                new SearchPrincipalImpl("noone", new String[0], false)), 0, 100);
        assertEquals(0, results.getTotalHits());

        results = backend.searchQuery(new NativeQueryStringImpl(
                "compass", "bk\\:barcode:0000",
                new SearchPrincipalImpl("dupont", new String[0], false)), 0, 100);
        assertEquals(1, results.getTotalHits());
    }

    public void testNullQuery() throws Exception {
        //TODO move to SearchEngineBackendTestCase once
        //we can write nxql for null queries
        backend.index(SharedTestDataBuilder.makeAboutLifeAggregated());

        TermQuery lQuery = new TermQuery(
                new Term("bk:published", Util.NULL_MARKER));
        ResultSet results = backend.searchQuery(
                new NativeQueryImpl(lQuery, backend.getName()), 0, 100);

        assertEquals(1, results.getTotalHits());
        assertNull(results.get(0).get("bk:published"));
    }

    public void testEmptyListsNoFalseHit1() throws Exception {
        backend.index(SharedTestDataBuilder.makeWarPeace());
        ResultSet results = null;
        try {
            results = backend.searchQuery(composeQuery(String.format(
                        "SELECT * FROM Document WHERE bk:tags = '%s'",
                        Util.EMPTY_MARKER)),
                    0, 2);
        } catch (SearchException se) {
            fail();
        }
        assertEquals(0, results.getTotalHits());
    }

    /**
     * Leverages the compass integration to test our ProofConceptQuery (see doc of the later).
     */
    public void testProofConceptQuery() throws Exception {
        CompassSession session = backend.openSession();
        try {
            backend.index(SharedTestDataBuilder.makeAboutLifeAggregated());
            CompassTransaction tx = session.beginTransaction();
            // Field is: contents should not be stored, analyzed as
            // ["content", "should", "stored"]

            ProofConceptQuery lQuery = new ProofConceptQuery(
                    new Term("bk:contents", "should"), 1);
            CompassQuery cQuery = LuceneHelper.createCompassQuery(session,
                    lQuery);
            CompassHits hits = cQuery.hits();
            assertEquals(1, hits.getLength());

            lQuery = new ProofConceptQuery(
                    new Term("bk:contents", "stored"), 1);
            cQuery = LuceneHelper.createCompassQuery(session, lQuery);
            hits = cQuery.hits();
            assertEquals(0, hits.getLength());

            lQuery = new ProofConceptQuery(
                    new Term("bk:contents", "stored"), 2);
            cQuery = LuceneHelper.createCompassQuery(session, lQuery);
            hits = cQuery.hits();
            assertEquals(1, hits.getLength());

            lQuery = new ProofConceptQuery(new Term(
                    "bk:contents", "stored"), 3);
            cQuery = LuceneHelper.createCompassQuery(session, lQuery);
            hits = cQuery.hits();
            assertEquals(0, hits.getLength());

            tx.commit();
        } finally {
            session.close();
        }
    }

    private static class FakeResolvedResource extends ResolvedResourceImpl {

        private static final long serialVersionUID = 1L;

        private final IndexableResourceConf conf;

        FakeResolvedResource(IndexableResourceConf conf) {
            this.conf = conf;
        }

        @Override
        public IndexableResourceConf getConfiguration() {
            return conf;
        }

    }

    public void testGetAlias() throws Exception {
        ResolvedResource rr = new FakeResolvedResource(
                new IndexableResourceDescriptor(
                       "comment", "rc", false, null, null, "relations"));
        assertEquals("nxrel-comment", CompassBackend.getAlias(rr));

        rr = new FakeResolvedResource(
                new IndexableResourceDescriptor(
                       "comment", "cm", false, null, null,
                       ResourceType.SCHEMA));
        assertEquals("nxdoc", CompassBackend.getAlias(rr));

        rr = new FakeResolvedResource(
                new IndexableResourceDescriptor(
                        "comment", "rc", false, null, null,
                        "exoticResource"));
        assertEquals("comment", CompassBackend.getAlias(rr));
    }

}
