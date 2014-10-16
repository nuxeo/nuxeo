/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.relations.jena;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Alexandre Russel
 *
 */
public class GraphLoadingTest extends NXRuntimeTestCase {

    private RelationManager service;

    private Graph graph;

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
        service = Framework.getService(RelationManager.class);
        assertNotNull(service);
        graph = service.getTransientGraph("jena");
        assertNotNull(graph);
    }

    @Test
    public void testGetStatement() {
        InputStream is = getClass().getResourceAsStream("/post-rdf.xml");
        assertNotNull(is);
        graph.clear();
        assertTrue(graph.getStatements().isEmpty());
        graph.read(is, null, null);
        assertNotNull(graph);
        List<Statement> lists = graph.getStatements();
        assertEquals(12, lists.size());

        // test blank nodes are distinct
        Set<Node> subjects = new HashSet<Node>();
        for (Statement s : lists) {
            subjects.add(s.getSubject());
        }
        assertEquals(2, subjects.size());
    }

    @Test
    public void testGetAllStatementWithURN() {
        InputStream is = getClass().getResourceAsStream(
                "/post-rdf-with-about.xml");
        assertNotNull(is);
        graph.clear();
        assertTrue(graph.getStatements().isEmpty());
        graph.read(is, null, null);
        assertNotNull(graph);
        List<Statement> lists = graph.getStatements();
        assertEquals(12, lists.size());
    }

    @Test
    public void testGetAllStatementWithURL() {
        InputStream is = getClass().getResourceAsStream(
                "/post-rdf-with-about-with-url.xml");
        assertNotNull(is);
        graph.clear();
        assertTrue(graph.getStatements().isEmpty());
        graph.read(is, null, null);
        assertNotNull(graph);
        List<Statement> lists = graph.getStatements();
        assertEquals(12, lists.size());
    }

}
