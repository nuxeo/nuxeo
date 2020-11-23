/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Charles Boidot
 */
package org.nuxeo.ecm.platform.video.action;

import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.video.computation.RecomputeTranscodedVideosComputation;
import org.nuxeo.ecm.platform.video.computation.RecomputeVideoInfoComputation;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * BAF Computation that fills video renditions for the blob property described by the given xpath.
 *
 * @since 11.5
 */
public class RecomputeVideoConversionsAction implements StreamProcessorTopology {

    public static final String ACTION_NAME = "recomputeVideoConversion";

    public static final String ACTION_FULL_NAME = "bulk/" + ACTION_NAME;

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(RecomputeVideoInfoComputation::new,
                               List.of(INPUT_1 + ":" + ACTION_FULL_NAME, OUTPUT_1 + ":" + RecomputeTranscodedVideosComputation.NAME))
                       .addComputation(RecomputeTranscodedVideosComputation::new,
                               List.of(INPUT_1 + ":" + RecomputeTranscodedVideosComputation.NAME,
                                       OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

}
