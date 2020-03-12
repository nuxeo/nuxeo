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
import java.util.Arrays;
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
import org.nuxeo.apidoc.api.graph.EditableGraph;
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
public abstract class AbstractGraphGeneratorImpl implements GraphGenerator {

    protected String name;

    protected Map<String, String> properties = new HashMap<>();

    public AbstractGraphGeneratorImpl() {
        super();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
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
    public List<Graph> getGraphs(DistributionSnapshot distribution) {
        EditableGraph g = getDefaultGraph(distribution);
        return Arrays.asList(g);
    }

    protected EditableGraph createDefaultGraph() {
        EditableGraph graph = new GraphImpl(getName());
        graph.setType(GRAPH_TYPE.BASIC.name());
        graph.setProperties(getProperties());
        return graph;
    }

    protected EditableGraph getDefaultGraph(DistributionSnapshot distribution) {
        EditableGraph graph = createDefaultGraph();

        // introspect the graph, ignore bundle groups but select:
        // - bundles
        // - components
        // - extension points declarations
        // - services
        // - contributions
        Map<String, Integer> hits = new HashMap<>();

        for (String bid : distribution.getBundleIds()) {
            BundleInfo bundle = distribution.getBundle(bid);
            NODE_CATEGORY cat = NODE_CATEGORY.guessCategory(bundle);
            Node bundleNode = processBundle(distribution, graph, hits, bundle, cat);
            // compute sub components
            List<ComponentInfo> components = bundle.getComponents();
            for (ComponentInfo component : components) {
                processComponent(distribution, graph, hits, bundleNode, cat, component, true, true, true);
            }
        }

        refine(graph, hits);

        return graph;
    }

    protected Node processBundle(DistributionSnapshot distribution, EditableGraph graph, Map<String, Integer> hits,
            BundleInfo bundle, NODE_CATEGORY cat) {
        Node bundleNode = createBundleNode(bundle, cat);
        graph.addNode(bundleNode);
        // compute requirements
        for (String requirement : bundle.getRequirements()) {
            addEdge(graph, hits, createEdge(bundleNode, createNode(NODE_TYPE.BUNDLE.prefix(requirement)),
                    EDGE_TYPE.REQUIRES.name()));
        }
        if (bundle.getRequirements().isEmpty()) {
            requireBundleRootNode(graph, hits, bundleNode);
        }
        return bundleNode;
    }

    protected Node processComponent(DistributionSnapshot distribution, EditableGraph graph, Map<String, Integer> hits,
            Node bundleNode, NODE_CATEGORY category, ComponentInfo component, boolean processServices,
            boolean processExtensionPoints, boolean processExtensions) {
        Node compNode = createComponentNode(component, category);
        graph.addNode(compNode);
        // do not influence the bundle weight based on its contributions as the layout will take them into account to
        // generate the "ground" bundles graph layout: hits map is ignored here
        addEdge(graph, null, createEdge(bundleNode, compNode, EDGE_TYPE.CONTAINS.name()));
        if (processServices) {
            for (ServiceInfo service : component.getServices()) {
                if (service.isOverriden()) {
                    continue;
                }
                Node serviceNode = createServiceNode(service, category);
                graph.addNode(serviceNode);
                addEdge(graph, hits, createEdge(compNode, serviceNode, EDGE_TYPE.CONTAINS.name()));
            }
        }

        if (processExtensionPoints) {
            for (ExtensionPointInfo xp : component.getExtensionPoints()) {
                Node xpNode = createXPNode(xp, category);
                graph.addNode(xpNode);
                addEdge(graph, hits, createEdge(compNode, xpNode, EDGE_TYPE.CONTAINS.name()));
            }
        }

        if (processExtensions) {
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

                Node contNode = createContributionNode(contribution, category);
                graph.addNode(contNode);
                // add link to corresponding component
                addEdge(graph, hits, createEdge(compNode, contNode, EDGE_TYPE.CONTAINS.name()));

                // also add link to target extension point, "guessing" the extension point id, not counting for
                // hits
                String targetId = NODE_TYPE.EXTENSION_POINT.prefix(contribution.getExtensionPoint());
                addEdge(graph, null, createEdge(createNode(targetId), contNode, EDGE_TYPE.REFERENCES.name()));

            }
        }

        // compute requirements
        for (String requirement : component.getRequirements()) {
            addEdge(graph, hits, createEdge(compNode, createNode(NODE_TYPE.COMPONENT.prefix(requirement)),
                    EDGE_TYPE.SOFT_REQUIRES.name()));
        }

        return compNode;
    }

    protected String getBundleRootId() {
        return NODE_TYPE.BUNDLE.prefix(BundleInfo.RUNTIME_ROOT_PSEUDO_BUNDLE);
    }

    protected void requireBundleRootNode(EditableGraph graph, Map<String, Integer> hits, Node bundleNode) {
        // make it require the root node, unless we're handling the root itself
        String rootId = getBundleRootId();
        if (!rootId.equals(bundleNode.getId())) {
            addEdge(graph, hits, createEdge(bundleNode, createNode(rootId), EDGE_TYPE.REQUIRES.name()));
        }
    }

    /**
     * Refines graphs for display.
     * <ul>
     * <li>Adds potentially missing bundle root
     * <li>Adds potentially missing nodes referenced in edges
     * <li>sets weights computed from hits map
     */
    protected void refine(EditableGraph graph, Map<String, Integer> hits) {
        // handle missing root
        String rrId = getBundleRootId();
        Node rrNode = graph.getNode(rrId);
        if (rrNode == null) {
            rrNode = createNode(rrId, BundleInfo.RUNTIME_ROOT_PSEUDO_BUNDLE, 0, "", NODE_TYPE.BUNDLE.name(),
                    NODE_CATEGORY.RUNTIME.name());
            graph.addNode(rrNode);
        }
        // handle missing references
        List<Node> orphans = new ArrayList<Node>();
        for (Edge edge : graph.getEdges()) {
            Node source = graph.getNode(edge.getSource());
            if (source == null) {
                source = addMissingNode(graph, edge.getSource(), 1);
            }
            Node target = graph.getNode(edge.getTarget());
            if (target == null) {
                target = addMissingNode(graph, edge.getTarget(), 1);
                if (NODE_TYPE.BUNDLE.name().equals(target.getType())
                        && EDGE_TYPE.REQUIRES.name().equals(edge.getValue())) {
                    orphans.add(target);
                }
            }
        }
        // handle orphan bundles
        for (Node orphan : orphans) {
            requireBundleRootNode(graph, hits, orphan);
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

    protected Node addMissingNode(EditableGraph graph, String id, int weight) {
        // try to guess category and type according to prefix
        NODE_CATEGORY cat = NODE_CATEGORY.PLATFORM;
        NODE_TYPE type = NODE_TYPE.guess(id);
        String unprefixedId = type.unprefix(id);
        cat = NODE_CATEGORY.guess(unprefixedId);
        Node node = createNode(id, unprefixedId, 0, "", type.name(), cat.name());
        node.setWeight(weight);
        graph.addNode(node);
        return node;
    }

    protected Node createNode(String id, String label, int weight, String path, String type, String category) {
        return new NodeImpl(id, label, weight, path, type, category);
    }

    protected Node createNode(String id) {
        return createNode(id, id, 0, null, null, null);
    }

    protected Node createBundleNode(BundleInfo bundle, NODE_CATEGORY cat) {
        String bid = bundle.getId();
        String pbid = NODE_TYPE.BUNDLE.prefix(bid);
        return createNode(pbid, bid, 0, "", NODE_TYPE.BUNDLE.name(), cat.name());
    }

    protected Node createComponentNode(ComponentInfo component, NODE_CATEGORY cat) {
        String compid = component.getId();
        String pcompid = NODE_TYPE.COMPONENT.prefix(compid);
        return createNode(pcompid, compid, 0, component.getHierarchyPath(), NODE_TYPE.COMPONENT.name(), cat.name());
    }

    protected Node createServiceNode(ServiceInfo service, NODE_CATEGORY cat) {
        String sid = service.getId();
        String psid = NODE_TYPE.SERVICE.prefix(sid);
        return createNode(psid, sid, 0, service.getHierarchyPath(), NODE_TYPE.SERVICE.name(), cat.name());
    }

    protected Node createXPNode(ExtensionPointInfo xp, NODE_CATEGORY cat) {
        String xpid = xp.getId();
        String pxpid = NODE_TYPE.EXTENSION_POINT.prefix(xpid);
        return createNode(pxpid, xpid, 0, xp.getHierarchyPath(), NODE_TYPE.EXTENSION_POINT.name(), cat.name());
    }

    protected Node createContributionNode(ExtensionInfo contribution, NODE_CATEGORY cat) {
        String cid = contribution.getId();
        String pcid = NODE_TYPE.CONTRIBUTION.prefix(cid);
        return createNode(pcid, cid, 0, contribution.getHierarchyPath(), NODE_TYPE.CONTRIBUTION.name(), cat.name());
    }

    protected Edge createEdge(Node source, Node target, String value) {
        return new EdgeImpl(source.getId(), target.getId(), value);
    }

    protected void addEdge(EditableGraph graph, Map<String, Integer> hits, Edge edge) {
        graph.addEdge(edge);
        if (hits != null) {
            hit(hits, edge.getSource());
            hit(hits, edge.getTarget());
        }
    }

    protected void hit(Map<String, Integer> hits, String source) {
        if (hits.containsKey(source)) {
            hits.put(source, hits.get(source) + 1);
        } else {
            hits.put(source, Integer.valueOf(1));
        }
    }

    protected void setWeight(EditableGraph graph, String nodeId, int hit) {
        Node node = graph.getNode(nodeId);
        if (node != null) {
            node.setWeight(hit);
        }
    }

}
