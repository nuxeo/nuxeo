/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.LiteralImpl;
import org.nuxeo.ecm.platform.relations.api.impl.QNameResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RestartFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, RestartFeature.class })
@Deploy("org.nuxeo.runtime.management")
@Deploy("org.nuxeo.ecm.core.schema")
@Deploy("org.nuxeo.ecm.core.api")
@Deploy("org.nuxeo.ecm.relations")
@Deploy("org.nuxeo.ecm.relations.jena")
@Deploy("org.nuxeo.ecm.relations.jena.tests:jena-test-bundle.xml")
public class TestJenaGraphReification {

    private JenaGraph graph;

    private List<Statement> statements;

    private String namespace;

    private Resource doc1;

    private Resource doc2;

    private QNameResource isBasedOn;

    private QNameResource references;

    private QNameResource createdBy;

    private Statement st1;

    private Statement st2;

    @Before
    public void setUp() throws Exception {
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
        createdBy = new QNameResourceImpl(namespace, "CreatedBy");
        st1 = new StatementImpl(doc2, isBasedOn, doc1);
        st1.addProperty(createdBy, new LiteralImpl("Omar"));
        st1.addProperty(createdBy, new LiteralImpl("Bodie"));
        st2 = new StatementImpl(doc1, references, new ResourceImpl(
                "http://www.wikipedia.com/Enterprise_Content_Management"));
        st2.addProperty(createdBy, new LiteralImpl("Prop Joe"));
        statements.add(st1);
        statements.add(st2);
    }

    private static File getTestFile() {
        String filePath = "test-reified.rdf";
        return FileUtils.getResourceFileFromContext(filePath);
    }

    private void compareStatements(List<Statement> first, List<Statement> second) {
        // ignore spurious statements representing properties that may be given
        // in the first list -> now useless, see NXP-1559
        for (Statement secondStmt : second) {
            assertTrue(first.contains(secondStmt));

            int index = first.indexOf(secondStmt);
            Statement firstStmt = first.get(index);
            assertEquals(firstStmt.getProperties().size(), secondStmt.getProperties().size());

            List<Node> firstProps = Arrays.asList(firstStmt.getProperties(createdBy));
            Collections.sort(firstProps);
            List<Node> secondProps = Arrays.asList(secondStmt.getProperties(createdBy));
            Collections.sort(secondProps);
            assertEquals(firstProps, secondProps);
        }
    }

    @Test
    public void testAdd() {
        assertEquals(0, graph.size().longValue());

        graph.add(statements);
        // 2 statements, 3 properties
        assertEquals(5, graph.size().longValue());
    }

    @Test
    public void testRemove() {
        assertEquals(0, graph.size().longValue());

        graph.add(statements);
        assertEquals(5, graph.size().longValue());

        List<Statement> stmts = new ArrayList<>();
        stmts.add(st1);
        graph.remove(stmts);
        // 1 statement, 1 property
        assertEquals(2, graph.size().longValue());
    }

    @Test
    public void testGetStatements() {
        List<Statement> stmts = new ArrayList<>();
        assertEquals(stmts, graph.getStatements());

        graph.add(statements);
        stmts = graph.getStatements();
        compareStatements(stmts, statements);
    }

    @Test
    public void testRead() throws Exception {
        InputStream in = new FileInputStream(getTestFile());
        assertEquals(0, graph.size().longValue());
        graph.read(in, null, null);
        assertNotEquals(0, graph.size().longValue());
        List<Statement> stmts = graph.getStatements();
        compareStatements(stmts, statements);
    }

    // cannot test write as serialization changes with blank nodes

}
