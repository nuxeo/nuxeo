/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.introspection.graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.api.graph.EDGE_TYPE;
import org.nuxeo.apidoc.api.graph.Graph;
import org.nuxeo.apidoc.api.graph.NODE_CATEGORY;
import org.nuxeo.apidoc.api.graph.NODE_TYPE;
import org.nuxeo.apidoc.api.graph.NetworkGraphGenerator;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;

/**
 * Basic implementation relying on introspection of distribution.
 * 
 * @since 11.1
 */
public class BasicGraphGeneratorImpl implements NetworkGraphGenerator {

    protected final String graphId;

    protected final DistributionSnapshot distribution;

    public BasicGraphGeneratorImpl(String graphId, DistributionSnapshot distribution) {
        super();
        this.graphId = graphId;
        this.distribution = distribution;
    }

    public static Graph getGraph(String graphId, DistributionSnapshot distribution) {
        BasicGraphGeneratorImpl gen = new BasicGraphGeneratorImpl(graphId, distribution);
        return gen.getGraph();
    }

    @Override
    public Graph getGraph() {
        GraphImpl graph = new GraphImpl(graphId);

        // introspect the graph, ignore bundle groups but select:
        // - bundles
        // - components
        // - extension points declarations
        // - services
        // - contributions
        List<String> bids = distribution.getBundleIds();
        for (String bid : bids) {
            BundleInfo bundle = distribution.getBundle(bid);
            // add node for bundle
            NODE_CATEGORY cat = NODE_CATEGORY.getCategory(bundle);
            graph.addNode(new NodeImpl(bid, bid, 0, "", NODE_TYPE.BUNDLE.name(), cat.name(), cat.getColor()));
            // compute sub components
            List<ComponentInfo> components = bundle.getComponents();
            for (ComponentInfo component : components) {
                String compid = component.getId();
                graph.addNode(new NodeImpl(compid, compid, 0, component.getHierarchyPath(), NODE_TYPE.COMPONENT.name(),
                        cat.name(), cat.getColor()));
                graph.addEdge(new EdgeImpl(bid, compid, EDGE_TYPE.REFERENCES.name()));
                for (ServiceInfo service : component.getServices()) {
                    if (service.isOverriden()) {
                        continue;
                    }
                    String sid = service.getId();
                    graph.addNode(new NodeImpl(sid, sid, 0, service.getHierarchyPath(), NODE_TYPE.SERVICE.name(),
                            cat.name(), cat.getColor()));
                    graph.addEdge(new EdgeImpl(compid, sid, EDGE_TYPE.REFERENCES.name()));
                }

                for (ExtensionPointInfo xp : component.getExtensionPoints()) {
                    String xpid = xp.getId();
                    graph.addNode(new NodeImpl(xpid, xpid, 0, xp.getHierarchyPath(), NODE_TYPE.EXTENSION_POINT.name(),
                            cat.name(), cat.getColor()));
                    graph.addEdge(new EdgeImpl(compid, xpid, EDGE_TYPE.REFERENCES.name()));
                }

                Map<String, Integer> comps = new HashMap<String, Integer>();
                for (ExtensionInfo contribution : component.getExtensions()) {
                    // handle multiple contributions to the same extension point
                    String cid = contribution.getId();
                    if (comps.containsKey(cid)) {
                        Integer num = comps.get(cid);
                        comps.put(cid, num + 1);
                        cid += "-" + String.valueOf(num + 1);
                    } else {
                        comps.put(cid, Integer.valueOf(0));
                    }

                    // add link to corresponding component
                    graph.addNode(new NodeImpl(cid, cid, 0, contribution.getHierarchyPath(),
                            NODE_TYPE.CONTRIBUTION.name(), cat.name(), cat.getColor()));
                    graph.addEdge(new EdgeImpl(compid, cid, EDGE_TYPE.REFERENCES.name()));

                    // also add link to target extension point, "guessing" the extension point id
                    String targetId = contribution.getTargetComponentName() + "--" + contribution.getExtensionPoint();
                    graph.addEdge(new EdgeImpl(targetId, cid, EDGE_TYPE.REFERENCES.name()));
                }
            }
        }

        return graph;
    }

}
