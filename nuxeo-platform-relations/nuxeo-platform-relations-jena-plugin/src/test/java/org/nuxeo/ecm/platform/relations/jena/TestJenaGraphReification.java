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
 * $Id: TestJenaGraphReification.java 25079 2007-09-18 14:49:05Z atchertchian $
 */

package org.nuxeo.ecm.platform.relations.jena;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

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
import org.nuxeo.runtime.test.NXRuntimeTestCase;

@SuppressWarnings({"IOResourceOpenedButNotSafelyClosed"})
public class TestJenaGraphReification extends NXRuntimeTestCase {

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

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.runtime.management");
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.relations");
        deployBundle("org.nuxeo.ecm.relations.jena");
        deployContrib("org.nuxeo.ecm.relations.jena.tests",
                "jena-test-bundle.xml");
        RelationManager service = Framework.getService(RelationManager.class);
        Graph graph = service.getGraphByName("myrelations");
        assertNotNull(graph);
        assertEquals(JenaGraph.class, graph.getClass());
        this.graph = (JenaGraph) graph;
        statements = new ArrayList<Statement>();
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
            assertSame(firstStmt.getProperties().size(),
                    secondStmt.getProperties().size());

            List<Node> firstProps = Arrays.asList(firstStmt.getProperties(createdBy));
            Collections.sort(firstProps);
            List<Node> secondProps = Arrays.asList(secondStmt.getProperties(createdBy));
            Collections.sort(secondProps);
            assertEquals(firstProps, secondProps);
        }
    }

    @Test
    public void testAdd() {
        assertSame(0L, graph.size());

        graph.add(statements);
        // 2 statements, 3 properties
        assertSame(5L, graph.size());
    }

    @Test
    public void testRemove() {
        assertSame(0L, graph.size());

        graph.add(statements);
        assertSame(5L, graph.size());

        List<Statement> stmts = new ArrayList<Statement>();
        stmts.add(st1);
        graph.remove(stmts);
        // 1 statement, 1 property
        assertSame(2L, graph.size());
    }

    @Test
    public void testGetStatements() {
        List<Statement> stmts = new ArrayList<Statement>();
        assertEquals(stmts, graph.getStatements());

        graph.add(statements);
        stmts = graph.getStatements();
        compareStatements(stmts, statements);
    }

    @Test
    public void testRead() throws Exception {
        InputStream in = new FileInputStream(getTestFile());
        assertSame(0L, graph.size());
        graph.read(in, null, null);
        assertNotSame(0L, graph.size());
        List<Statement> stmts = graph.getStatements();
        compareStatements(stmts, statements);
    }

    // cannot test write as serialization changes with blank nodes

}
