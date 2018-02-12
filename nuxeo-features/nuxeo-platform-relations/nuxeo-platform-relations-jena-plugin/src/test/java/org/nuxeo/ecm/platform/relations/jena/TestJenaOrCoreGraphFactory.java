/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.relations.jena;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.relations.CoreGraph;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.impl.QNameResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;
import org.nuxeo.ecm.platform.relations.services.RelationService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.relations")
@Deploy("org.nuxeo.ecm.relations.jena")
@Deploy("org.nuxeo.ecm.relations.jena.tests:jena-or-core-test-contrib.xml")
public class TestJenaOrCoreGraphFactory {

    public static final String DC_TERMS_NS = "http://purl.org/dc/terms/";

    @Inject
    private RelationManager service;

    @Inject
    protected CoreFeature coreFeature;

    @After
    public void tearDown() {
        JenaOrCoreGraphFactory.testJenaGraph = null;
    }

    @Test
    public void testJenaOrCoreGraph() throws Exception {
        RelationService serviceImpl = (RelationService) service;

        // open graph, will be core
        Graph coreGraph = service.getGraphByName("jenaorcoregraph");
        assertEquals(CoreGraph.class, coreGraph.getClass());
        assertTrue(serviceImpl.graphFactories.containsKey("jenaorcoregraph"));

        // jena graph (in-memory)
        Graph jenaGraph = service.getGraphByName("jenagraph");
        assertEquals(JenaGraph.class, jenaGraph.getClass());
        assertTrue(serviceImpl.graphRegistry.containsKey("jenagraph"));

        // put some stuff in the graph
        QNameResource doc = new QNameResourceImpl(RelationConstants.DOCUMENT_NAMESPACE,
                coreFeature.getRepositoryName() + "/00010000-2c86-46fa-909e-02494bcb0001");
        QNameResource isBasedOn = new QNameResourceImpl(DC_TERMS_NS, "IsBasedOn");
        jenaGraph.add(new StatementImpl(doc, isBasedOn, doc));

        // reuse in-memory jena graph for next lookup
        JenaOrCoreGraphFactory.testJenaGraph = (JenaGraph) jenaGraph;

        // TODO the following doesn't work anymore now that this test uses RuntimeFeature
        // which calls applicationStarted which initializes all graphs beforehand

        // non-empty graph detected and used
        // Graph jenaGraph2 = service.getGraphByName("jenaorcoregraph2");
        // assertEquals(JenaGraph.class, jenaGraph2.getClass());
        // assertEquals(jenaGraph, jenaGraph2);
        // assertFalse(serviceImpl.graphFactories.containsKey("jenaorcoregraph2"));
        // assertTrue(serviceImpl.graphRegistry.containsKey("jenaorcoregraph2"));
    }

}
