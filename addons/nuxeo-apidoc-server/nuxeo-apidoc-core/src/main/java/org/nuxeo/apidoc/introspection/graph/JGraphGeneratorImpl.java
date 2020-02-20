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

import org.nuxeo.apidoc.api.graph.Graph;
import org.nuxeo.apidoc.api.graph.NetworkGraphGenerator;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;

/**
 * Basic implementation relying on introspection of distribution using jgrapht library.
 *
 * @since 11.1
 */
public class JGraphGeneratorImpl implements NetworkGraphGenerator {

    protected final String graphId;

    protected final DistributionSnapshot distribution;

    public JGraphGeneratorImpl(String graphId, DistributionSnapshot distribution) {
        super();
        this.graphId = graphId;
        this.distribution = distribution;
    }

    public static Graph getGraph(String graphId, DistributionSnapshot distribution) {
        JGraphGeneratorImpl gen = new JGraphGeneratorImpl(graphId, distribution);
        return gen.getGraph();
    }

    @Override
    public Graph getGraph() {
        GraphImpl graph = new GraphImpl(graphId);

        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

}
