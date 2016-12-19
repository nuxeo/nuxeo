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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.relations.jena;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.QueryResult;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.BlankImpl;
import org.nuxeo.ecm.platform.relations.api.impl.LiteralImpl;
import org.nuxeo.ecm.platform.relations.api.impl.QNameResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.descriptors.GraphDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

import com.hp.hpl.jena.rdf.model.Model;

public class TestJenaGraph extends NXRuntimeTestCase {

    private JenaGraph graph;

    private List<Statement> statements;

    private String namespace;

    private Resource doc1;

    private Resource doc2;

    private QNameResource isBasedOn;

    private QNameResource references;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.runtime.management");
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.relations");
        deployBundle("org.nuxeo.ecm.relations.jena");
        deployContrib("org.nuxeo.ecm.relations.jena.tests", "jena-test-bundle.xml");
        RelationManager service = Framework.getService(RelationManager.class);
        Graph graph = service.getGraphByName("myrelations");
        assertNotNull(graph);
        assertEquals(JenaGraph.class, graph.getClass());
        this.graph = (JenaGraph) graph;
        statements = new ArrayList<>();
        doc1 = new ResourceImpl("http://www.ecm.org/uid/DOC200600013_02.01");
        doc2 = new ResourceImpl("http://www.ecm.org/uid/DOC200600015_01.00");
        namespace = "http://purl.org/dc/terms/";
        isBasedOn = new QNameResourceImpl(namespace, "IsBasedOn");
        references = new QNameResourceImpl(namespace, "References");
        statements.add(new StatementImpl(doc2, isBasedOn, doc1));
        statements.add(new StatementImpl(doc1, references,
                new ResourceImpl("http://www.wikipedia.com/Enterprise_Content_Management")));
        statements.add(new StatementImpl(doc2, references, new LiteralImpl("NXRuntime")));
        Collections.sort(statements);
    }

    private static String getTestFile() {
        String filePath = "test.rdf";
        return FileUtils.getResourcePathFromContext(filePath);
    }

    @Test
    public void testGetGraph() {
        Model jenaGraph = graph.openGraph().getGraph();
        Map<String, String> map = jenaGraph.getNsPrefixMap();
        assertEquals("http://www.w3.org/1999/02/22-rdf-syntax-ns#", map.get("rdf"));
        assertEquals("http://purl.org/dc/terms/", map.get("dcterms"));
    }

    @Test
    public void testSetOptions() {
        Map<String, String> options = new HashMap<>();
        options.put("backend", "dummy");
        try {
            graph.setOptions(options);
            fail("Should have raised IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        options.put("backend", "memory");
        graph.setOptions(options);

        options.put("databaseDoCompressUri", "dummy");
        try {
            graph.setOptions(options);
            fail("Should have raised IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        options.put("databaseDoCompressUri", "true");
        graph.setOptions(options);

        options.put("databaseTransactionEnabled", "dummy");
        try {
            graph.setOptions(options);
            fail("Should have raised IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        options.put("databaseTransactionEnabled", "false");
        graph.setOptions(options);
    }

    @Test
    public void testSetNamespaces() {
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("dummy", "http://dummy");

        boolean forceReload = false;
        Model jenaGraph = graph.openGraph(forceReload).getGraph();
        Map<String, String> map = jenaGraph.getNsPrefixMap();
        // old namespaces are kept
        assertEquals("http://www.w3.org/1999/02/22-rdf-syntax-ns#", map.get("rdf"));
        assertEquals("http://purl.org/dc/terms/", map.get("dcterms"));
        assertNull(map.get("dummy"));

        GraphDescriptor desc = new GraphDescriptor();
        desc.namespaces = namespaces;
        graph.setDescription(desc);

        // not set yet on the graph, have to rebuild it
        jenaGraph = graph.openGraph(forceReload).getGraph();
        map = jenaGraph.getNsPrefixMap();
        assertEquals("http://www.w3.org/1999/02/22-rdf-syntax-ns#", map.get("rdf"));
        assertEquals("http://purl.org/dc/terms/", map.get("dcterms"));
        assertNull(map.get("dummy"));

        // rebuild graph
        forceReload = true;
        jenaGraph = graph.openGraph(forceReload).getGraph();
        map = jenaGraph.getNsPrefixMap();
        // old namespaces are still set on the named graph lots
        assertNull(map.get("rdf"));
        assertNull(map.get("dcterms"));
        assertEquals("http://dummy", map.get("dummy"));
    }

    @Test
    public void testAdd() {
        assertSame(0L, graph.size());
        graph.add(statements);
        assertSame(3L, graph.size());
    }

    @Test
    public void testRemove() {
        assertSame(0L, graph.size());
        graph.add(statements);
        assertSame(3L, graph.size());
        List<Statement> stmts = new ArrayList<>();
        stmts.add(new StatementImpl(doc2, references, new LiteralImpl("NXRuntime")));
        graph.remove(stmts);
        assertSame(2L, graph.size());
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
    public void testGetSubjects() {
        graph.add(statements);
        List<Node> expected;
        List<Node> res;

        res = graph.getSubjects(references, new ResourceImpl("http://www.wikipedia.com/Enterprise_Content_Management"));
        Collections.sort(res);
        expected = new ArrayList<>();
        expected.add(doc1);
        Collections.sort(expected);
        assertEquals(expected, res);

        res = graph.getSubjects(references, null);
        Collections.sort(res);
        expected = new ArrayList<>();
        expected.add(doc1);
        expected.add(doc2);
        Collections.sort(expected);
        assertEquals(expected, res);

        res = graph.getSubjects(null, doc1);
        Collections.sort(res);
        expected = new ArrayList<>();
        expected.add(doc2);
        Collections.sort(expected);
        assertEquals(expected, res);

        res = graph.getSubjects(null, null);
        Collections.sort(res);
        expected = new ArrayList<>();
        expected.add(doc1);
        expected.add(doc2);
        Collections.sort(expected);
        assertEquals(expected, res);
    }

    @Test
    public void testGetPredicates() {
        graph.add(statements);
        List<Node> expected;
        List<Node> res;

        res = graph.getPredicates(doc2, doc1);
        Collections.sort(res);
        expected = new ArrayList<>();
        expected.add(isBasedOn);
        Collections.sort(expected);
        assertEquals(expected, res);

        res = graph.getPredicates(doc2, null);
        Collections.sort(res);
        expected = new ArrayList<>();
        expected.add(isBasedOn);
        expected.add(references);
        Collections.sort(expected);
        assertEquals(expected, res);

        res = graph.getPredicates(null, doc1);
        Collections.sort(res);
        expected = new ArrayList<>();
        expected.add(isBasedOn);
        Collections.sort(expected);
        assertEquals(expected, res);

        res = graph.getPredicates(null, null);
        Collections.sort(res);
        expected = new ArrayList<>();
        expected.add(isBasedOn);
        expected.add(references);
        Collections.sort(expected);
        assertEquals(expected, res);
    }

    @Test
    public void testGetObject() {
        graph.add(statements);
        List<Node> expected;
        List<Node> res;

        res = graph.getObjects(doc2, isBasedOn);
        Collections.sort(res);
        expected = new ArrayList<>();
        expected.add(doc1);
        Collections.sort(expected);
        assertEquals(expected, res);

        res = graph.getObjects(doc2, null);
        Collections.sort(res);
        expected = new ArrayList<>();
        expected.add(doc1);
        expected.add(new LiteralImpl("NXRuntime"));
        Collections.sort(expected);
        assertEquals(expected, res);

        res = graph.getObjects(null, references);
        Collections.sort(res);
        expected = new ArrayList<>();
        expected.add(new ResourceImpl("http://www.wikipedia.com/Enterprise_Content_Management"));
        expected.add(new LiteralImpl("NXRuntime"));
        Collections.sort(expected);
        assertEquals(expected, res);

        res = graph.getObjects(null, null);
        Collections.sort(res);
        expected = new ArrayList<>();
        expected.add(doc1);
        expected.add(new ResourceImpl("http://www.wikipedia.com/Enterprise_Content_Management"));
        expected.add(new LiteralImpl("NXRuntime"));
        Collections.sort(expected);
        assertEquals(expected, res);
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
        assertFalse(graph.hasResource(new ResourceImpl("http://foo")));
    }

    @Test
    public void testSize() {
        assertSame(0L, graph.size());
        List<Statement> stmts = new ArrayList<>();
        stmts.add(new StatementImpl(doc1, isBasedOn, new LiteralImpl("foo")));
        graph.add(stmts);
        assertSame(1L, graph.size());
        graph.add(statements);
        assertSame(4L, graph.size());
    }

    @Test
    public void testClear() {
        assertSame(0L, graph.size());
        graph.add(statements);
        assertSame(3L, graph.size());
        graph.clear();
        assertSame(0L, graph.size());
    }

    @Test
    public void testQuery() {
        graph.add(statements);
        String queryString = "SELECT ?subj ?pred ?obj " + "WHERE {" + "      ?subj ?pred ?obj " + "       }";
        QueryResult res = graph.query(queryString, "sparql", null);
        assertSame(3, res.getCount());
        List<String> variableNames = new ArrayList<>();
        variableNames.add("subj");
        variableNames.add("pred");
        variableNames.add("obj");
        assertEquals(variableNames, res.getVariableNames());

        queryString = "PREFIX dcterms: <http://purl.org/dc/terms/> " + "SELECT ?subj ?obj " + "WHERE {"
                + "      ?subj dcterms:References ?obj ." + "       }";
        res = graph.query(queryString, "sparql", null);
        assertSame(2, res.getCount());
        variableNames.remove("pred");
        assertEquals(variableNames, res.getVariableNames());
    }

    @Test
    public void testRead() throws Exception {
        InputStream in = new FileInputStream(getTestFile());
        assertSame(0L, graph.size());
        graph.read(in, null, null);
        assertNotSame(0L, graph.size());
        List<Statement> statements = graph.getStatements();
        Collections.sort(statements);
        // assertSame(statements.size(), this.statements.size());
        // for (int i = 0; i < statements.size(); i++) {
        // assertEquals(statements.get(i), this.statements.get(i));
        // }
        assertEquals(statements, this.statements);
    }

    @Test
    public void testReadPath() {
        assertSame(0L, graph.size());
        graph.read(getTestFile(), null, null);
        assertNotSame(0L, graph.size());
        List<Statement> statements = graph.getStatements();
        Collections.sort(statements);
        // assertSame(statements.size(), this.statements.size());
        // for (int i = 0; i < statements.size(); i++) {
        // assertEquals(statements.get(i), this.statements.get(i));
        // }
        assertEquals(statements, this.statements);
    }

    @Test
    public void testWrite() throws Exception {
        graph.add(statements);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        graph.write(out, null, null);
        try (InputStream written = new ByteArrayInputStream(out.toByteArray());
                InputStream expected = new FileInputStream(getTestFile())) {
            assertEquals(IOUtils.toString(expected, Charsets.UTF_8).replaceAll("\r?\n", ""),
                    IOUtils.toString(written, Charsets.UTF_8).replaceAll("\r?\n", ""));
        }
    }

    @Test
    public void testWritePath() throws Exception {
        graph.add(statements);
        File file = Framework.createTempFile("test", ".rdf");
        String path = file.getPath();
        graph.write(path, null, null);
        try (InputStream written = new FileInputStream(new File(path));
                InputStream expected = new FileInputStream(getTestFile())) {
            String expectedString = IOUtils.toString(expected, Charsets.UTF_8).replaceAll("\r?\n", "");
            String writtenString = IOUtils.toString(written, Charsets.UTF_8).replaceAll("\r?\n", "");
            assertEquals(expectedString, writtenString);
        }
    }

    // XXX AT: test serialization of the graph because the RelationServiceBean
    // will attempt to keep references to graphs it manages.
    @Test
    public void testSerialization() throws Exception {
        graph.add(statements);

        // serialize
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(out);
        oos.writeObject(graph);
        oos.close();
        assertTrue(out.toByteArray().length > 0);

        // deserialize
        byte[] pickled = out.toByteArray();
        InputStream in = new ByteArrayInputStream(pickled);
        ObjectInputStream ois = new ObjectInputStream(in);
        Object o = ois.readObject();

        JenaGraph newGraph = (JenaGraph) o;

        // new graph has same properties than old one but statements are lost
        // because they were stored in a memory graph
        assertSame(0L, newGraph.size());
        Model newModel = newGraph.openGraph().getGraph();
        Map<String, String> map = newModel.getNsPrefixMap();
        assertEquals("http://www.w3.org/1999/02/22-rdf-syntax-ns#", map.get("rdf"));
        assertEquals("http://purl.org/dc/terms/", map.get("dcterms"));
    }
}
