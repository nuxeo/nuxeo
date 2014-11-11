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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.relations.jena;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.relations.CoreGraph;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.impl.QNameResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;
import org.nuxeo.ecm.platform.relations.services.RelationService;
import org.nuxeo.runtime.api.Framework;

public class TestJenaOrCoreGraphFactory extends SQLRepositoryTestCase {

    public static final String DC_TERMS_NS = "http://purl.org/dc/terms/";

    private RelationService service;

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
                "jena-or-core-test-contrib.xml");
        service = (RelationService) Framework.getService(RelationManager.class);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        JenaOrCoreGraphFactory.testJenaGraph = null;
        super.tearDown();
    }

    @Test
    public void testJenaOrCoreGraph() throws Exception {
        // open graph, will be core
        Graph coreGraph = service.getGraphByName("jenaorcoregraph");
        assertEquals(CoreGraph.class, coreGraph.getClass());
        assertTrue(service.graphFactories.containsKey("jenaorcoregraph"));

        // jena graph (in-memory)
        Graph jenaGraph = service.getGraphByName("jenagraph");
        assertEquals(JenaGraph.class, jenaGraph.getClass());
        assertTrue(service.graphRegistry.containsKey("jenagraph"));

        // put some stuff in the graph
        QNameResource doc = new QNameResourceImpl(
                RelationConstants.DOCUMENT_NAMESPACE, database.repositoryName
                        + "/00010000-2c86-46fa-909e-02494bcb0001");
        QNameResource isBasedOn = new QNameResourceImpl(DC_TERMS_NS,
                "IsBasedOn");
        jenaGraph.add(new StatementImpl(doc, isBasedOn, doc));

        // reuse in-memory jena graph for next lookup
        JenaOrCoreGraphFactory.testJenaGraph = (JenaGraph) jenaGraph;

        // non-empty graph detected and used
        Graph jenaGraph2 = service.getGraphByName("jenaorcoregraph2");
        assertEquals(JenaGraph.class, jenaGraph2.getClass());
        assertEquals(jenaGraph, jenaGraph2);
        assertFalse(service.graphFactories.containsKey("jenaorcoregraph2"));
        assertTrue(service.graphRegistry.containsKey("jenaorcoregraph2"));
    }

}
