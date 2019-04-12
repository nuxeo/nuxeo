/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.relations.CoreGraphFactory;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.GraphDescription;
import org.nuxeo.ecm.platform.relations.api.GraphFactory;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.services.RelationService;
import org.nuxeo.runtime.api.Framework;

/**
 * A factory that detects if a Jena graph or a core graph should be used, and remembers it for future invocations.
 * <p>
 * A Jena graph is used if it contains at least one relation for the given graph.
 */
public class JenaOrCoreGraphFactory implements GraphFactory {

    private static final Log log = LogFactory.getLog(JenaOrCoreGraphFactory.class);

    // used for tests
    protected static JenaGraph testJenaGraph;

    @Override
    public Graph createGraph(GraphDescription graphDescription, CoreSession session) {
        RelationService service = (RelationService) Framework.getService(RelationManager.class);

        String name = graphDescription.getName();

        Graph graph;
        if (testJenaGraph == null) {
            graph = new JenaGraph();
        } else {
            // test mode, allows reuse of in-memory graph
            graph = testJenaGraph;
        }
        graph.setDescription(graphDescription);
        if (graph.size().longValue() > 0) {
            // Jena graph already contains data, use it
            service.graphFactories.remove(name);
            service.graphRegistry.put(name, graph);
            log.info("Graph " + name + " using Jena");
        } else {
            // use a core graph and remember this factory
            GraphFactory factory = new CoreGraphFactory();
            service.graphFactories.put(name, factory);
            graph = factory.createGraph(graphDescription, session);
            log.info("Graph " + name + " using Core");
        }
        return graph;
    }

}
