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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.bulk.introspection;

import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_2;
import static org.nuxeo.lib.stream.computation.log.LogStreamManager.METRICS_STREAM;
import static org.nuxeo.lib.stream.computation.log.LogStreamManager.PROCESSORS_STREAM;

import java.util.Arrays;
import java.util.Map;

import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * A processor to introspect Nuxeo Stream activities at the cluster level.
 *
 * @since 11.5
 */
public class StreamIntrospectionProcessor implements StreamProcessorTopology {

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(() -> new StreamIntrospectionComputation(),
                               Arrays.asList(INPUT_1 + ":" + PROCESSORS_STREAM, INPUT_2 + ":" + METRICS_STREAM))
                       .build();
    }

}
