/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.relations.CoreGraphFactory;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.GraphDescription;
import org.nuxeo.ecm.platform.relations.api.GraphFactory;
import org.nuxeo.ecm.platform.relations.services.RelationService;
import org.nuxeo.runtime.api.Framework;

/**
 * A factory that detects if a Jena graph or a core graph should be used, and
 * remembers it for future invocations.
 * <p>
 * A Jena graph is used if it contains at least one relation for the given
 * graph.
 */
public class JenaOrCoreGraphFactory implements GraphFactory {

    private static final Log log = LogFactory.getLog(JenaOrCoreGraphFactory.class);

    // used for tests
    protected static JenaGraph testJenaGraph;

    @Override
    public Graph createGraph(GraphDescription graphDescription,
            CoreSession session) {
        RelationService service = (RelationService) Framework.getRuntime().getComponent(
                RelationService.NAME);
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
