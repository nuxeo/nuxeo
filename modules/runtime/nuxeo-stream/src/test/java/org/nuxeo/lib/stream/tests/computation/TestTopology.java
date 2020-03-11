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
package org.nuxeo.lib.stream.tests.computation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Test;
import org.nuxeo.lib.stream.computation.Topology;

/**
 * @since 9.3
 */
public class TestTopology {

    @Test
    public void testTopology() {

        Topology topology = Topology.builder()
                                    .addComputation(() -> new ComputationSource("C1"),
                                            Collections.singletonList("o1:s1"))
                                    .addComputation(() -> new ComputationForward("C2", 1, 2),
                                            Arrays.asList("i1:s1", "o1:s2", "o2:s3"))
                                    .addComputation(() -> new ComputationForward("C3", 2, 1),
                                            Arrays.asList("i1:s1", "i2:s4", "o1:output"))
                                    .addComputation(() -> new ComputationForward("C4", 1, 2),
                                            Arrays.asList("i1:s2", "o1:output", "o2:s4"))
                                    .addComputation(() -> new ComputationForward("C5", 1, 0),
                                            Collections.singletonList("i1:s3"))
                                    .build();

        assertNotNull(topology);
        assertEquals(5, topology.streamsSet().size());
        assertEquals(5, topology.metadataList().size());

        assertEquals(new HashSet<>(), topology.getAncestorComputationNames("C1"));
        assertEquals(new HashSet<>(Collections.singletonList("C1")), topology.getAncestorComputationNames("C2"));
        assertEquals(new HashSet<>(Arrays.asList("C1", "C2", "C4")), topology.getAncestorComputationNames("C3"));
        assertEquals(new HashSet<>(Arrays.asList("C1", "C2")), topology.getAncestorComputationNames("C4"));
        assertEquals(new HashSet<>(Arrays.asList("C1", "C2")), topology.getAncestorComputationNames("C5"));

        assertTrue(topology.isSource("C1"));
        assertFalse(topology.isSource("C2"));

        assertTrue(topology.isSink("C5"));
        assertFalse(topology.isSink("C2"));

        assertEquals(new HashSet<>(Collections.singletonList("s1")), topology.getChildren("C1"));
        assertEquals(new HashSet<>(Collections.singletonList("C1")), topology.getParents("s1"));

        assertEquals(new HashSet<>(Arrays.asList("s2", "s3")), topology.getChildren("C2"));
        assertEquals(new HashSet<>(Collections.singletonList("s1")), topology.getParents("C2"));

        assertEquals(new HashSet<>(Arrays.asList("C3", "C4")), topology.getParents("output"));
        assertEquals(new HashSet<>(Arrays.asList("C1", "C2", "C3", "C4", "s1", "s2", "s4")),
                topology.getAncestors("output"));
        assertEquals(new HashSet<>(Arrays.asList("C2", "C3", "C4", "C5", "s2", "s3", "s4", "output")),
                topology.getDescendants("s1"));
        assertEquals(new HashSet<>(Arrays.asList("C2", "C3", "C4", "C5")),
                topology.getDescendantComputationNames("s1"));

        assertEquals(new HashSet<>(Collections.singletonList("C1")), topology.getRoots());

        assertEquals(Collections.emptySet(), topology.getParentComputationsNames("C1"));
        assertEquals(new HashSet<>(Arrays.asList("C3", "C4")), topology.getParentComputationsNames("output"));
        assertEquals(new HashSet<>(Arrays.asList("C1", "C4")), topology.getParentComputationsNames("C3"));

        // check plantuml representation
        assertTrue(topology.toPlantuml().startsWith("@startuml"));
        assertTrue(topology.toPlantuml().endsWith("@enduml\n"));
    }

    @Test
    public void testTopologyMultiRoot() {

        Topology topology = Topology.builder()
                                    .addComputation(() -> new ComputationSource("R1"),
                                            Collections.singletonList("o1:s1"))
                                    .addComputation(() -> new ComputationForward("C2", 1, 2),
                                            Arrays.asList("i1:s1", "o1:s2", "o2:s3"))
                                    .addComputation(() -> new ComputationSource("R2"),
                                            Collections.singletonList("o1:s20"))
                                    .addComputation(() -> new ComputationForward("C21", 1, 0),
                                            Collections.singletonList("i1:s20"))
                                    .addComputation(() -> new ComputationForward("R3", 1, 1),
                                            Arrays.asList("i1:s30", "o1:s31"))
                                    .build();

        assertNotNull(topology);
        assertEquals(new HashSet<>(Arrays.asList("R1", "R2", "s30")), topology.getRoots());
        assertEquals(new HashSet<>(Collections.singletonList("R3")), topology.getDescendantComputationNames("s30"));

    }
}
