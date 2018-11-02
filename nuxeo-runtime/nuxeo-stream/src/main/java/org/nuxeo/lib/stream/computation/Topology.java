/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.lib.stream.computation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.graph.DefaultEdge;

/**
 * Represent a Directed Acyclic Graph (DAG) of computations.
 *
 * @since 9.3
 */
public class Topology {

    protected final List<ComputationMetadataMapping> metadataList; // use a list because computation are ordered using

    protected final Map<String, ComputationMetadataMapping> metadataMap = new HashMap<>();

    // dag
    protected final Map<String, Supplier<Computation>> supplierMap = new HashMap<>();

    protected final DirectedAcyclicGraph<Vertex, DefaultEdge> dag = new DirectedAcyclicGraph<>(DefaultEdge.class);

    protected Topology(Builder builder) {
        this.supplierMap.putAll(builder.suppliersMap);
        builder.metadataSet.forEach(meta -> metadataMap.put(meta.name, meta));
        this.metadataList = new ArrayList<>(builder.metadataSet.size());
        try {
            generateDag(builder.metadataSet);
        } catch (DirectedAcyclicGraph.CycleFoundException e) {
            throw new IllegalStateException("Cycle found in topology: " + e.getMessage(), e);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * A plantuml representation of the topology.
     */
    public String toPlantuml() {
        return toPlantuml(new Settings(0, 0));
    }

    @SuppressWarnings("SpellCheckingInspection")
    public String toPlantuml(Settings settings) {
        StringBuilder ret = new StringBuilder();
        ret.append("@startuml\n");
        for (Vertex vertex : dag) {
            if (VertexType.COMPUTATION.equals(vertex.getType())) {
                String name = vertex.getName();
                int concurrency = settings.getConcurrency(vertex.getName());
                ret.append(String.format("node %s [%s%n----%n%d threads]%n", name, name, concurrency));
            } else if (VertexType.STREAM.equals(vertex.getType())) {
                String name = vertex.getName();
                int partitions = settings.getPartitions(vertex.getName());
                ret.append(String.format("queue %s [%s%n----%n%d partitions]%n", name, name, partitions));
            }
        }
        for (DefaultEdge edge : dag.edgeSet()) {
            ret.append(
                    String.format("%s==>%s%n", dag.getEdgeSource(edge).getName(), dag.getEdgeTarget(edge).getName()));
        }
        ret.append("@enduml\n");
        return ret.toString().replace("%n1 partitions", "%n1 partition").replace("%n1 threads", "%n1 thread");
    }

    protected void generateDag(Set<ComputationMetadataMapping> metadataSet)
            throws DirectedAcyclicGraph.CycleFoundException {
        for (ComputationMetadata metadata : metadataSet) {
            Vertex computationVertex = new Vertex(VertexType.COMPUTATION, metadata.name);
            dag.addVertex(computationVertex);
            if (metadata.outputStreams != null) {
                for (String stream : metadata.outputStreams) {
                    Vertex streamVertex = new Vertex(VertexType.STREAM, stream);
                    dag.addVertex(streamVertex);
                    dag.addDagEdge(computationVertex, streamVertex);
                }
            }
            if (metadata.inputStreams() != null) {
                for (String streamName : metadata.inputStreams()) {
                    Vertex streamVertex = new Vertex(VertexType.STREAM, streamName);
                    dag.addVertex(streamVertex);
                    dag.addDagEdge(streamVertex, computationVertex);
                }
            }
        }
        generateMetadataMapping(metadataSet);
    }

    protected void generateMetadataMapping(Set<ComputationMetadataMapping> metadataSet) {
        for (Vertex vertex : dag) {
            if (VertexType.COMPUTATION.equals(vertex.getType())) {
                for (ComputationMetadataMapping metadata : metadataSet) {
                    if (vertex.getName().equals(metadata.name)) {
                        metadataList.add(metadata);
                        break;
                    }
                }
            }
        }
    }

    public ComputationMetadataMapping getMetadata(String name) {
        return metadataMap.get(name);
    }

    public Supplier<Computation> getSupplier(String name) {
        return supplierMap.get(name);
    }

    public boolean isSource(String name) {
        return getParents(name).isEmpty();
    }

    public boolean isSink(String name) {
        return getChildren(name).isEmpty();
    }

    public Set<String> streamsSet() {
        Set<String> ret = new HashSet<>();
        for (ComputationMetadata metadata : this.metadataList) {
            ret.addAll(metadata.inputStreams());
            ret.addAll(metadata.outputStreams());
        }
        return ret;
    }

    public Set<String> streamsSet(String root) {
        Set<String> ret = new HashSet<>();
        for (String name : getDescendantComputationNames(root)) {
            ComputationMetadataMapping meta = getMetadata(name);
            ret.addAll(meta.inputStreams());
            ret.addAll(meta.outputStreams());
        }
        return ret;
    }

    public List<ComputationMetadataMapping> metadataList() {
        return metadataList;
    }

    protected Vertex getVertex(String name) {
        Vertex ret;
        if (metadataMap.containsKey(name)) {
            ret = new Vertex(VertexType.COMPUTATION, name);
        } else if (streamsSet().contains(name)) {
            ret = new Vertex(VertexType.STREAM, name);
        } else {
            throw new IllegalArgumentException("Unknown vertex name: " + name + " for dag: " + dag);
        }
        return ret;
    }

    public Set<String> getDescendants(String name) {
        Vertex start = getVertex(name);
        return dag.getDescendants(dag, start).stream().map(Vertex::getName).collect(Collectors.toSet());
    }

    public Set<String> getDescendantComputationNames(String name) {
        Vertex start = getVertex(name);
        return dag.getDescendants(dag, start)
                  .stream()
                  .filter(vertex -> vertex.type == VertexType.COMPUTATION)
                  .map(vertex -> vertex.name)
                  .collect(Collectors.toSet());
    }

    public Set<String> getChildren(String name) {
        Vertex start = getVertex(name);
        return dag.outgoingEdgesOf(start)
                  .stream()
                  .map(edge -> dag.getEdgeTarget(edge).getName())
                  .collect(Collectors.toSet());
    }

    public Set<String> getChildrenComputationNames(String name) {
        Vertex start = getVertex(name);
        Set<String> children = getChildren(name);
        if (start.type == VertexType.STREAM) {
            return children;
        }
        Set<String> ret = new HashSet<>();
        children.forEach(child -> ret.addAll(getChildren(child)));
        return ret;
    }

    public Set<String> getParents(String name) {
        Vertex start = getVertex(name);
        return dag.incomingEdgesOf(start)
                  .stream()
                  .map(edge -> dag.getEdgeSource(edge).getName())
                  .collect(Collectors.toSet());
    }

    public Set<String> getParentComputationsNames(String name) {
        Vertex start = getVertex(name);
        Set<String> parents = getParents(name);
        if (start.type == VertexType.STREAM) {
            return parents;
        }
        Set<String> ret = new HashSet<>();
        parents.forEach(parent -> ret.addAll(getParents(parent)));
        return ret;
    }

    public Set<String> getAncestorComputationNames(String name) {
        Set<Vertex> ancestors = dag.getAncestors(dag, new Vertex(VertexType.COMPUTATION, name));
        return ancestors.stream()
                        .filter(vertex -> vertex.type == VertexType.COMPUTATION)
                        .map(vertex -> vertex.name)
                        .collect(Collectors.toSet());
    }

    public Set<String> getAncestors(String name) {
        Vertex start = getVertex(name);
        return dag.getAncestors(dag, start).stream().map(Vertex::getName).collect(Collectors.toSet());
    }

    public Set<String> getRoots() {
        Set<String> ret = new HashSet<>();
        for (Vertex vertex : dag) {
            if (dag.getAncestors(dag, vertex).isEmpty()) {
                ret.add(vertex.getName());
            }
        }
        return ret;
    }

    public DirectedAcyclicGraph<Vertex, DefaultEdge> getDag() {
        return dag;
    }

    protected enum VertexType {
        COMPUTATION, STREAM
    }

    public static class Builder {
        final Set<ComputationMetadataMapping> metadataSet = new HashSet<>();

        final Map<String, Supplier<Computation>> suppliersMap = new HashMap<>();

        public Builder addComputation(Supplier<Computation> supplier, List<String> mapping) {
            Map<String, String> map = new HashMap<>(mapping.size());
            mapping.stream().filter(m -> m.contains(":")).forEach(m -> map.put(m.split(":")[0], m.split(":")[1]));
            ComputationMetadataMapping meta = new ComputationMetadataMapping(supplier.get().metadata(), map);
            metadataSet.add(meta);
            suppliersMap.put(meta.name, supplier);
            return this;
        }

        public Topology build() {
            return new Topology(this);
        }
    }

    public static class Vertex {
        protected final String name;

        protected final VertexType type;

        public Vertex(VertexType type, String name) {
            this.type = type;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public VertexType getType() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            Vertex myVertex = (Vertex) o;

            return name.equals(myVertex.name) && type == myVertex.type;

        }

        @Override
        public String toString() {
            return "Vertex{" + "name='" + name + '\'' + ", type=" + type + '}';
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + type.hashCode();
            return result;
        }
    }
}
