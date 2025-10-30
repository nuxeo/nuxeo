/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.core.bulk.introspection;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.nuxeo.common.utils.ByteSize;

/**
 * @since 2025.12
 */
public record ScaleActivity(Scale scale, List<ClusterNode> nodes, List<ActiveComputation> computations)
        implements Serializable {

    public ScaleActivity {
        nodes = List.copyOf(nodes);
        computations = List.copyOf(computations);
    }

    public record Scale(int currentNodes, int bestNodes, int optimalNodes, int metric) {
    }

    public record ClusterNode(String hostname, String nodeId, String type, String ip, int cpuCores,
            ByteSize jvmHeapSize, Instant created, Instant alive) implements Serializable {
    }

    public record ActiveComputation(String computation, Map<String, ActiveComputationStream> streams,
            List<ActiveComputationNode> nodes, @Nullable ActiveComputationRecommendation current,
            @Nullable ActiveComputationRecommendation best) implements Serializable {

        public ActiveComputation {
            streams = Map.copyOf(streams);
            nodes = List.copyOf(nodes);
        }

        public ActiveComputation(String computation, Map<String, ActiveComputationStream> streams,
                List<ActiveComputationNode> nodes) {
            this(computation, streams, nodes, null, null);
        }
    }

    public record ActiveComputationStream(String stream, int partitions, long lag, @Nullable Long end,
            @Nullable Duration latency) implements Serializable {
    }

    public record ActiveComputationNode(String nodeId, int threads, Instant timestamp, long count, double sum,
            double rate1m, double rate5m, double min, double p50, double mean, double p95, double max, double stddev)
            implements Serializable {
    }

    public record ActiveComputationRecommendation(int nodes, int threads, double rate1m, int eta, boolean relevant)
            implements Serializable {
    }
}
