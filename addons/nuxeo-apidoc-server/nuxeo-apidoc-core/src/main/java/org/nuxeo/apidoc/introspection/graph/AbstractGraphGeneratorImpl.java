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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /**
     * Introspect the graph, ignoring bundle groups but selecting all other nodes.
     * <ul>
     * <li>bundles
     * <li>components
     * <li>extension points declarations
     * <li>services
     * <li>contributions
     * </ul>
     * <p>
     * Relations between nodes will respect the general introspection of bundles.
     * <p>
     * TODO: define/describe strategy for node and edge weights.
     */
    protected EditableGraph getDefaultGraph(DistributionSnapshot distribution) {
        EditableGraph graph = createDefaultGraph();

        Map<String, Integer> nodeHits = new HashMap<>();
        Set<String> isolatedBundles = new HashSet<>();

        for (String bid : distribution.getBundleIds()) {
            BundleInfo bundle = distribution.getBundle(bid);
            NODE_CATEGORY category = NODE_CATEGORY.guessCategory(bundle);
            Node bundleNode = createBundleNode(bundle, category);
            bundleNode.setWeight(1);
            graph.addNode(bundleNode);
            for (String requirement : bundle.getRequirements()) {
                graph.addEdge(createEdge(bundleNode, createNode(NODE_TYPE.BUNDLE.prefix(requirement)),
                        EDGE_TYPE.REQUIRES.name()));
            }
            if (bundle.getRequirements().isEmpty()) {
                isolatedBundles.add(NODE_TYPE.BUNDLE.prefix(bundle.getId()));
            }
            for (ComponentInfo component : bundle.getComponents()) {
                Node compNode = createComponentNode(component, category);
                graph.addNode(compNode);
                graph.addEdge(createEdge(bundleNode, compNode, EDGE_TYPE.CONTAINS.name()));
                for (ServiceInfo service : component.getServices()) {
                    if (service.isOverriden()) {
                        continue;
                    }
                    Node serviceNode = createServiceNode(service, category);
                    graph.addNode(serviceNode);
                    graph.addEdge(createEdge(compNode, serviceNode, EDGE_TYPE.CONTAINS.name()));
                    hit(nodeHits, compNode.getId());
                }

                for (ExtensionPointInfo xp : component.getExtensionPoints()) {
                    Node xpNode = createXPNode(xp, category);
                    graph.addNode(xpNode);
                    graph.addEdge(createEdge(compNode, xpNode, EDGE_TYPE.CONTAINS.name()));
                    hit(nodeHits, compNode.getId());
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

                    Node contNode = createContributionNode(contribution, category);
                    graph.addNode(contNode);
                    // add link to corresponding component
                    graph.addEdge(createEdge(compNode, contNode, EDGE_TYPE.CONTAINS.name()));
                    hit(nodeHits, compNode.getId());

                    // also add link to target extension point, "guessing" the extension point id, not counting for
                    // hits
                    String targetId = NODE_TYPE.EXTENSION_POINT.prefix(contribution.getExtensionPoint());
                    graph.addEdge(createEdge(createNode(targetId), contNode, EDGE_TYPE.REFERENCES.name()));
                    hit(nodeHits, targetId);
                    hit(nodeHits, NODE_TYPE.COMPONENT.prefix(contribution.getTargetComponentName().getRawName()));
                }

                // compute requirements
                for (String requirement : component.getRequirements()) {
                    graph.addEdge(createEdge(compNode, createNode(NODE_TYPE.COMPONENT.prefix(requirement)),
                            EDGE_TYPE.SOFT_REQUIRES.name()));
                }

            }
        }

        // Adds potentially missing bundle root
        String rrId = getBundleRootId();
        Node rrNode = graph.getNode(rrId);
        if (rrNode == null) {
            rrNode = createBundleRoot();
            graph.addNode(rrNode);
        }
        // Adds potentially missing link to bundle root for isolated (children nodes)
        List<Node> orphans = new ArrayList<>();
        // handle missing references in all edges too
        for (Edge edge : graph.getEdges()) {
            Node source = graph.getNode(edge.getSource());
            if (source == null) {
                source = addMissingNode(graph, edge.getSource());
            }
            Node target = graph.getNode(edge.getTarget());
            if (target == null) {
                target = addMissingNode(graph, edge.getTarget());
                if (NODE_TYPE.BUNDLE.name().equals(target.getType())
                        && EDGE_TYPE.REQUIRES.name().equals(edge.getValue())) {
                    orphans.add(target);
                }
            }
        }
        // handle isolated bundles
        for (Node orphan : orphans) {
            requireBundleRootNode(graph, orphan);
        }
        for (String child : isolatedBundles) {
            requireBundleRootNode(graph, graph.getNode(child));
        }

        refine(graph, nodeHits);

        return graph;
    }

    /**
     * Introspect the graph, centered on services.
     * <p>
     * select only services (resolving their dependencies based on their bundle's), linking them as parent of their
     * extension points, and handling their contributions as a different relation.
     * <p>
     * When more than one service is declared within the same component, corresponding extension points and
     * contributions will be "shared".
     * <p>
     * Other contributions, that cannot be linked to any service within the same components, are handled as mere
     * "contributions" and are only linked to the extension point that they're contributing to.
     */
    protected EditableGraph getServicesGraph(DistributionSnapshot distribution) {
        EditableGraph graph = createDefaultGraph();
        Map<String, Integer> hits = new HashMap<>();
        refine(graph, hits);

        return graph;
    }

    /**
     * Retrieve services that are a requirement to the given bundle, according to targeted bundles services.
     */
    protected List<ServiceInfo> retrieveServiceRequirements(DistributionSnapshot distribution, BundleInfo bundle) {
        List<ServiceInfo> services = new ArrayList<>();
        // compute requirements
        for (String requirement : bundle.getRequirements()) {
            BundleInfo rbundle = distribution.getBundle(requirement);
            if (rbundle != null) {
                for (ComponentInfo rcomponent : rbundle.getComponents()) {
                    for (ServiceInfo service : rcomponent.getServices()) {
                        if (service.isOverriden()) {
                            continue;
                        }
                        services.add(service);
                    }
                }
            }
        }
        return services;
    }

    protected void requireBundleRootNode(EditableGraph graph, Node bundleNode) {
        if (bundleNode == null) {
            return;
        }
        // make it require the root node, unless we're handling the root itself
        String rootId = getBundleRootId();
        if (!rootId.equals(bundleNode.getId())) {
            graph.addEdge(createEdge(bundleNode, createNode(rootId), EDGE_TYPE.REQUIRES.name()));
        }
    }

    /**
     * Refines graphs for display.
     * <ul>
     * <li>sets weights computed from hits map
     * <li>handles zero weights
     */
    protected void refine(EditableGraph graph, Map<String, Integer> hits) {
        for (Map.Entry<String, Integer> hit : hits.entrySet()) {
            setWeight(graph, hit.getKey(), hit.getValue());
        }
    }

    protected Node addMissingNode(EditableGraph graph, String id) {
        // try to guess category and type according to prefix
        NODE_CATEGORY cat = NODE_CATEGORY.PLATFORM;
        NODE_TYPE type = NODE_TYPE.guess(id);
        String unprefixedId = type.unprefix(id);
        cat = NODE_CATEGORY.guess(unprefixedId);
        Node node = createNode(id, unprefixedId, getDefaultNodeWeight(), "", type.name(), cat.name());
        graph.addNode(node);
        return node;
    }

    protected String getBundleRootId() {
        return NODE_TYPE.BUNDLE.prefix(BundleInfo.RUNTIME_ROOT_PSEUDO_BUNDLE);
    }

    protected int getDefaultNodeWeight() {
        return 1;
    }

    protected int getDefaultEdgeWeight() {
        return 1;
    }

    protected Node createNode(String id, String label, int weight, String path, String type, String category) {
        return new NodeImpl(id, label, weight, path, type, category);
    }

    protected Node createNode(String id) {
        return createNode(id, id, getDefaultNodeWeight(), null, null, null);
    }

    protected Node createBundleRoot() {
        return createNode(getBundleRootId(), BundleInfo.RUNTIME_ROOT_PSEUDO_BUNDLE, getDefaultNodeWeight(), "",
                NODE_TYPE.BUNDLE.name(), NODE_CATEGORY.RUNTIME.name());
    }

    protected Node createBundleNode(BundleInfo bundle, NODE_CATEGORY cat) {
        String bid = bundle.getId();
        String pbid = NODE_TYPE.BUNDLE.prefix(bid);
        return createNode(pbid, bid, getDefaultNodeWeight(), "", NODE_TYPE.BUNDLE.name(), cat.name());
    }

    protected Node createComponentNode(ComponentInfo component, NODE_CATEGORY cat) {
        String compid = component.getId();
        String pcompid = NODE_TYPE.COMPONENT.prefix(compid);
        return createNode(pcompid, compid, getDefaultNodeWeight(), component.getHierarchyPath(),
                NODE_TYPE.COMPONENT.name(), cat.name());
    }

    protected Node createServiceNode(ServiceInfo service, NODE_CATEGORY cat) {
        String sid = service.getId();
        String psid = NODE_TYPE.SERVICE.prefix(sid);
        return createNode(psid, sid, getDefaultNodeWeight(), service.getHierarchyPath(), NODE_TYPE.SERVICE.name(),
                cat.name());
    }

    protected Node createXPNode(ExtensionPointInfo xp, NODE_CATEGORY cat) {
        String xpid = xp.getId();
        String pxpid = NODE_TYPE.EXTENSION_POINT.prefix(xpid);
        return createNode(pxpid, xpid, getDefaultNodeWeight(), xp.getHierarchyPath(), NODE_TYPE.EXTENSION_POINT.name(),
                cat.name());
    }

    protected Node createContributionNode(ExtensionInfo contribution, NODE_CATEGORY cat) {
        String cid = contribution.getId();
        String pcid = NODE_TYPE.CONTRIBUTION.prefix(cid);
        return createNode(pcid, cid, getDefaultNodeWeight(), contribution.getHierarchyPath(),
                NODE_TYPE.CONTRIBUTION.name(), cat.name());
    }

    protected Edge createEdge(Node source, Node target, String value) {
        return new EdgeImpl(source.getId(), target.getId(), value, getDefaultEdgeWeight());
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
            node.setWeight(node.getWeight() + hit);
        }
    }

}
