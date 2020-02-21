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
            String pbid = prefixId(BundleInfo.TYPE_NAME, bid);
            graph.addNode(new NodeImpl(pbid, bid, 0, "", NODE_TYPE.BUNDLE.name(), cat.name(), cat.getColor()));
            // compute sub components
            List<ComponentInfo> components = bundle.getComponents();
            for (ComponentInfo component : components) {
                String compid = component.getId();
                String pcompid = prefixId(ComponentInfo.TYPE_NAME, compid);
                graph.addNode(new NodeImpl(pcompid, compid, 0, component.getHierarchyPath(), NODE_TYPE.COMPONENT.name(),
                        cat.name(), cat.getColor()));
                graph.addEdge(new EdgeImpl(pbid, pcompid, EDGE_TYPE.REFERENCES.name()));
                for (ServiceInfo service : component.getServices()) {
                    if (service.isOverriden()) {
                        continue;
                    }
                    String sid = service.getId();
                    String psid = prefixId(ServiceInfo.TYPE_NAME, sid);
                    graph.addNode(new NodeImpl(psid, sid, 0, service.getHierarchyPath(), NODE_TYPE.SERVICE.name(),
                            cat.name(), cat.getColor()));
                    graph.addEdge(new EdgeImpl(pcompid, psid, EDGE_TYPE.REFERENCES.name()));
                }

                for (ExtensionPointInfo xp : component.getExtensionPoints()) {
                    String xpid = xp.getId();
                    String pxpid = prefixId(ExtensionPointInfo.TYPE_NAME, xpid);
                    graph.addNode(new NodeImpl(pxpid, xpid, 0, xp.getHierarchyPath(), NODE_TYPE.EXTENSION_POINT.name(),
                            cat.name(), cat.getColor()));
                    graph.addEdge(new EdgeImpl(pcompid, pxpid, EDGE_TYPE.REFERENCES.name()));
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
                    graph.addNode(new NodeImpl(pcid, cid, 0, contribution.getHierarchyPath(),
                            NODE_TYPE.CONTRIBUTION.name(), cat.name(), cat.getColor()));
                    graph.addEdge(new EdgeImpl(pcompid, pcid, EDGE_TYPE.REFERENCES.name()));

                    // also add link to target extension point, "guessing" the extension point id
                    String targetId = prefixId(ComponentInfo.TYPE_NAME,
                            contribution.getTargetComponentName() + "--" + contribution.getExtensionPoint());
                    graph.addEdge(new EdgeImpl(targetId, pcid, EDGE_TYPE.REFERENCES.name()));
                }
            }
        }

        return graph;
    }

    /**
     * Prefix all ids assuming each id is unique within a given type, to avoid potential collisions.
     */
    protected String prefixId(String prefix, String id) {
        return prefix + "-" + id;
    }

}
