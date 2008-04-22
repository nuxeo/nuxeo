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

package org.nuxeo.ecm.platform.relations.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResources;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.ResourceAdapter;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.Subject;
import org.nuxeo.ecm.platform.relations.api.impl.LiteralImpl;
import org.nuxeo.ecm.platform.relations.api.impl.QNameResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.jena.JenaGraph;
import org.nuxeo.ecm.platform.relations.search.indexer.RelationIndexer;
import org.nuxeo.ecm.platform.relations.search.resources.indexing.RelationIndexableResourceImpl;
import org.nuxeo.ecm.platform.relations.services.RelationService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * In this test, we set relations in a Jena graph.
 * there are two documents, of type Article. Each document
 * can correspond to two RDF resources:
 * think of one for itself, and one for its
 * family (i.e., all versions).
 *
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 */
public class TestRelationIndexer extends NXRuntimeTestCase {

    RelationIndexer indexer;

    private Graph graph;

    private List<Statement> statements;

    private Resource doc1;

    private Resource doc1Fam;

    private Resource doc2;

    private QNameResource isBasedOn;

    private QNameResource references;

    private RelationService service;

    private Node uri1;

    private static final String baseNs =
        "http://nuxeo.org/nxrelations/test/search/";

    private static final String docNs = baseNs + "doc/";

    private static final String famNs = baseNs + "docfam/";

    private static final String predNs = "http://purl.org/dc/terms/";

    private static final ResourceAdapter adapter
        = new FakeRelationIndexerDocumentResourceAdapter();

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deploy("nxsearch-relations-test-framework.xml");
        deploy("nxsearch-relations-test-contrib.xml");
        // All-in-one for relation
        deploy("nxrelations-test-search-bundle.xml");

        indexer = new RelationIndexer();
        deploy("jena-test-bundle.xml");
        service = (RelationService) Framework.getRuntime()
                .getComponent(RelationService.NAME);
        Graph graph = service.getGraphByName("myrelations");
        assertNotNull(graph);
        assertEquals(JenaGraph.class, graph.getClass());

        this.graph = graph;

        statements = new ArrayList<Statement>();

        // leaves
        doc1 = new QNameResourceImpl(docNs, "DOC200600013_02.01");
        doc1Fam = new ResourceImpl(famNs + "DOC200600013");
        doc2 = new QNameResourceImpl(docNs, "DOC200600015_01.00");
        uri1 = new ResourceImpl(
            "http://www.wikipedia.com/Enterprise_Content_Management");

        // predicates
        isBasedOn = new QNameResourceImpl(predNs, "IsBasedOn");
        references = new QNameResourceImpl(predNs, "References");

        // statements
        statements.add(new StatementImpl(doc2, isBasedOn, doc1));
        statements.add(new StatementImpl(doc1Fam, references, uri1));
        statements.add(new StatementImpl(doc1, references,
                new ResourceImpl(
                        "http://www.wikipedia.com/" +
                        "Enterprise_Content_Management#Introduction")));
        statements.add(new StatementImpl(doc2, references, new LiteralImpl(
                "NXRuntime")));
    }

    public void testGetResourceConfs() throws Exception {
        DocumentModel dm = new DocumentModelImpl("Commentable");
        Set<IndexableResourceConf> confs = indexer.getResourceConfs(dm);

        assertNotNull(confs);
        assertEquals(1, confs.size());
        for (IndexableResourceConf conf: confs) {
            assertEquals("comments", conf.getName());
        }
    }

    private void addStatement(Subject subject, Resource predicate, Node object) {
        graph.add(Arrays.asList(
                (Statement) new StatementImpl(subject, predicate, object)));
    }

    private void addStatement(Statement s) {
        graph.add(Arrays.asList(s));
    }

    /**
     * If this test fails, don't go further and fix it prior to anything else.
     * @throws Exception
     */
    public void testConfiguration() throws Exception {
        DocumentModel doc1M = (DocumentModel) adapter
            .getResourceRepresentation(doc1);

        // preliminary verifications
        assertNotNull(indexer.getResourceConfs(doc1M));
        assertEquals(1, indexer.getResourceConfs(doc1M).size());

        Set<Resource> resources = service.getAllResources(doc1M);
        assertNotNull(resources);
        assertEquals(2, resources.size());
    }

    /**
     * One doc resource as subject, then as object.
     *
     * @throws Exception
     */
    public void testExtractResources1() throws Exception {
        Statement st = new StatementImpl(doc1, isBasedOn, doc2);
        addStatement(st);

        List<IndexableResources> iResources = indexer.extractResources(
                (DocumentModel) adapter.getResourceRepresentation(doc1));
        assertNotNull(iResources);
        assertEquals(1, iResources.size());

        RelationIndexableResourceImpl iResource = (RelationIndexableResourceImpl)
                iResources.get(0).getIndexableResources().get(0);
        assertEquals(st, iResource.getStatement());


        assertEquals("myrelations", iResource.getConfiguration().getName());

        iResources = indexer.extractResources(
                (DocumentModel) adapter.getResourceRepresentation(doc2));
        assertNotNull(iResources);
        assertEquals(1, iResources.size());
        iResource = (RelationIndexableResourceImpl)
            iResources.get(0).getIndexableResources().get(0);
        assertEquals(st, iResource.getStatement());
    }

    /** Uses the two namespaces and subject/object. **/
    public void testExtractResources2() throws Exception {
        addStatement(doc1, isBasedOn, doc2);
        addStatement(new StatementImpl(doc1Fam, isBasedOn, uri1));
        addStatement(doc2, references, doc1);

        List<IndexableResources> iResources;

        iResources = indexer.extractResources(
                (DocumentModel) adapter.getResourceRepresentation(doc1));
        assertNotNull(iResources);
        assertEquals(3, iResources.size());

        iResources = indexer.extractResources(
                (DocumentModel) adapter.getResourceRepresentation(doc2));
        assertNotNull(iResources);
        assertEquals(2, iResources.size());
    }

    /**
     * A doc as subject, an external uri resource as object.
     *
     * @throws Exception
     */
    public void testExtractResources3() throws Exception {
        Statement st = new StatementImpl(doc1, references, uri1);
        addStatement(st);

        List<IndexableResources> iResources = indexer.extractResources(
                (DocumentModel) adapter.getResourceRepresentation(doc1));
        assertNotNull(iResources);
        assertEquals(1, iResources.size());

        RelationIndexableResourceImpl iResource = (RelationIndexableResourceImpl)
                iResources.get(0).getIndexableResources().get(0);
        assertEquals(st, iResource.getStatement());
    }

    /**
     * A doc as object, an external uri resource as subject.
     *
     * @throws Exception
     */
    public void testExtractResources4() throws Exception {
        Statement st = new StatementImpl(uri1, references, doc1);
        addStatement(st);

        List<IndexableResources> iResources = indexer.extractResources(
                (DocumentModel) adapter.getResourceRepresentation(doc1));
        assertNotNull(iResources);
        assertEquals(1, iResources.size());

        RelationIndexableResourceImpl iResource = (RelationIndexableResourceImpl)
                iResources.get(0).getIndexableResources().get(0);
        assertEquals(st, iResource.getStatement());
    }

}
