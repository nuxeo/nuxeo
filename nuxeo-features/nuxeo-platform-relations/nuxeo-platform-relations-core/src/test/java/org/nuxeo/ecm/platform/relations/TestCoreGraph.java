/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.relations;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
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
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.relations")
@LocalDeploy("org.nuxeo.ecm.relations.tests:relation-core-test-contrib.xml")
public class TestCoreGraph {

    public static final String DC_TERMS_NS = "http://purl.org/dc/terms/";

    private static final String GRAPH_NAME = "myrelations";

    @Inject
    protected CoreSession session;

    @Inject
    private RelationManager service;

    private CoreGraph graph;

    private List<Statement> statements;

    private Resource doc1;

    private Resource doc2;

    private QNameResource isBasedOn;

    private QNameResource references;

    @Before
    public void setUp() throws Exception {
        statements = new ArrayList<>();
        doc1 = new QNameResourceImpl(RelationConstants.DOCUMENT_NAMESPACE,
                session.getRepositoryName() + "/00010000-2c86-46fa-909e-02494bcb0001");
        doc2 = new QNameResourceImpl(RelationConstants.DOCUMENT_NAMESPACE,
                session.getRepositoryName() + "/00020000-2c86-46fa-909e-02494bcb0002");
        isBasedOn = new QNameResourceImpl(DC_TERMS_NS, "IsBasedOn");
        references = new QNameResourceImpl(DC_TERMS_NS, "References");
        statements.add(new StatementImpl(doc2, isBasedOn, doc1));
        statements.add(new StatementImpl(doc1, references,
                new ResourceImpl("http://www.wikipedia.com/Enterprise_Content_Management")));
        statements.add(new StatementImpl(doc2, references, new LiteralImpl("NXRuntime")));
        Collections.sort(statements);

        graph = (CoreGraph) service.getGraphByName(GRAPH_NAME);
    }

    public void useGraphWithSession() throws Exception {
        graph = (CoreGraph) service.getGraph(GRAPH_NAME, session);
    }

    private static String getTestFile() {
        String filePath = "test.rdf";
        return FileUtils.getResourcePathFromContext(filePath);
    }

    @Test
    public void testSetOptions() {
        Map<String, String> options = new HashMap<>();
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

    @Test
    public void testAdd() {
        assertEquals(Long.valueOf(0), graph.size());
        graph.add(statements);
        assertEquals(Long.valueOf(3), graph.size());
    }

    @Test
    public void testAddWithKnowPredicateNamespace() {
        assertEquals(Long.valueOf(0), graph.size());
        Resource src = new ResourceImpl("http://foo.com/bar");
        Statement stmt = new StatementImpl(src,
                new QNameResourceImpl(RelationConstants.DOCUMENT_NAMESPACE, "startContainer"),
                new LiteralImpl("/html[1]/body[1]/p[4], 3"));
        graph.add(stmt);
        assertEquals(Long.valueOf(1), graph.size());
    }

    @Test
    public void testAddWithSession() throws Exception {
        useGraphWithSession();
        testAdd();
    }

    @Test
    public void testSubjectResource() {
        assertEquals(Long.valueOf(0), graph.size());
        Resource src = new ResourceImpl("urn:foo:1234");
        Statement st = new StatementImpl(src, isBasedOn, doc1);
        graph.add(st);
        List<Statement> stmts = graph.getStatements();
        assertEquals(1, stmts.size());
        assertEquals(st, stmts.get(0));
        graph.remove(Collections.singletonList(st));
        assertEquals(Long.valueOf(0), graph.size());
    }

    @Test
    public void testBlank() {
        assertEquals(Long.valueOf(0), graph.size());
        Node src = new BlankImpl();
        Node dst = new BlankImpl("123");
        Statement st = new StatementImpl(src, isBasedOn, dst);
        graph.add(st);
        List<Statement> stmts = graph.getStatements();
        assertEquals(1, stmts.size());
        assertEquals(st, stmts.get(0));
        graph.remove(Collections.singletonList(st));
        assertEquals(Long.valueOf(0), graph.size());
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
        assertEquals(Long.valueOf(0), graph.size());
        graph.add(statements);
        assertEquals(Long.valueOf(3), graph.size());
        List<Statement> stmts = new ArrayList<>();
        stmts.add(new StatementImpl(doc2, references, new LiteralImpl("NXRuntime")));
        graph.remove(stmts);
        assertEquals(Long.valueOf(2), graph.size());
    }

    @Test
    public void testRemoveWithSession() throws Exception {
        useGraphWithSession();
        testRemove();
    }

    @Test
    public void testStatementProperties() {
        List<Statement> stmts = new ArrayList<>();
        Node p;
        Calendar cal = Calendar.getInstance();
        cal.set(2012, 12 - 1, 21, 1, 2, 3);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();
        Statement st = new StatementImpl(doc2, isBasedOn, doc1);
        st.setProperty(RelationConstants.AUTHOR, new LiteralImpl("bob"));
        st.setProperty(RelationConstants.CREATION_DATE, RelationDate.getLiteralDate(date));
        st.setProperty(RelationConstants.MODIFICATION_DATE, RelationDate.getLiteralDate(date));
        st.setProperty(RelationConstants.COMMENT, new LiteralImpl("hi there"));
        graph.add(st);
        stmts = graph.getStatements();
        assertEquals(1, stmts.size());
        st = stmts.get(0);
        p = st.getProperty(RelationConstants.AUTHOR);
        assertEquals("bob", ((Literal) p).getValue());
        // no DublinCoreListener registered, dates are unchanged
        p = st.getProperty(RelationConstants.CREATION_DATE);
        assertEquals(date.getTime(), RelationDate.getDate((Literal) p).getTime());
        p = st.getProperty(RelationConstants.MODIFICATION_DATE);
        assertEquals(date.getTime(), RelationDate.getDate((Literal) p).getTime());
        p = st.getProperty(RelationConstants.COMMENT);
        assertEquals("hi there", ((Literal) p).getValue());
    }

    @Test
    public void testGetStatements() {
        List<Statement> stmts = new ArrayList<>();
        assertEquals(stmts, graph.getStatements());
        graph.add(statements);
        stmts = graph.getStatements();
        Collections.sort(stmts);
        assertEquals(statements, stmts);
    }

    @Test
    public void testGetStatementsPattern() {
        List<Statement> expected = new ArrayList<>();
        assertEquals(expected, graph.getStatements());
        graph.add(statements);

        List<Statement> stmts = graph.getStatements(new StatementImpl(null, null, null));
        Collections.sort(stmts);
        expected = statements;
        assertEquals(expected, stmts);

        stmts = graph.getStatements(new StatementImpl(doc1, null, null));
        Collections.sort(stmts);
        expected = new ArrayList<>();
        expected.add(new StatementImpl(doc1, references,
                new ResourceImpl("http://www.wikipedia.com/Enterprise_Content_Management")));
        assertEquals(expected, stmts);

        stmts = graph.getStatements(new StatementImpl(null, references, null));
        Collections.sort(stmts);
        expected = new ArrayList<>();
        expected.add(new StatementImpl(doc1, references,
                new ResourceImpl("http://www.wikipedia.com/Enterprise_Content_Management")));
        expected.add(new StatementImpl(doc2, references, new LiteralImpl("NXRuntime")));
        assertEquals(expected, stmts);

        stmts = graph.getStatements(new StatementImpl(doc2, null, doc1));
        Collections.sort(stmts);
        expected = new ArrayList<>();
        expected.add(new StatementImpl(doc2, isBasedOn, doc1));
        assertEquals(expected, stmts);

        // test with unknown nodes
        expected = new ArrayList<>();
        stmts = graph.getStatements(new StatementImpl(new ResourceImpl("http://subject"),
                new ResourceImpl("http://propertty"), new ResourceImpl("http://object")));
        assertEquals(expected, stmts);

        stmts = graph.getStatements(
                new StatementImpl(new ResourceImpl("http://subject"), null, new LiteralImpl("literal")));
        assertEquals(expected, stmts);

        stmts = graph.getStatements(
                new StatementImpl(new ResourceImpl("http://subject"), null, new BlankImpl("blank")));
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

        res = graph.getSubjects(references, new ResourceImpl("http://www.wikipedia.com/Enterprise_Content_Management"));
        assertEquals(Collections.singletonList(doc1), res);

        res = graph.getSubjects(null, doc1);
        assertEquals(Collections.singletonList(doc2), res);

        Set<Node> docs = new HashSet<>(Arrays.asList(doc1, doc2));
        res = graph.getSubjects(references, null);
        assertEquals(docs, new HashSet<>(res));

        res = graph.getSubjects(null, null);
        assertEquals(docs, new HashSet<>(res));
    }

    @Test
    public void testGetPredicates() {
        graph.add(statements);
        List<Node> res;

        res = graph.getPredicates(doc2, doc1);
        assertEquals(Collections.singletonList(isBasedOn), res);

        res = graph.getPredicates(null, doc1);
        assertEquals(Collections.singletonList(isBasedOn), res);

        Set<Node> both = new HashSet<>(Arrays.asList(isBasedOn, references));
        res = graph.getPredicates(doc2, null);
        assertEquals(both, new HashSet<>(res));

        res = graph.getPredicates(null, null);
        assertEquals(both, new HashSet<>(res));
    }

    @Test
    public void testGetObject() {
        graph.add(statements);
        List<Node> res;
        Literal lit = new LiteralImpl("NXRuntime");
        Resource reswiki = new ResourceImpl("http://www.wikipedia.com/Enterprise_Content_Management");

        res = graph.getObjects(doc2, isBasedOn);
        assertEquals(Collections.singletonList(doc1), res);

        res = graph.getObjects(doc2, null);
        assertEquals(new HashSet<>(Arrays.asList(doc1, lit)), new HashSet<>(res));

        res = graph.getObjects(null, references);
        assertEquals(new HashSet<>(Arrays.asList(reswiki, lit)), new HashSet<>(res));

        res = graph.getObjects(null, null);
        assertEquals(new HashSet<>(Arrays.asList(doc1, reswiki, lit)), new HashSet<>(res));
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
        assertEquals(Long.valueOf(0), graph.size());
        List<Statement> stmts = new ArrayList<>();
        stmts.add(new StatementImpl(doc1, isBasedOn, new LiteralImpl("foo")));
        graph.add(stmts);
        assertEquals(Long.valueOf(1), graph.size());
        graph.add(statements);
        assertEquals(Long.valueOf(4), graph.size());
    }

    @Test
    public void testClear() {
        assertEquals(Long.valueOf(0), graph.size());
        graph.add(statements);
        assertEquals(Long.valueOf(3), graph.size());
        graph.clear();
        assertEquals(Long.valueOf(0), graph.size());
    }

    public void TODOtestQuery() {
        graph.add(statements);
        String queryString = "SELECT ?subj ?pred ?obj " //
                + "WHERE {" //
                + "      ?subj ?pred ?obj " //
                + "       }";
        QueryResult res = graph.query(queryString, "sparql", null);
        assertEquals(3, res.getCount().intValue());
        List<String> variableNames = new ArrayList<>();
        variableNames.add("subj");
        variableNames.add("pred");
        variableNames.add("obj");
        assertEquals(variableNames, res.getVariableNames());

        queryString = "PREFIX dcterms: <http://purl.org/dc/terms/> " + "SELECT ?subj ?obj " + "WHERE {"
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
                throw new IllegalArgumentException("Invalid query variable: " + var + " in query: " + queryString);
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
        assertEquals(Long.valueOf(0), graph.size());
        graph.read(in, null, null);
        assertEquals(Long.valueOf(0), graph.size());
        List<Statement> statements = graph.getStatements();
        Collections.sort(statements);
        // assertSame(statements.size(), this.statements.size());
        // for (int i = 0; i < statements.size(); i++) {
        // assertEquals(statements.get(i), this.statements.get(i));
        // }
        assertEquals(statements, this.statements);
    }

    public void TODOtestReadPath() {
        assertEquals(Long.valueOf(0), graph.size());
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
        assertEquals(IOUtils.toString(expected, UTF_8).replaceAll("\r?\n", ""),
                IOUtils.toString(written, UTF_8).replaceAll("\r?\n", ""));
    }

    public void TODOtestWritePath() throws Exception {
        graph.add(statements);
        File file = Framework.createTempFile("test", ".rdf");
        String path = file.getPath();
        graph.write(path, null, null);
        InputStream written = new FileInputStream(new File(path));
        InputStream expected = new FileInputStream(getTestFile());

        String expectedString = IOUtils.toString(expected, UTF_8).replaceAll("\r?\n", "");
        String writtenString = IOUtils.toString(written, UTF_8).replaceAll("\r?\n", "");
        assertEquals(expectedString, writtenString);
    }

}
