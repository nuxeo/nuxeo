/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.relations;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.relations.api.Literal;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.QueryResult;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.BlankImpl;
import org.nuxeo.ecm.platform.relations.api.impl.LiteralImpl;
import org.nuxeo.ecm.platform.relations.api.impl.QNameResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.RelationDate;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;
import org.nuxeo.ecm.platform.relations.descriptors.GraphDescriptor;
import org.nuxeo.runtime.api.Framework;

public class TestCoreGraph extends SQLRepositoryTestCase {

    public static final String DC_TERMS_NS = "http://purl.org/dc/terms/";

    private static final String GRAPH_NAME = "myrelations";

    private RelationManager service;

    private CoreGraph graph;

    private List<Statement> statements;

    private Resource doc1;

    private Resource doc2;

    private QNameResource isBasedOn;

    private QNameResource references;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.relations");
        deployContrib("org.nuxeo.ecm.relations.tests",
                "relation-core-test-contrib.xml");
        openSession();
        service = Framework.getService(RelationManager.class);

        statements = new ArrayList<Statement>();
        doc1 = new QNameResourceImpl(RelationConstants.DOCUMENT_NAMESPACE,
                database.repositoryName
                        + "/00010000-2c86-46fa-909e-02494bcb0001");
        doc2 = new QNameResourceImpl(RelationConstants.DOCUMENT_NAMESPACE,
                database.repositoryName
                        + "/00020000-2c86-46fa-909e-02494bcb0002");
        isBasedOn = new QNameResourceImpl(DC_TERMS_NS, "IsBasedOn");
        references = new QNameResourceImpl(DC_TERMS_NS, "References");
        statements.add(new StatementImpl(doc2, isBasedOn, doc1));
        statements.add(new StatementImpl(doc1, references, new ResourceImpl(
                "http://www.wikipedia.com/Enterprise_Content_Management")));
        statements.add(new StatementImpl(doc2, references, new LiteralImpl(
                "NXRuntime")));
        Collections.sort(statements);

        graph = (CoreGraph) service.getGraphByName(GRAPH_NAME);
        assertNotNull(graph);
    }

    public void useGraphWithSession() throws Exception {
        graph = (CoreGraph) service.getGraph(GRAPH_NAME, session);
    }

    @After
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    private static String getTestFile() {
        String filePath = "test.rdf";
        return FileUtils.getResourcePathFromContext(filePath);
    }

    @Test
    public void testSetOptions() {
        Map<String, String> options = new HashMap<String, String>();
        options.put(CoreGraph.OPTION_DOCTYPE, "Foo");
        try {
            graph.setOptions(options);
            fail("Should have raised IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // ok
        }
        options.put(CoreGraph.OPTION_DOCTYPE, "Relation");
        graph.setOptions(options);
    }

    //public static void assertEquals(int expected, Long actual) {
    //    assertEquals(Long.valueOf(expected), actual);
    //}

    @Test
    public void testAdd() {
        assertEquals(0, (double)graph.size(), 1e-8);
        graph.add(statements);
        assertEquals(3, (double)graph.size(), 1e-8);
    }

    @Test
    public void testAddWithSession() throws Exception {
        useGraphWithSession();
        testAdd();
    }

    @Test
    public void testSubjectResource() {
        assertEquals(0, (double)graph.size(), 1e-8);
        Resource src = new ResourceImpl("urn:foo:1234");
        Statement st = new StatementImpl(src, isBasedOn, doc1);
        graph.add(st);
        List<Statement> stmts = graph.getStatements();
        assertEquals(1, stmts.size());
        assertEquals(st, stmts.get(0));
        graph.remove(Collections.singletonList(st));
        assertEquals(0, (double)graph.size(), 1e-8);
    }

    @Test
    public void testBlank() {
        assertEquals(0, (double)graph.size(), 1e-8);
        Node src = new BlankImpl();
        Node dst = new BlankImpl("123");
        Statement st = new StatementImpl(src, isBasedOn, dst);
        graph.add(st);
        List<Statement> stmts = graph.getStatements();
        assertEquals(1, stmts.size());
        assertEquals(st, stmts.get(0));
        graph.remove(Collections.singletonList(st));
        assertEquals(0, (double)graph.size(), 1e-8);
    }

    @Test
    public void testNamespaces() {
        String NS = "http://foo.com/";
        // without namespace
        Resource src = new ResourceImpl(NS + "bar");
        Statement stmt = new StatementImpl(src, isBasedOn, doc1);
        graph.add(stmt);
        List<Statement> stmts = graph.getStatements();
        Statement st = stmts.get(0);
        assertEquals(stmt, st);
        assertFalse(st.getSubject().isQNameResource());

        graph.remove(Collections.singletonList(stmt));

        GraphDescriptor desc = new GraphDescriptor();
        desc.namespaces = Collections.singletonMap("prfx", NS);
        graph.setDescription(desc);
        graph.add(stmt);
        stmts = graph.getStatements();
        st = stmts.get(0);
        assertTrue(st.getSubject().isQNameResource());
    }

    @Test
    public void testRemove() {
        assertEquals(0, (double)graph.size(), 1e-8);
        graph.add(statements);
        assertEquals(3, (double)graph.size(), 1e-8);
        List<Statement> stmts = new ArrayList<Statement>();
        stmts.add(new StatementImpl(doc2, references, new LiteralImpl(
                "NXRuntime")));
        graph.remove(stmts);
        assertEquals(2, (double)graph.size(), 1e-8);
    }

    @Test
    public void testRemoveWithSession() throws Exception {
        useGraphWithSession();
        testRemove();
    }

    @Test
    public void testStatementProperties() {
        List<Statement> stmts = new ArrayList<Statement>();
        Node p;
        Calendar cal = Calendar.getInstance();
        cal.set(2012, 12 - 1, 21, 1, 2, 3);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();
        Statement st = new StatementImpl(doc2, isBasedOn, doc1);
        st.setProperty(RelationConstants.AUTHOR, new LiteralImpl("bob"));
        st.setProperty(RelationConstants.CREATION_DATE,
                RelationDate.getLiteralDate(date));
        st.setProperty(RelationConstants.MODIFICATION_DATE,
                RelationDate.getLiteralDate(date));
        st.setProperty(RelationConstants.COMMENT, new LiteralImpl("hi there"));
        graph.add(st);
        stmts = graph.getStatements();
        assertEquals(1, stmts.size());
        st = stmts.get(0);
        p = st.getProperty(RelationConstants.AUTHOR);
        assertEquals("bob", ((Literal) p).getValue());
        // no DublinCoreListener registered, dates are unchanged
        p = st.getProperty(RelationConstants.CREATION_DATE);
        assertEquals(date.getTime(),
                RelationDate.getDate((Literal) p).getTime());
        p = st.getProperty(RelationConstants.MODIFICATION_DATE);
        assertEquals(date.getTime(),
                RelationDate.getDate((Literal) p).getTime());
        p = st.getProperty(RelationConstants.COMMENT);
        assertEquals("hi there", ((Literal) p).getValue());
    }

    @Test
    public void testGetStatements() {
        List<Statement> stmts = new ArrayList<Statement>();
        assertEquals(stmts, graph.getStatements());
        graph.add(statements);
        stmts = graph.getStatements();
        Collections.sort(stmts);
        assertEquals(statements, stmts);
    }

    @Test
    public void testGetStatementsPattern() {
        List<Statement> expected = new ArrayList<Statement>();
        assertEquals(expected, graph.getStatements());
        graph.add(statements);

        List<Statement> stmts = graph.getStatements(new StatementImpl(null,
                null, null));
        Collections.sort(stmts);
        expected = statements;
        assertEquals(expected, stmts);

        stmts = graph.getStatements(new StatementImpl(doc1, null, null));
        Collections.sort(stmts);
        expected = new ArrayList<Statement>();
        expected.add(new StatementImpl(doc1, references, new ResourceImpl(
                "http://www.wikipedia.com/Enterprise_Content_Management")));
        assertEquals(expected, stmts);

        stmts = graph.getStatements(new StatementImpl(null, references, null));
        Collections.sort(stmts);
        expected = new ArrayList<Statement>();
        expected.add(new StatementImpl(doc1, references, new ResourceImpl(
                "http://www.wikipedia.com/Enterprise_Content_Management")));
        expected.add(new StatementImpl(doc2, references, new LiteralImpl(
                "NXRuntime")));
        assertEquals(expected, stmts);

        stmts = graph.getStatements(new StatementImpl(doc2, null, doc1));
        Collections.sort(stmts);
        expected = new ArrayList<Statement>();
        expected.add(new StatementImpl(doc2, isBasedOn, doc1));
        assertEquals(expected, stmts);

        // test with unknown nodes
        expected = new ArrayList<Statement>();
        stmts = graph.getStatements(new StatementImpl(new ResourceImpl(
                "http://subject"), new ResourceImpl("http://propertty"),
                new ResourceImpl("http://object")));
        assertEquals(expected, stmts);

        stmts = graph.getStatements(new StatementImpl(new ResourceImpl(
                "http://subject"), null, new LiteralImpl("literal")));
        assertEquals(expected, stmts);

        stmts = graph.getStatements(new StatementImpl(new ResourceImpl(
                "http://subject"), null, new BlankImpl("blank")));
        assertEquals(expected, stmts);
    }

    @Test
    public void testGetStatementsPatternWithSession() throws Exception {
        useGraphWithSession();
        testGetStatementsPattern();
    }

    @Test
    public void testGetSubjects() {
        graph.add(statements);
        List<Node> res;

        res = graph.getSubjects(references, new ResourceImpl(
                "http://www.wikipedia.com/Enterprise_Content_Management"));
        assertEquals(Collections.singletonList(doc1), res);

        res = graph.getSubjects(null, doc1);
        assertEquals(Collections.singletonList(doc2), res);

        Set<Node> docs = new HashSet<Node>(Arrays.asList(doc1, doc2));
        res = graph.getSubjects(references, null);
        assertEquals(docs, new HashSet<Node>(res));

        res = graph.getSubjects(null, null);
        assertEquals(docs, new HashSet<Node>(res));
    }

    @Test
    public void testGetPredicates() {
        graph.add(statements);
        List<Node> res;

        res = graph.getPredicates(doc2, doc1);
        assertEquals(Collections.singletonList(isBasedOn), res);

        res = graph.getPredicates(null, doc1);
        assertEquals(Collections.singletonList(isBasedOn), res);

        Set<Node> both = new HashSet<Node>(Arrays.asList(isBasedOn, references));
        res = graph.getPredicates(doc2, null);
        assertEquals(both, new HashSet<Node>(res));

        res = graph.getPredicates(null, null);
        assertEquals(both, new HashSet<Node>(res));
    }

    @Test
    public void testGetObject() {
        graph.add(statements);
        List<Node> res;
        Literal lit = new LiteralImpl("NXRuntime");
        Resource reswiki = new ResourceImpl(
                "http://www.wikipedia.com/Enterprise_Content_Management");

        res = graph.getObjects(doc2, isBasedOn);
        assertEquals(Collections.singletonList(doc1), res);

        res = graph.getObjects(doc2, null);
        assertEquals(new HashSet<Node>(Arrays.asList(doc1, lit)),
                new HashSet<Node>(res));

        res = graph.getObjects(null, references);
        assertEquals(new HashSet<Node>(Arrays.asList(reswiki, lit)),
                new HashSet<Node>(res));

        res = graph.getObjects(null, null);
        assertEquals(new HashSet<Node>(Arrays.asList(doc1, reswiki, lit)),
                new HashSet<Node>(res));
    }

    @Test
    public void testHasStatement() {
        graph.add(statements);
        assertFalse(graph.hasStatement(null));
        assertTrue(graph.hasStatement(new StatementImpl(doc2, isBasedOn, doc1)));
        assertFalse(graph.hasStatement(new StatementImpl(doc2, isBasedOn, doc2)));
        assertTrue(graph.hasStatement(new StatementImpl(doc2, isBasedOn, null)));
        assertFalse(graph.hasStatement(new StatementImpl(null, null, doc2)));
    }

    @Test
    public void testHasResource() {
        graph.add(statements);
        assertFalse(graph.hasResource(null));
        assertTrue(graph.hasResource(doc1));
        assertTrue(graph.hasResource(doc2));
        assertTrue(graph.hasResource(isBasedOn));
        assertTrue(graph.hasResource(references));
        assertFalse(graph.hasResource(new ResourceImpl("http://foo")));
    }

    @Test
    public void testSize() {
        assertEquals(0, (double)graph.size(), 1e-8);
        List<Statement> stmts = new ArrayList<Statement>();
        stmts.add(new StatementImpl(doc1, isBasedOn, new LiteralImpl("foo")));
        graph.add(stmts);
        assertEquals(1, (double)graph.size(), 1e-8);
        graph.add(statements);
        assertEquals(4, (double)graph.size(), 1e-8);
    }

    @Test
    public void testClear() {
        assertEquals(0, (double)graph.size(), 1e-8);
        graph.add(statements);
        assertEquals(3, (double)graph.size(), 1e-8);
        graph.clear();
        assertEquals(0, (double)graph.size(), 1e-8);
    }

    public void TODOtestQuery() {
        graph.add(statements);
        String queryString = "SELECT ?subj ?pred ?obj " //
                + "WHERE {" //
                + "      ?subj ?pred ?obj " //
                + "       }";
        QueryResult res = graph.query(queryString, "sparql", null);
        assertEquals(3, res.getCount().intValue());
        List<String> variableNames = new ArrayList<String>();
        variableNames.add("subj");
        variableNames.add("pred");
        variableNames.add("obj");
        assertEquals(variableNames, res.getVariableNames());

        queryString = "PREFIX dcterms: <http://purl.org/dc/terms/> "
                + "SELECT ?subj ?obj " + "WHERE {"
                + "      ?subj dcterms:References ?obj ." + "       }";
        res = graph.query(queryString, "sparql", null);
        assertEquals(2, res.getCount().intValue());
        variableNames.remove("pred");
        assertEquals(variableNames, res.getVariableNames());
    }

    public void TODOtest() {
        String queryString = "SELECT ?s ?o WHERE { ?s <http://foo> ?o . }";
        /**
         * <code>
         * SELECT ?s WHERE {?s <http://www.w3.org/2000/10/annotation-ns#body> ?o}
         *
         * SELECT ?o WHERE {?s <http://www.w3.org/2000/10/annotation-ns#body> ?o}
         *
         * SELECT ?o WHERE {?s <http://www.w3.org/2000/10/annotation-ns#annotates> ?o}
         *
         * SELECT ?o WHERE {?s <http://purl.org/dc/elements/1.1/creator> ?o}
         *
         * SELECT ?s ?p ?o WHERE { ?s ?p ?o . }
         *
         * SELECT ?s ?p ?o WHERE {
         *   ?s ?p ?o .
         *   ?s <http://www.w3.org/2000/10/annotation-ns#annotates> <someuri> .
         * }
         * </code>
         */
        String q = queryString.replace("\n", " ").replace("\r", " ").trim();
        Pattern PAT = Pattern.compile("SELECT\\s+" //
                + "(.*)" // group 1
                + "\\s+WHERE\\s*\\{\\s*" //
                + "(.*)" // group 2
                + "\\s*\\}");
        Matcher m = PAT.matcher(q);
        assertTrue(m.matches());

        String g1 = m.group(1);
        String g2 = m.group(2);
        assertEquals("?s ?o", g1);

        String[] vars = g1.split(" ");
        for (String var : vars) {
            if (!var.startsWith("?") || var.length() == 1) {
                throw new IllegalArgumentException("Invalid query variable: "
                        + var + " in query: " + queryString);
            }
            // String v = var.substring(1);
        }

        assertEquals("?s <http://foo> ?o . ", g2);
        String[] tuples = (g2 + " ").split(" . ");
        for (String tuple : tuples) {
            tuple = tuple.trim();
            if (tuple.isEmpty()) {
                continue;
            }
            assertEquals("?s <http://foo> ?o", tuple);

        }
    }

    public void TODOtestRead() throws Exception {
        InputStream in = new FileInputStream(getTestFile());
        assertEquals(0, (double)graph.size(), 1e-8);
        graph.read(in, null, null);
        assertEquals(0, (double)graph.size(), 1e-8);
        List<Statement> statements = graph.getStatements();
        Collections.sort(statements);
        // assertSame(statements.size(), this.statements.size());
        // for (int i = 0; i < statements.size(); i++) {
        // assertEquals(statements.get(i), this.statements.get(i));
        // }
        assertEquals(statements, this.statements);
    }

    public void TODOtestReadPath() {
        assertEquals(0, (double)graph.size(), 1e-8);
        graph.read(getTestFile(), null, null);
        assertFalse(graph.size().intValue() == 0);
        List<Statement> statements = graph.getStatements();
        Collections.sort(statements);
        // assertSame(statements.size(), this.statements.size());
        // for (int i = 0; i < statements.size(); i++) {
        // assertEquals(statements.get(i), this.statements.get(i));
        // }
        assertEquals(statements, this.statements);
    }

    public void TODOtestWrite() throws Exception {
        graph.add(statements);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        graph.write(out, null, null);
        InputStream written = new ByteArrayInputStream(out.toByteArray());
        InputStream expected = new FileInputStream(getTestFile());
        assertEquals(FileUtils.read(expected).replaceAll("\r?\n", ""),
                FileUtils.read(written).replaceAll("\r?\n", ""));
    }

    public void TODOtestWritePath() throws Exception {
        graph.add(statements);
        File file = File.createTempFile("test", ".rdf");
        String path = file.getPath();
        graph.write(path, null, null);
        InputStream written = new FileInputStream(new File(path));
        InputStream expected = new FileInputStream(getTestFile());

        String expectedString = FileUtils.read(expected).replaceAll("\r?\n", "");
        String writtenString = FileUtils.read(written).replaceAll("\r?\n", "");
        assertEquals(expectedString, writtenString);
    }

}
