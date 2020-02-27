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

import java.util.ArrayList;
import java.util.Collections;
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
import org.nuxeo.apidoc.api.graph.GRAPH_TYPE;
import org.nuxeo.apidoc.api.graph.Graph;
import org.nuxeo.apidoc.api.graph.GraphGenerator;
import org.nuxeo.apidoc.api.graph.NODE_CATEGORY;
import org.nuxeo.apidoc.api.graph.NODE_TYPE;
import org.nuxeo.apidoc.api.graph.Node;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;

/**
 * Basic implementation relying on introspection of distribution.
 * 
 * @since 11.1
 */
public class BasicGraphGeneratorImpl implements GraphGenerator {

    protected String graphName;

    protected Map<String, String> properties = new HashMap<>();

    public BasicGraphGeneratorImpl() {
        super();
    }

    @Override
    public String getGraphName() {
        return graphName;
    }

    @Override
    public void setGraphName(String name) {
        this.graphName = name;
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    @Override
    public void setProperties(Map<String, String> properties) {
        this.properties.clear();
        this.properties.putAll(properties);
    }

    @Override
    public Graph getGraph(DistributionSnapshot distribution) {
        GraphImpl graph = (GraphImpl) createGraph();

        // introspect the graph, ignore bundle groups but select:
        // - bundles
        // - components
        // - extension points declarations
        // - services
        // - contributions
        Map<String, Integer> hits = new HashMap<>();
        List<String> children = new ArrayList<>();

        for (String bid : distribution.getBundleIds()) {
            BundleInfo bundle = distribution.getBundle(bid);
            // add node for bundle
            NODE_CATEGORY cat = NODE_CATEGORY.getCategory(bundle);
            Node bundleNode = createBundleNode(bundle, cat);
            graph.addNode(bundleNode);
            // compute requirements
            for (String requirement : bundle.getRequirements()) {
                addEdge(graph, hits, createEdge(bundleNode, createNode(prefixId(BundleInfo.TYPE_NAME, requirement)),
                        EDGE_TYPE.REQUIRES.name()));
            }
            if (bundle.getRequirements().isEmpty()) {
                children.add(bid);
            }
            // compute sub components
            List<ComponentInfo> components = bundle.getComponents();
            for (ComponentInfo component : components) {
                Node compNode = createComponentNode(component, cat);
                graph.addNode(compNode);
                addEdge(graph, hits, createEdge(bundleNode, compNode, EDGE_TYPE.CONTAINS.name()));
                for (ServiceInfo service : component.getServices()) {
                    if (service.isOverriden()) {
                        continue;
                    }
                    Node serviceNode = createServiceNode(service, cat);
                    graph.addNode(serviceNode);
                    addEdge(graph, hits, createEdge(compNode, serviceNode, EDGE_TYPE.CONTAINS.name()));
                }

                for (ExtensionPointInfo xp : component.getExtensionPoints()) {
                    Node xpNode = createXPNode(xp, cat);
                    graph.addNode(xpNode);
                    addEdge(graph, hits, createEdge(compNode, xpNode, EDGE_TYPE.CONTAINS.name()));
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

                    Node contNode = createContributionNode(contribution, cat);
                    graph.addNode(contNode);
                    // add link to corresponding component
                    addEdge(graph, hits, createEdge(compNode, contNode, EDGE_TYPE.CONTAINS.name()));

                    // also add link to target extension point, "guessing" the extension point id, not counting for
                    // hits
                    String targetId = prefixId(ComponentInfo.TYPE_NAME,
                            contribution.getTargetComponentName() + "--" + contribution.getExtensionPoint());
                    addEdge(graph, null, createEdge(createNode(targetId), contNode, EDGE_TYPE.REFERENCES.name()));

                    // compute requirements
                    for (String requirement : component.getRequirements()) {
                        addEdge(graph, hits, createEdge(compNode,
                                createNode(prefixId(ComponentInfo.TYPE_NAME, requirement)), EDGE_TYPE.REQUIRES.name()));
                    }
                }
            }
        }

        // add common root for all roots in the bundle graph, as it is supposed to be a directed tree without cycles
        List<Node> roots = new ArrayList<>();
        for (Node node : graph.getNodes()) {
            if (!children.contains(node.getOriginalId())) {
                roots.add(node);
            }
        }
        if (roots.size() > 1) {
            // add a common root for all roots
            String pbid = prefixId(BundleInfo.TYPE_NAME, BundleInfo.RUNTIME_ROOT_PSEUDO_BUNDLE);
            Node bundleNode = createNode(pbid, BundleInfo.RUNTIME_ROOT_PSEUDO_BUNDLE, 0, "", NODE_TYPE.BUNDLE.name(),
                    NODE_CATEGORY.RUNTIME.name(), NODE_CATEGORY.RUNTIME.getColor());
            graph.addNode(bundleNode);
            for (Node root : roots) {
                addEdge(graph, hits, createEdge(root, bundleNode, EDGE_TYPE.REQUIRES.name()));
            }
        }

        refine(graph, hits);

        return graph;
    }

    /**
     * Refines graphs for display.
     * <ul>
     * <li>Adds potentially missing nodes referenced in edges
     * <li>set integer id on nodes and edges, as required by some rendering frameworks
     * <li>sets weights computed from hits map
     */
    protected void refine(GraphImpl graph, Map<String, Integer> hits) {
        // handle ids
        int itemIndex = 1;
        for (Node node : graph.getNodes()) {
            node.setId(itemIndex);
            itemIndex++;
        }
        for (Edge edge : graph.getEdges()) {
            edge.setId(itemIndex);
            itemIndex++;
            Node source = graph.getNode(edge.getOriginalSourceId());
            if (source == null) {
                source = addMissingNode(graph, edge.getOriginalSourceId(), itemIndex++, 1);
            }
            edge.setSource(source.getId());
            Node target = graph.getNode(edge.getOriginalTargetId());
            if (target == null) {
                target = addMissingNode(graph, edge.getOriginalTargetId(), itemIndex++, 1);
            }
            edge.setTarget(target.getId());
        }
        // handle weights
        if (hits == null) {
            for (Node node : graph.getNodes()) {
                if (node.getWeight() == 0) {
                    node.setWeight(1);
                }
            }
            return;
        }
        for (Map.Entry<String, Integer> hit : hits.entrySet()) {
            setWeight(graph, hit.getKey(), hit.getValue());
        }

    }

    protected Node addMissingNode(GraphImpl graph, String originalId, int id, int weight) {
        Node node = createNode(originalId);
        node.setId(id);
        node.setWeight(weight);
        graph.addNode(node);
        return node;
    }

    protected Graph createGraph() {
        return new GraphImpl(graphName, GRAPH_TYPE.BASIC.name(), getProperties());
    }

    protected Node createNode(String id, String label, int weight, String path, String type, String category,
            String color) {
        return new NodeImpl(id, label, weight, path, type, category, color);
    }

    protected Node createNode(String id) {
        return createNode(id, id, 0, null, null, null, null);
    }

    protected Node createBundleNode(BundleInfo bundle, NODE_CATEGORY cat) {
        String bid = bundle.getId();
        String pbid = prefixId(BundleInfo.TYPE_NAME, bid);
        return createNode(pbid, bid, 0, "", NODE_TYPE.BUNDLE.name(), cat.name(), cat.getColor());
    }

    protected Node createComponentNode(ComponentInfo component, NODE_CATEGORY cat) {
        String compid = component.getId();
        String pcompid = prefixId(ComponentInfo.TYPE_NAME, compid);
        return createNode(pcompid, compid, 0, component.getHierarchyPath(), NODE_TYPE.COMPONENT.name(), cat.name(),
                cat.getColor());
    }

    protected Node createServiceNode(ServiceInfo service, NODE_CATEGORY cat) {
        String sid = service.getId();
        String psid = prefixId(ServiceInfo.TYPE_NAME, sid);
        return createNode(psid, sid, 0, service.getHierarchyPath(), NODE_TYPE.SERVICE.name(), cat.name(),
                cat.getColor());
    }

    protected Node createXPNode(ExtensionPointInfo xp, NODE_CATEGORY cat) {
        String xpid = xp.getId();
        String pxpid = prefixId(ExtensionPointInfo.TYPE_NAME, xpid);
        return createNode(pxpid, xpid, 0, xp.getHierarchyPath(), NODE_TYPE.EXTENSION_POINT.name(), cat.name(),
                cat.getColor());
    }

    protected Node createContributionNode(ExtensionInfo contribution, NODE_CATEGORY cat) {
        String cid = contribution.getId();
        String pcid = prefixId(ExtensionInfo.TYPE_NAME, cid);
        return createNode(pcid, cid, 0, contribution.getHierarchyPath(), NODE_TYPE.CONTRIBUTION.name(), cat.name(),
                cat.getColor());
    }

    protected Edge createEdge(Node source, Node target, String value) {
        return new EdgeImpl(source.getOriginalId(), target.getOriginalId(), value);
    }

    protected void addEdge(Graph graph, Map<String, Integer> hits, Edge edge) {
        graph.addEdge(edge);
        if (hits != null) {
            hit(hits, edge.getOriginalSourceId());
            hit(hits, edge.getOriginalTargetId());
        }
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
