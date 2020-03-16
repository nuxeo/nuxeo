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

import java.util.Arrays;
import java.util.List;

import org.nuxeo.apidoc.api.graph.EditableGraph;
import org.nuxeo.apidoc.api.graph.GRAPH_TYPE;
import org.nuxeo.apidoc.api.graph.Graph;
import org.nuxeo.apidoc.api.graph.NODE_TYPE;
import org.nuxeo.apidoc.introspection.graph.export.GephiGraphExporter;
import org.nuxeo.apidoc.introspection.graph.export.PlotlyGraphExporter;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;

/**
 * Basic implementation relying on introspection of distribution using jgrapht library.
 *
 * @since 11.1
 */
public class GephiGraphGeneratorImpl extends AbstractGraphGeneratorImpl {

    public GephiGraphGeneratorImpl() {
        super();
    }

    @Override
    protected PositionedNodeImpl createNode(String id, String label, int weight, String path, String type,
            String category) {
        return new PositionedNodeImpl(id, label, weight, path, type, category);
    }

    @Override
    public List<Graph> getGraphs(DistributionSnapshot distribution) {
        EditableGraph defaultGraph = super.getDefaultGraph(distribution);

        // position nodes depending on bundles weight, centered on bundle root, thanks to gephi exporter.
        GephiGraphExporter gephiExporter = new GephiGraphExporter(defaultGraph.copy(null));
        EditableGraph completeGraph = gephiExporter.applyForceLayout(distribution, NODE_TYPE.BUNDLE, true);
        completeGraph.setName(getName());
        completeGraph.setType(GRAPH_TYPE.BASIC_LAYOUT_3D.name());
        completeGraph.setTitle("Complete Force Graph");
        completeGraph.setDescription("Complete graph, with dependencies, with a force layout");
        ContentGraphImpl g1 = new PlotlyGraphExporter(completeGraph).export();
        // also persist the gephi export that produced this layout

        ContentGraphImpl g1gephi = gephiExporter.export();
        g1gephi.setName(getName() + "gefx");
        g1gephi.setType(GRAPH_TYPE.BASIC_LAYOUT_3D.name());
        g1gephi.setTitle("Complete Force Graph");
        g1gephi.setDescription("Complete graph, with dependencies, with a force layout");

        gephiExporter.setGraph(defaultGraph.copy(null));
        EditableGraph completeGraphFlat = gephiExporter.applyForceLayout(distribution, NODE_TYPE.BUNDLE, false);
        completeGraphFlat.setName(getName() + "flat");
        completeGraphFlat.setType(GRAPH_TYPE.BASIC_LAYOUT.name());
        completeGraphFlat.setTitle("Complete Force Graph - Flat");
        completeGraphFlat.setDescription("Complete graph, with dependencies, with a force layout");
        ContentGraphImpl g1flat = new PlotlyGraphExporter(completeGraphFlat).export();

        gephiExporter.setGraph(defaultGraph.copy(new NodeTypeFilter(NODE_TYPE.BUNDLE.name())));
        EditableGraph bundleGraph = gephiExporter.applyForceLayout(distribution, null, false);
        bundleGraph.setName(getName() + "bundles");
        bundleGraph.setType(GRAPH_TYPE.BASIC_LAYOUT.name());
        bundleGraph.setTitle("Bundles Force Graph");
        bundleGraph.setDescription("Selected bundles, with dependencies, with a force layout");
        ContentGraphImpl g2 = new PlotlyGraphExporter(bundleGraph).export();

        gephiExporter.setGraph(defaultGraph.copy(new NodeTypeFilter(NODE_TYPE.COMPONENT.name(),
                NODE_TYPE.EXTENSION_POINT.name(), NODE_TYPE.CONTRIBUTION.name())));
        EditableGraph xpGraph = gephiExporter.applyForceLayout(distribution, NODE_TYPE.COMPONENT, true);
        xpGraph.setName(getName() + "xp");
        xpGraph.setType(GRAPH_TYPE.BASIC_LAYOUT_3D.name());
        xpGraph.setTitle("XP Force Graph");
        xpGraph.setDescription("Selected XP, with dependencies, with a force layout");
        ContentGraphImpl g3 = new PlotlyGraphExporter(xpGraph).export();

        gephiExporter.setGraph(defaultGraph.copy(new NodeTypeFilter(NODE_TYPE.COMPONENT.name(),
                NODE_TYPE.EXTENSION_POINT.name(), NODE_TYPE.CONTRIBUTION.name())));
        EditableGraph xpGraphFlat = gephiExporter.applyForceLayout(distribution, NODE_TYPE.COMPONENT, false);
        xpGraphFlat.setName(getName() + "xpflat");
        xpGraphFlat.setType(GRAPH_TYPE.BASIC_LAYOUT.name());
        xpGraphFlat.setTitle("XP Force Graph - Flat");
        xpGraphFlat.setDescription("Selected XP, with dependencies, with a force layout");
        ContentGraphImpl g3flat = new PlotlyGraphExporter(xpGraphFlat).export();

        // EditableGraph completeOOGraph = applyBundleOpenOrdLayout(distribution, defaultGraph.copy(null));
        // completeOOGraph.setName(getName() + "oo");
        // completeOOGraph.setType(GRAPH_TYPE.BASIC_LAYOUT.name());
        // completeOOGraph.setTitle("Complete OpenOrd Graph");
        // completeOOGraph.setDescription("Complete graph, with dependencies, with a OpenOrd layout");
        // ContentGraphImpl g5 = new PlotlyGraphExporter().export(completeOOGraph);
        //
        // EditableGraph bundleOOGraph = completeOOGraph.copy(new NodeTypeFilter(NODE_TYPE.BUNDLE.name()));
        // bundleOOGraph.setName(getName() + "oobundles");
        // bundleOOGraph.setType(GRAPH_TYPE.BASIC_LAYOUT.name());
        // bundleOOGraph.setTitle("Bundles OpenOrd Graph");
        // bundleOOGraph.setDescription("Selected bundles, with dependencies, with a OpenOrd layout");
        // ContentGraphImpl g6 = new PlotlyGraphExporter().export(bundleOOGraph);

        // XXX: force atlas 3d: https://github.com/gephi/gephi-plugins/pull/143
        // XXX: force atlas weight explanation: https://github.com/gephi/gephi/issues/1816

        return Arrays.asList(g1, g1gephi, g1flat, g2, g3, g3flat);
    }

}
