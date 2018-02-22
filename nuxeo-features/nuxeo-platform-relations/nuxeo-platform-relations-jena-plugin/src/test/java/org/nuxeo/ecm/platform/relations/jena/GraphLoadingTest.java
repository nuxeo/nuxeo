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
 *     Alexandre Russel
 */
package org.nuxeo.ecm.platform.relations.jena;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @author Alexandre Russel
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.runtime.management")
@Deploy("org.nuxeo.ecm.core.schema")
@Deploy("org.nuxeo.ecm.core.api")
@Deploy("org.nuxeo.ecm.relations")
@Deploy("org.nuxeo.ecm.relations.jena")
@Deploy("org.nuxeo.ecm.relations.jena.tests:jena-test-bundle.xml")
public class GraphLoadingTest {

    @Inject
    RelationManager service;

    private Graph graph;

    @Before
    public void setUp() throws Exception {
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
        Set<Node> subjects = new HashSet<>();
        for (Statement s : lists) {
            subjects.add(s.getSubject());
        }
        assertEquals(2, subjects.size());
    }

    @Test
    public void testGetAllStatementWithURN() {
        InputStream is = getClass().getResourceAsStream("/post-rdf-with-about.xml");
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
        InputStream is = getClass().getResourceAsStream("/post-rdf-with-about-with-url.xml");
        assertNotNull(is);
        graph.clear();
        assertTrue(graph.getStatements().isEmpty());
        graph.read(is, null, null);
        assertNotNull(graph);
        List<Statement> lists = graph.getStatements();
        assertEquals(12, lists.size());
    }

}
