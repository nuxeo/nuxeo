/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 */

package org.nuxeo.ecm.platform.relations.io.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.io.DocumentTranslationMap;
import org.nuxeo.ecm.core.io.impl.DocumentTranslationMapImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.io.api.IOManager;
import org.nuxeo.ecm.platform.io.api.IOResourceAdapter;
import org.nuxeo.ecm.platform.io.api.IOResources;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.QNameResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.io.IORelationResources;
import org.nuxeo.ecm.platform.relations.jena.JenaGraph;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * Test layout component extension points.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@LocalDeploy({ "org.nuxeo.ecm.relations.io.tests:io-test-framework.xml", //
        "org.nuxeo.ecm.relations.io.tests:io-relations-test-contrib.xml", //
        "org.nuxeo.ecm.relations.io.tests:jena-test-bundle.xml", //
})
public class TestIORelationAdapter {

    private static final String graphName = "myrelations";

    private static final String documentNamespace = "http://www.ecm.org/uid/";

    private static final String predicateNamespace = "http://purl.org/dc/terms/";

    private static final QNameResource isBasedOn = new QNameResourceImpl(predicateNamespace, "IsBasedOn");

    private static final QNameResource references = new QNameResourceImpl(predicateNamespace, "References");

    private static final String doc1Ref = "DOC200600013_02.01";

    private static final String doc1RefCopy = "DOC200600013_02.01_copy";

    private static final QNameResource doc1Resource = new QNameResourceImpl(documentNamespace,
            "test/DOC200600013_02.01");

    private static final QNameResource doc2Resource = new QNameResourceImpl(documentNamespace,
            "test/DOC200600015_01.00");

    private static final QNameResource doc1ResourceCopy = new QNameResourceImpl(documentNamespace,
            "test/DOC200600013_02.01_copy");

    private static final Resource simpleResource = new ResourceImpl(
            "http://www.wikipedia.com/Enterprise_Content_Management");

    @Inject
    protected CoreSession session;

    @Inject
    private IOManager ioService;

    @Inject
    private RelationManager rService;

    private JenaGraph graph;

    private String repoName;

    @Before
    public void setUp() throws Exception {
        Graph graph = rService.getGraphByName(graphName);
        assertNotNull(graph);
        assertEquals(JenaGraph.class, graph.getClass());
        this.graph = (JenaGraph) graph;

        createDocuments();
        repoName = session.getRepositoryName();
    }

    private void createDocuments() {
        String type = "File";
        String id1 = doc1Ref;
        String id2 = doc1RefCopy;
        String name = "file1";
        String parentPath = "/";
        DocumentModel doc1 = new DocumentModelImpl((String) null, type, id1, new Path(name), null, null, new PathRef(
                parentPath), null, null, null, session.getRepositoryName());
        DocumentModel doc2 = new DocumentModelImpl((String) null, type, id2, new Path(name), null, null, new PathRef(
                parentPath), null, null, null, session.getRepositoryName());
        session.importDocuments(Arrays.asList(doc1, doc2));
        session.save();
    }

    @After
    public void tearDown() throws Exception {
        if (graph != null) {
            graph.clear();
        }
    }

    private static InputStream getTestFile(String filePath) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath);
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
        assertEquals(0, graph.size().longValue());
        graph.read(getTestFile(filePath), null, null);
        assertNotEquals(0, graph.size().longValue());
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

    @Test
    public void testIOResourceAdapterEP() {
        IOResourceAdapter adapter = ioService.getAdapter("all");
        assertNotNull(adapter);
        adapter = ioService.getAdapter("ignore-external");
        assertNotNull(adapter);
    }

    @Test
    public void testExtractResources() throws Exception {
        feedGraph("data/initial_statements.xml", graph);
        IOResourceAdapter adapter = ioService.getAdapter("all");
        assertNotNull(adapter);
        List<DocumentRef> sources = Arrays.asList(new DocumentRef[] { new IdRef(doc1Ref) });
        IORelationResources ioRes = (IORelationResources) adapter.extractResources(repoName, sources);
        List<Statement> expected = Arrays.asList(new Statement[] {
                new StatementImpl(doc2Resource, isBasedOn, doc1Resource),
                new StatementImpl(doc1Resource, references, simpleResource) });
        Collections.sort(expected);
        assertEquals(2, ioRes.getStatements().size());
        List<Statement> actual = ioRes.getStatements();
        Collections.sort(actual);
        assertEquals(expected, actual);
    }

    @Test
    public void testExtractResourcesIgnoreExternal() throws Exception {
        feedGraph("data/initial_statements.xml", graph);
        IOResourceAdapter adapter = ioService.getAdapter("ignore-external");
        assertNotNull(adapter);
        List<DocumentRef> sources = Arrays.asList(new DocumentRef[] { new IdRef(doc1Ref) });
        IORelationResources ioRes = (IORelationResources) adapter.extractResources(repoName, sources);
        List<Statement> expected = Arrays.asList(new Statement[] { new StatementImpl(doc1Resource, references,
                simpleResource) });
        Collections.sort(expected);
        assertEquals(1, ioRes.getStatements().size());
        List<Statement> actual = ioRes.getStatements();
        Collections.sort(actual);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetResourcesAsXML() throws Exception {
        feedGraph("data/initial_statements.xml", graph);
        IOResourceAdapter adapter = ioService.getAdapter("all");
        assertNotNull(adapter);
        List<DocumentRef> sources = Arrays.asList(new DocumentRef[] { new IdRef(doc1Ref) });
        IORelationResources ioRes = (IORelationResources) adapter.extractResources(repoName, sources);
        // File tmp = Framework.createTempFile("test", ".xml");
        // OutputStream out = new FileOutputStream(tmp);
        // adapter.getResourcesAsXML(out, ioRes);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        adapter.getResourcesAsXML(out, ioRes);
        InputStream actual = new ByteArrayInputStream(out.toByteArray());
        Graph actualGraph = getMemoryGraph();
        actualGraph.read(actual, null, null);
        compareGraph("data/exported_statements.xml", actualGraph);
    }

    @Test
    public void testLoadResourcesFromXML() throws Exception {
        IOResourceAdapter adapter = ioService.getAdapter("all");
        assertNotNull(adapter);
        InputStream stream = getTestFile("data/exported_statements.xml");
        IORelationResources ioRes = (IORelationResources) adapter.loadResourcesFromXML(stream);
        stream.close();
        List<Statement> expected = Arrays.asList(new Statement[] {
                new StatementImpl(doc2Resource, isBasedOn, doc1Resource),
                new StatementImpl(doc1Resource, references, simpleResource) });
        Collections.sort(expected);
        assertEquals(2, ioRes.getStatements().size());
        List<Statement> actual = ioRes.getStatements();
        Collections.sort(actual);
        assertEquals(expected, actual);
    }

    @Test
    public void testTranslateResources() throws Exception {
        feedGraph("data/initial_statements.xml", graph);
        IOResourceAdapter adapter = ioService.getAdapter("all");
        assertNotNull(adapter);
        List<DocumentRef> sources = Arrays.asList(new DocumentRef[] { new IdRef(doc1Ref) });
        IOResources resources = adapter.extractResources(repoName, sources);
        Map<DocumentRef, DocumentRef> docRefMap = new HashMap<DocumentRef, DocumentRef>();
        docRefMap.put(new IdRef(doc1Ref), new IdRef(doc1RefCopy));
        DocumentTranslationMap map = new DocumentTranslationMapImpl(repoName, repoName, docRefMap);
        IORelationResources ioRes = (IORelationResources) adapter.translateResources(repoName, resources, map);
        List<Statement> expected = Arrays.asList(new Statement[] {
                new StatementImpl(doc2Resource, isBasedOn, doc1ResourceCopy),
                new StatementImpl(doc1ResourceCopy, references, simpleResource) });
        Collections.sort(expected);
        assertEquals(2, ioRes.getStatements().size());
        List<Statement> actual = ioRes.getStatements();
        Collections.sort(actual);
        assertEquals(expected, actual);
    }

    @Test
    public void testTranslateResourcesNoChanges() throws Exception {
        feedGraph("data/initial_statements.xml", graph);
        IOResourceAdapter adapter = ioService.getAdapter("all");
        assertNotNull(adapter);
        List<DocumentRef> sources = Arrays.asList(new DocumentRef[] { new IdRef(doc1Ref) });
        IOResources resources = adapter.extractResources(repoName, sources);
        Map<DocumentRef, DocumentRef> docRefMap = new HashMap<DocumentRef, DocumentRef>();
        docRefMap.put(new IdRef(doc1Ref), new IdRef(doc1Ref));
        DocumentTranslationMap map = new DocumentTranslationMapImpl(repoName, repoName, docRefMap);
        IORelationResources ioRes = (IORelationResources) adapter.translateResources(repoName, resources, map);
        List<Statement> expected = Arrays.asList(new Statement[] {
                new StatementImpl(doc2Resource, isBasedOn, doc1Resource),
                new StatementImpl(doc1Resource, references, simpleResource) });
        Collections.sort(expected);
        assertEquals(2, ioRes.getStatements().size());
        List<Statement> actual = ioRes.getStatements();
        Collections.sort(actual);
        assertEquals(expected, actual);
    }

    @Test
    public void testStoreResources() throws Exception {
        feedGraph("data/initial_statements.xml", graph);
        IOResourceAdapter adapter = ioService.getAdapter("all");
        assertNotNull(adapter);
        List<DocumentRef> sources = Arrays.asList(new DocumentRef[] { new IdRef(doc1Ref) });
        IOResources resources = adapter.extractResources(repoName, sources);
        Map<DocumentRef, DocumentRef> docRefMap = new HashMap<DocumentRef, DocumentRef>();
        docRefMap.put(new IdRef(doc1Ref), new IdRef(doc1RefCopy));
        DocumentTranslationMap map = new DocumentTranslationMapImpl(repoName, repoName, docRefMap);
        IORelationResources ioRes = (IORelationResources) adapter.translateResources(repoName, resources, map);
        adapter.storeResources(ioRes);
        compareGraph("data/copied_statements.xml", graph);
    }

}
