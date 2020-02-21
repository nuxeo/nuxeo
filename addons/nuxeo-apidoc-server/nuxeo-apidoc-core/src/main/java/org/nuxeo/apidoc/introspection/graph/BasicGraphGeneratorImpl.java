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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.api.graph.EDGE_TYPE;
import org.nuxeo.apidoc.api.graph.Edge;
import org.nuxeo.apidoc.api.graph.Graph;
import org.nuxeo.apidoc.api.graph.NODE_CATEGORY;
import org.nuxeo.apidoc.api.graph.NODE_TYPE;
import org.nuxeo.apidoc.api.graph.NetworkGraphGenerator;
import org.nuxeo.apidoc.api.graph.Node;
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
        Map<String, Integer> hits = new HashMap<>();

        List<String> bids = distribution.getBundleIds();
        for (String bid : bids) {
            BundleInfo bundle = distribution.getBundle(bid);
            // add node for bundle
            NODE_CATEGORY cat = NODE_CATEGORY.getCategory(bundle);
            String pbid = prefixId(BundleInfo.TYPE_NAME, bid);
            graph.addNode(createNode(pbid, bid, 0, "", NODE_TYPE.BUNDLE.name(), cat.name(), cat.getColor()));
            // compute sub components
            List<ComponentInfo> components = bundle.getComponents();
            for (ComponentInfo component : components) {
                String compid = component.getId();
                String pcompid = prefixId(ComponentInfo.TYPE_NAME, compid);
                graph.addNode(createNode(pcompid, compid, 0, component.getHierarchyPath(), NODE_TYPE.COMPONENT.name(),
                        cat.name(), cat.getColor()));
                addEdge(graph, hits, createEdge(pbid, pcompid, EDGE_TYPE.CONTAINS.name()));
                for (ServiceInfo service : component.getServices()) {
                    if (service.isOverriden()) {
                        continue;
                    }
                    String sid = service.getId();
                    String psid = prefixId(ServiceInfo.TYPE_NAME, sid);
                    graph.addNode(createNode(psid, sid, 0, service.getHierarchyPath(), NODE_TYPE.SERVICE.name(),
                            cat.name(), cat.getColor()));
                    addEdge(graph, hits, createEdge(pcompid, psid, EDGE_TYPE.CONTAINS.name()));
                }

                for (ExtensionPointInfo xp : component.getExtensionPoints()) {
                    String xpid = xp.getId();
                    String pxpid = prefixId(ExtensionPointInfo.TYPE_NAME, xpid);
                    graph.addNode(createNode(pxpid, xpid, 0, xp.getHierarchyPath(), NODE_TYPE.EXTENSION_POINT.name(),
                            cat.name(), cat.getColor()));
                    addEdge(graph, hits, createEdge(pcompid, pxpid, EDGE_TYPE.CONTAINS.name()));
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

                    String pcid = prefixId(ExtensionInfo.TYPE_NAME, cid);
                    // add link to corresponding component
                    graph.addNode(createNode(pcid, cid, 0, contribution.getHierarchyPath(),
                            NODE_TYPE.CONTRIBUTION.name(), cat.name(), cat.getColor()));
                    addEdge(graph, hits, createEdge(pcompid, pcid, EDGE_TYPE.CONTAINS.name()));

                    // also add link to target extension point, "guessing" the extension point id
                    String targetId = prefixId(ComponentInfo.TYPE_NAME,
                            contribution.getTargetComponentName() + "--" + contribution.getExtensionPoint());
                    graph.addEdge(createEdge(targetId, pcid, EDGE_TYPE.REFERENCES.name()));
                }
            }
        }

        for (Map.Entry<String, Integer> hit : hits.entrySet()) {
            setWeight(graph, hit.getKey(), hit.getValue());
        }

        return graph;
    }

    protected Node createNode(String id, String label, int weight, String path, String type, String category,
            String color) {
        return new NodeImpl(id, label, weight, path, type, category, color);
    }

    protected Edge createEdge(String source, String target, String value) {
        return new EdgeImpl(source, target, value);
    }

    protected void addEdge(Graph graph, Map<String, Integer> hits, Edge edge) {
        graph.addEdge(edge);
        hit(hits, edge.getSource());
        hit(hits, edge.getTarget());
    }

    protected void hit(Map<String, Integer> hits, String source) {
        if (hits.containsKey(source)) {
            hits.put(source, hits.get(source) + 1);
        } else {
            hits.put(source, Integer.valueOf(1));
        }
    }

    protected void setWeight(Graph graph, String nodeId, int hit) {
        Node node = graph.getNode(nodeId);
        if (node != null) {
            node.setWeight(hit);
        }
    }

    /**
     * Prefix all ids assuming each id is unique within a given type, to avoid potential collisions.
     */
    protected String prefixId(String prefix, String id) {
        return prefix + "-" + id;
    }

}
