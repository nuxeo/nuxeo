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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.compass.core.CompassQuery;
import org.nuxeo.ecm.core.query.sql.SQLQueryParser;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.factory.BuiltinDocumentFields;
import org.nuxeo.ecm.core.search.api.client.query.QueryException;
import org.nuxeo.ecm.core.search.api.client.query.SearchPrincipal;
import org.nuxeo.ecm.core.search.api.client.query.impl.SearchPrincipalImpl;

public class TestQueryConverter extends TestCase {

    QueryConverter converter;

    Map<String, FakeIndexableResourceDataDescriptor> dataConfs;

    Map<String, Set<String>> facetsToTypes;

    Map<String, Set<String>> facetsCollectionsToTypes;

    Map<String, Set<String>> typesInheritance;

    @Override
    public void setUp() {
        IntrospectableCompassBackend backend
                = new IntrospectableCompassBackend("/testcompass.cfg.xml");
        FakeSearchService service = backend.getSearchService();
        dataConfs = service.dataConfs;
        facetsToTypes = service.facetsToTypes;
        facetsCollectionsToTypes = service.facetsCollectionToTypes;
        typesInheritance = service.typeInheritance;

        dataConfs.put("kw1", new FakeIndexableResourceDataDescriptor("kw1",
                null, "Keyword", false, false));
        dataConfs.put("kw2", new FakeIndexableResourceDataDescriptor("kw2",
                null, "Keyword", false, false));
        dataConfs.put("kw3", new FakeIndexableResourceDataDescriptor("kw3",
                null, "Keyword", false, false));
        dataConfs.put("kw4", new FakeIndexableResourceDataDescriptor("kw4",
                "lowerWhitespace", "Text", false, false));
        dataConfs.put("path", new FakeIndexableResourceDataDescriptor("path",
                null, "Path", false, false));
        dataConfs.put("frenchtitle",
                new FakeIndexableResourceDataDescriptor("frenchtitle",
                        "french", "text", false, false));
        dataConfs.put("title",
                new FakeIndexableResourceDataDescriptor("title",
                        "default", "text", false, true));
        dataConfs.put("bk:published",
                new FakeIndexableResourceDataDescriptor("bk:published",
                        null, "date", false, false));
        dataConfs.put("bk:pages",
                new FakeIndexableResourceDataDescriptor("bk:pages",
                        null, "int", false, false));
        dataConfs.put("bk:available",
                new FakeIndexableResourceDataDescriptor("bk:available",
                        null, "boolean", false, false));

        converter = new QueryConverter(backend.openSession(),
                backend.getSearchService());
    }

    private String convertToStringQuery(String nxqlQuery)
            throws QueryException {
        SQLQuery parsed = SQLQueryParser.parse(nxqlQuery);
        return converter.toCompassQuery(parsed, null).toString();
    }

    public void testlowerWhitespaceAnalyzer() throws QueryException {
        assertEquals(
                "kw4:ext0000",
                convertToStringQuery("SELECT * FROM Document WHERE kw4='ext0000'")
                );
    }

    public void testBooleanProperty() throws QueryException {
        assertEquals(
                "bk:available:true",
                convertToStringQuery("SELECT * FROM Document WHERE bk:available=1")
                );
        assertEquals(
                "bk:available:false",
                convertToStringQuery("SELECT * FROM Document WHERE bk:available=0")
                );
    }

    public void testBooleanQuery() throws QueryException {
        assertEquals(
                "+kw1:vie +kw2:mechante",
                convertToStringQuery("SELECT * FROM Document WHERE kw1='vie' AND kw2='mechante'"));
        assertEquals(
                "kw1:vie kw2:mechante",
                convertToStringQuery("SELECT * FROM Document WHERE kw1='vie' OR kw2='mechante'"));
        assertEquals(
                "kw1:vie (+kw2:mechante +kw3:novel)",
                convertToStringQuery("SELECT * FROM Document WHERE kw1='vie' OR (kw2='mechante' AND  kw3='novel')"));
        assertEquals(
                "+kw1:vie -kw3:novel",
                convertToStringQuery("SELECT * FROM Document WHERE kw1='vie' AND NOT kw3='novel'"));
        // TODO test that these two queries actually do work (with a matchall to
        // help, maybe)
        assertEquals(
                "(+MatchAllDocsQuery -kw1:vie) kw2:mechante",
                convertToStringQuery("SELECT * FROM Document WHERE NOT kw1='vie' OR kw2='mechante'"));
        assertEquals(
                "kw1:vie (+MatchAllDocsQuery -kw2:mechante)",
                convertToStringQuery("SELECT * FROM Document WHERE kw1='vie' OR NOT kw2='mechante'"));
        assertEquals(
                "+MatchAllDocsQuery -(+kw1:vie +kw2:mechante)",
                convertToStringQuery("SELECT * FROM Document WHERE NOT kw1='vie' OR NOT kw2='mechante'"));
    }

    public void testInQueries() throws QueryException {
        assertEquals(
                "kw1:foo kw1:bar",
                convertToStringQuery("SELECT * FROM Document WHERE kw1 IN ('foo', 'bar')"));
        // TODO The following syntax is not supported by the parser
        // assertEquals("kw2:foo",
        // convertToStringQuery("SELECT * FROM Document WHERE 'foo' IN kw2"));
    }

    public void testPathQueries() throws QueryException {
        assertEquals(
                "path:some/path",
                convertToStringQuery("SELECT * FROM Document WHERE path STARTSWITH 'some/path'"));
        assertEquals(
                "+kw1:foo",
                convertToStringQuery("SELECT * FROM Document WHERE path STARTSWITH '/' AND kw1 = 'foo'"));
        assertEquals(
                "MatchAllDocsQuery",
                convertToStringQuery("SELECT * FROM Document WHERE path STARTSWITH '/'"));
        assertEquals(
                "MatchAllDocsQuery",
                convertToStringQuery("SELECT * FROM Document WHERE path STARTSWITH ''"));
        // actually tests the transmission of null
        assertEquals(
                "MatchAllDocsQuery",
                convertToStringQuery("SELECT * FROM Document WHERE path STARTSWITH '' AND path STARTSWITH '/'"));
    }

    /* Disabled for Hudson TODO */
    public void xtestTextFieldQuery() throws QueryException {
        // NB: Lucene french analyzer stems "méchante" as "mech"
        assertEquals("frenchtitle:\"mech vie\"",
                convertToStringQuery(
                        "SELECT * FROM Document WHERE frenchtitle = 'la méchante vie'"));

        assertEquals("frenchtitle:mech frenchtitle:vie",
                convertToStringQuery(
                        "SELECT * FROM Document WHERE frenchtitle LIKE 'la méchante vie'"));

        /* Check that the ':' is not mistaken for field specification.
         * (QueryParser syntax)
         *
         * Typical bad outputs: using a "vie" field or dropping "vie" altogether
         */
        assertEquals("frenchtitle:mech frenchtitle:vie",
                convertToStringQuery(
                        "SELECT * FROM Document WHERE frenchtitle LIKE 'mechante vie:la'"));
    }

    public void testFullTextQuery() throws QueryException {
        assertEquals(Util.COMPASS_FULLTEXT + ":foo",
                convertToStringQuery("SELECT * FROM Document WHERE " +
                        BuiltinDocumentFields.FIELD_FULLTEXT + " LIKE 'foo'"));
        // Test that analyzer was called (lowerWhitespace)
        assertEquals(Util.COMPASS_FULLTEXT + ":foo",
                convertToStringQuery("SELECT * FROM Document WHERE " +
                        BuiltinDocumentFields.FIELD_FULLTEXT + " LIKE 'fOo'"));
    }

    public void testVoidWhere() throws QueryException {
        assertEquals("MatchAllDocsQuery",
                convertToStringQuery("SELECT * FROM Document"));
    }

    public void testDateQuery() throws QueryException {
        // TODO demonstrates that one can't write seminclusive with compass
        // and this should be improved by direct Lucene query statement
        // if possible
        assertEquals("+bk:published:[2007-04-01-00-00-00-0-AM TO *]" +
                " +bk:published:[* TO 2007-04-02-00-00-00-0-AM}",
                convertToStringQuery("SELECT * FROM Document WHERE bk:published = DATE '2007-04-01'"));

        assertEquals("bk:published:[* TO 2007-04-02-00-00-00-0-AM}",
                convertToStringQuery("SELECT * FROM Document WHERE bk:published <= DATE '2007-04-01'"));

        assertEquals("bk:published:[2007-04-01-00-00-00-0-AM TO *]",
                convertToStringQuery("SELECT * FROM Document WHERE bk:published >= DATE '2007-04-01'"));

        assertEquals("bk:published:[* TO 2007-04-01-00-00-00-0-AM}",
                convertToStringQuery("SELECT * FROM Document WHERE bk:published < DATE '2007-04-01'"));

        assertEquals("bk:published:[2007-04-02-00-00-00-0-AM TO *]",
                convertToStringQuery("SELECT * FROM Document WHERE bk:published > DATE '2007-04-01'"));
    }

    public void testBetweenDateQuery() throws QueryException {
        assertEquals("bk:published:[2007-03-03-02-00-00-0-AM TO 2007-04-03-03-00-00-0-AM]",
                convertToStringQuery(
                "SELECT * FROM Document WHERE bk:published BETWEEN " +
                "TIMESTAMP '2007-03-03 02:00' AND " +
                "TIMESTAMP '2007-04-03 03:00'"));
        // TODO cf remark on boolean
        assertEquals("+bk:published:[2007-03-03-02-00-00-0-AM TO *] " +
                "+bk:published:[* TO 2007-04-04-00-00-00-0-AM}",
                convertToStringQuery(
                "SELECT * FROM Document WHERE bk:published BETWEEN " +
                "TIMESTAMP '2007-03-03 02:00' AND " +
                "DATE '2007-04-03'"));
    }

    public void testBetweenQuery() throws QueryException {
        assertEquals("kw1:[bar TO foo]",
                convertToStringQuery("SELECT * FROM Document " +
                        "WHERE kw1 BETWEEN 'bar' AND 'foo'"));
        assertEquals("kw1:[* TO foo]",
                convertToStringQuery("SELECT * FROM Document " +
                        "WHERE kw1 <= 'foo'"));
        assertEquals("kw1:[* TO foo}",
                convertToStringQuery("SELECT * FROM Document " +
                        "WHERE kw1 < 'foo'"));
        assertEquals("kw1:{foo TO *]",
                convertToStringQuery("SELECT * FROM Document " +
                        "WHERE kw1 > 'foo'"));
        assertEquals("kw1:[foo TO *]",
                convertToStringQuery("SELECT * FROM Document " +
                "WHERE kw1 >= 'foo'"));
    }

    public void testIntQuery() throws QueryException {
        assertEquals("bk:pages:00000000000000000437",
                convertToStringQuery("SELECT * FROM Document " +
                        "WHERE bk:pages = 437"));
    }

    public void testMakeSecurityQuery() throws QueryException {
        String[] groups = new String[2];
        groups[0] = "g";
        groups[1] = "h";
        SearchPrincipal principal = new SearchPrincipalImpl("u", groups, false);
        List<String> perms = Arrays.asList("p", "q");
        // !! Test relies on irrelevant orderings and value of separator
        assertEquals("secu:MatchBefore([+u#p, +g#p, +h#p, +u#q, +g#q, +h#q], " +
                "[-u#p, -g#p, -h#p, -u#q, -g#q, -h#q])",
                converter.makeSecurityQuery(principal, perms, "secu").toString());
    }


    public void xtestOrderQuery() throws QueryException {
        // TODO find a way to instrospect the order clause that has been set
        // and finish this test (higher level test from SearchBackendTestCase
        // applies
        assertEquals("",
                converter.toCompassQuery(SQLQueryParser.parse(
                "SELECT * FROM Document ORDER BY kw1"), null)
        );
    }

    public void testUnsupportedOrderClause() throws QueryException {
        try {
            @SuppressWarnings("unused")
            CompassQuery compassQuery = converter.toCompassQuery(
                    SQLQueryParser.parse(
                                "SELECT * FROM Document ORDER BY frenchtitle"),
                                null);
        } catch (QueryException e) {
            return;
        }
        fail("Should have thrown a QueryException");
    }

    public static List<String> splittedSort(String s) {
        String[] a = s.split("[ -()]");
        Arrays.sort(a);
        return Arrays.asList(a);
    }

    public void assertEqualsWithDocType(String expected, String value) {
        assertEquals(splittedSort(expected.replaceAll("docType",
                                  BuiltinDocumentFields.FIELD_DOC_TYPE)),
                     splittedSort(value));
    }

    public void testFacetQueries() throws QueryException {
        Set<String> types = new HashSet<String>();
        types.addAll(Arrays.asList("Fish", "Vegetable"));
        facetsToTypes.put("eatable", types);

        assertEqualsWithDocType("docType:Fish docType:Vegetable",
                convertToStringQuery(String.format(
                        "SELECT * FROM Document WHERE %s = 'eatable'",
                        BuiltinDocumentFields.FIELD_DOC_FACETS)));

        assertEqualsWithDocType("+MatchAllDocsQuery " +
                "-(docType:Fish docType:Vegetable)",
            convertToStringQuery(String.format(
                    "SELECT * FROM Document WHERE %s != 'eatable'",
                    BuiltinDocumentFields.FIELD_DOC_FACETS)));

        types.add("Rock");
        facetsCollectionsToTypes.put("[eatable, solid]", types);

        assertEqualsWithDocType("docType:Fish docType:Vegetable docType:Rock",
                convertToStringQuery(String.format(
                        "SELECT * FROM Document WHERE %s " +
                        "IN ('eatable', 'solid')",
                        BuiltinDocumentFields.FIELD_DOC_FACETS)));

        assertEqualsWithDocType("+MatchAllDocsQuery " +
                "-(docType:Fish docType:Rock docType:Vegetable)",
            convertToStringQuery(String.format(
                    "SELECT * FROM Document WHERE %s NOT IN " +
                        "('eatable', 'solid')",
                    BuiltinDocumentFields.FIELD_DOC_FACETS)));
    }

    public void testFromClause() throws QueryException {
        Set<String> types = new HashSet<String>();
        types.addAll(Arrays.asList("Food", "Vegetable"));
        typesInheritance.put("Food", types);
        types = new HashSet<String>();
        types.add("Vegetable");
        typesInheritance.put("Vegetable", types);
        assertEqualsWithDocType("docType:Vegetable",
                convertToStringQuery("SELECT * FROM Vegetable"));
        assertEqualsWithDocType("docType:Food docType:Vegetable",
                convertToStringQuery("SELECT * FROM Food"));
        try {
            @SuppressWarnings("unused")
            String dummy = convertToStringQuery("SELECT * FROM Unknown");
            fail("Expected a QueryException");
        } catch (QueryException e) {

        }
    }

    public void testStartsWith() throws QueryException {
        assertEquals("kw1:foo*", convertToStringQuery(
            "SELECT * FROM Document WHERE kw1 STARTSWITH 'foo'"));
        assertEquals("title:foo*", convertToStringQuery(
            "SELECT * FROM Document WHERE title STARTSWITH 'foo'"));
    }

    public void testPureNotQuery() throws QueryException {
        // NXP-1816: NOT ... STARTSWITH is a pure not, ie internally looks
        // like NOT (... STARTSWITH ...) instead of e.g, NOTSTARTSWITH ... (dedicated)
        // operator.
        assertEquals("+MatchAllDocsQuery -ecm:path:/some/path", convertToStringQuery(
            "SELECT * FROM Document WHERE NOT ecm:path STARTSWITH '/some/path'"));
    }

}
