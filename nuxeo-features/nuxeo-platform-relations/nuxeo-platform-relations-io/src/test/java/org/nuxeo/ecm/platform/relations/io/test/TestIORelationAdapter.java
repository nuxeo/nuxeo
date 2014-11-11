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
 * $Id: TestIORelationAdapter.java 26500 2007-10-26 18:08:51Z fguillaume $
 */

package org.nuxeo.ecm.platform.relations.io.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.io.DocumentTranslationMap;
import org.nuxeo.ecm.core.io.impl.DocumentTranslationMapImpl;
import org.nuxeo.ecm.platform.io.api.IOManager;
import org.nuxeo.ecm.platform.io.api.IOResourceAdapter;
import org.nuxeo.ecm.platform.io.api.IOResources;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.Literal;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.LiteralImpl;
import org.nuxeo.ecm.platform.relations.api.impl.QNameResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.io.IORelationResources;
import org.nuxeo.ecm.platform.relations.jena.JenaGraph;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * Test layout component extension points.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
@SuppressWarnings({"RedundantArrayCreation"})
public class TestIORelationAdapter extends NXRuntimeTestCase {

    private static final String repoName = "demo";

    private static final String graphName = "myrelations";

    private static final String documentNamespace = "http://www.ecm.org/uid/";

    private static final String predicateNamespace = "http://purl.org/dc/terms/";

    private static final QNameResource isBasedOn = new QNameResourceImpl(
            predicateNamespace, "IsBasedOn");

    private static final QNameResource references = new QNameResourceImpl(
            predicateNamespace, "References");

    private static final String doc1Ref = "DOC200600013_02.01";

    private static final String doc1RefCopy = "DOC200600013_02.01_copy";

    private static final QNameResource doc1Resource = new QNameResourceImpl(
            documentNamespace, "demo/DOC200600013_02.01");

    private static final QNameResource doc2Resource = new QNameResourceImpl(
            documentNamespace, "demo/DOC200600015_01.00");

    private static final QNameResource doc1ResourceCopy = new QNameResourceImpl(
            documentNamespace, "demo/DOC200600013_02.01_copy");

    private static final Resource simpleResource = new ResourceImpl(
            "http://www.wikipedia.com/Enterprise_Content_Management");

    private static final Literal literal = new LiteralImpl("NXRuntime");

    private IOManager ioService;

    private RelationManager rService;

    private JenaGraph graph;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // fake repo setup
        deployContrib("org.nuxeo.ecm.relations.io.tests",
                "RepositoryManager.xml");
        deployContrib("org.nuxeo.ecm.relations.io.tests",
                "RepositoryService.xml");
        deployContrib("org.nuxeo.ecm.relations.io.tests", "FakeRepository.xml");
        deployContrib("org.nuxeo.ecm.relations.io.tests", "SecurityService.xml");

        // specific files
        deployContrib("org.nuxeo.ecm.relations.io.tests",
                "io-test-framework.xml");
        deployContrib("org.nuxeo.ecm.relations.io.tests",
                "io-relations-test-contrib.xml");
        deployContrib("org.nuxeo.ecm.relations.io.tests",
                "jena-test-bundle.xml");
        ioService = Framework.getService(IOManager.class);
        assertNotNull(ioService);
        assertNotNull(ioService);
        rService = Framework.getService(RelationManager.class);
        Graph graph = rService.getGraphByName(graphName);
        assertNotNull(graph);
        assertEquals(JenaGraph.class, graph.getClass());
        this.graph = (JenaGraph) graph;
    }

    @Override
    public void tearDown() throws Exception {
        if (graph != null) {
            graph.clear();
        }
        graph = null;
        super.tearDown();
    }

    private static InputStream getTestFile(String filePath) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(
                filePath);
    }

    private static JenaGraph getMemoryGraph() {
        JenaGraph graph = new JenaGraph();
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        namespaces.put("dcterms", "http://purl.org/dc/terms/");
        namespaces.put("uid", "http://www.ecm.org/uid/");
        namespaces.put("metadata", "http://www.ecm.org/metadata/");
        graph.setNamespaces(namespaces);
        return graph;
    }

    private static void feedGraph(String filePath, Graph graph) {
        assertSame(0L, graph.size());
        graph.read(getTestFile(filePath), null, null);
        assertNotSame(0L, graph.size());
    }

    private static void compareGraph(String filePath, Graph graph) {
        Graph newGraph = getMemoryGraph();
        feedGraph(filePath, newGraph);
        List<Statement> expected = newGraph.getStatements();
        Collections.sort(expected);
        List<Statement> actual = graph.getStatements();
        Collections.sort(actual);
        for (int i = 0; i < expected.size(); i++) {
            // compare each statement
            assertEquals(expected.get(i), actual.get(i));
            // compare properties
            Map<Resource, Node[]> expProps = expected.get(i).getProperties();
            Map<Resource, Node[]> actProps = actual.get(i).getProperties();
            assertEquals(expProps.size(), actProps.size());
            // TODO: compare values
            // assertEquals(expProps, actProps);
        }
    }

    public void testIOResourceAdapterEP() throws ClientException {
        IOResourceAdapter adapter = ioService.getAdapter("all");
        assertNotNull(adapter);
        adapter = ioService.getAdapter("ignore-external");
        assertNotNull(adapter);
    }

    public void testExtractResources() throws Exception {
        feedGraph("data/initial_statements.xml", graph);
        IOResourceAdapter adapter = ioService.getAdapter("all");
        assertNotNull(adapter);
        List<DocumentRef> sources = Arrays.asList(
                new DocumentRef[]{ new IdRef(doc1Ref) });
        IORelationResources ioRes = (IORelationResources) adapter.extractResources(
                repoName, sources);
        List<Statement> expected = Arrays.asList(new Statement[]{
                new StatementImpl(doc2Resource, isBasedOn, doc1Resource),
                new StatementImpl(doc1Resource, references, simpleResource) });
        Collections.sort(expected);
        assertEquals(2, ioRes.getStatements().size());
        List<Statement> actual = ioRes.getStatements();
        Collections.sort(actual);
        assertEquals(expected, actual);
    }

    public void testExtractResourcesIgnoreExternal() throws Exception {
        feedGraph("data/initial_statements.xml", graph);
        IOResourceAdapter adapter = ioService.getAdapter("ignore-external");
        assertNotNull(adapter);
        List<DocumentRef> sources = Arrays.asList(new DocumentRef[]{ new IdRef(
                doc1Ref) });
        IORelationResources ioRes = (IORelationResources) adapter.extractResources(
                repoName, sources);
        List<Statement> expected = Arrays.asList(new Statement[]{
                new StatementImpl(doc1Resource, references, simpleResource) });
        Collections.sort(expected);
        assertEquals(1, ioRes.getStatements().size());
        List<Statement> actual = ioRes.getStatements();
        Collections.sort(actual);
        assertEquals(expected, actual);
    }

    public void testGetResourcesAsXML() throws Exception {
        feedGraph("data/initial_statements.xml", graph);
        IOResourceAdapter adapter = ioService.getAdapter("all");
        assertNotNull(adapter);
        List<DocumentRef> sources = Arrays.asList(
                new DocumentRef[]{ new IdRef(doc1Ref) });
        IORelationResources ioRes = (IORelationResources) adapter.extractResources(
                repoName, sources);
        // File tmp = File.createTempFile("test", ".xml");
        // OutputStream out = new FileOutputStream(tmp);
        // adapter.getResourcesAsXML(out, ioRes);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        adapter.getResourcesAsXML(out, ioRes);
        InputStream actual = new ByteArrayInputStream(out.toByteArray());
        Graph actualGraph = getMemoryGraph();
        actualGraph.read(actual, null, null);
        compareGraph("data/exported_statements.xml", actualGraph);
    }

    public void testLoadResourcesFromXML() throws Exception {
        IOResourceAdapter adapter = ioService.getAdapter("all");
        assertNotNull(adapter);
        InputStream stream = getTestFile("data/exported_statements.xml");
        IORelationResources ioRes = (IORelationResources) adapter.loadResourcesFromXML(stream);
        List<Statement> expected = Arrays.asList(new Statement[]{
                new StatementImpl(doc2Resource, isBasedOn, doc1Resource),
                new StatementImpl(doc1Resource, references, simpleResource) });
        Collections.sort(expected);
        assertEquals(2, ioRes.getStatements().size());
        List<Statement> actual = ioRes.getStatements();
        Collections.sort(actual);
        assertEquals(expected, actual);
    }

    public void testTranslateResources() throws Exception {
        feedGraph("data/initial_statements.xml", graph);
        IOResourceAdapter adapter = ioService.getAdapter("all");
        assertNotNull(adapter);
        List<DocumentRef> sources = Arrays.asList(new DocumentRef[]{ new IdRef(
                doc1Ref) });
        IOResources resources = adapter.extractResources(repoName, sources);
        Map<DocumentRef, DocumentRef> docRefMap = new HashMap<DocumentRef, DocumentRef>();
        docRefMap.put(new IdRef(doc1Ref), new IdRef(doc1RefCopy));
        DocumentTranslationMap map = new DocumentTranslationMapImpl(repoName,
                repoName, docRefMap);
        IORelationResources ioRes = (IORelationResources) adapter.translateResources(
                repoName, resources, map);
        List<Statement> expected = Arrays.asList(new Statement[]{
                new StatementImpl(doc2Resource, isBasedOn, doc1ResourceCopy),
                new StatementImpl(doc1ResourceCopy, references, simpleResource) });
        Collections.sort(expected);
        assertEquals(2, ioRes.getStatements().size());
        List<Statement> actual = ioRes.getStatements();
        Collections.sort(actual);
        assertEquals(expected, actual);
    }

    public void testTranslateResourcesNoChanges() throws Exception {
        feedGraph("data/initial_statements.xml", graph);
        IOResourceAdapter adapter = ioService.getAdapter("all");
        assertNotNull(adapter);
        List<DocumentRef> sources = Arrays.asList(new DocumentRef[]{ new IdRef(
                doc1Ref) });
        IOResources resources = adapter.extractResources(repoName, sources);
        Map<DocumentRef, DocumentRef> docRefMap = new HashMap<DocumentRef, DocumentRef>();
        docRefMap.put(new IdRef(doc1Ref), new IdRef(doc1Ref));
        DocumentTranslationMap map = new DocumentTranslationMapImpl(repoName,
                repoName, docRefMap);
        IORelationResources ioRes = (IORelationResources) adapter.translateResources(
                repoName, resources, map);
        List<Statement> expected = Arrays.asList(new Statement[]{
                new StatementImpl(doc2Resource, isBasedOn, doc1Resource),
                new StatementImpl(doc1Resource, references, simpleResource) });
        Collections.sort(expected);
        assertEquals(2, ioRes.getStatements().size());
        List<Statement> actual = ioRes.getStatements();
        Collections.sort(actual);
        assertEquals(expected, actual);
    }

    public void testStoreResources() throws Exception {
        feedGraph("data/initial_statements.xml", graph);
        IOResourceAdapter adapter = ioService.getAdapter("all");
        assertNotNull(adapter);
        List<DocumentRef> sources = Arrays.asList(new DocumentRef[]{ new IdRef(
                doc1Ref) });
        IOResources resources = adapter.extractResources(repoName, sources);
        Map<DocumentRef, DocumentRef> docRefMap = new HashMap<DocumentRef, DocumentRef>();
        docRefMap.put(new IdRef(doc1Ref), new IdRef(doc1RefCopy));
        DocumentTranslationMap map = new DocumentTranslationMapImpl(repoName,
                repoName, docRefMap);
        IORelationResources ioRes = (IORelationResources) adapter.translateResources(
                repoName, resources, map);
        adapter.storeResources(ioRes);
        compareGraph("data/copied_statements.xml", graph);
    }

}
