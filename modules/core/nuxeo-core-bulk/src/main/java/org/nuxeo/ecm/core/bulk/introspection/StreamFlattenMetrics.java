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

import java.time.Instant;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

/**
 * @since 2025.12
 */
public record StreamFlattenMetrics(Map<String, StreamMetric> streamMetrics,
        Map<Pair<String, String>, ComputationMetric> computationMetrics, Map<String, NodeMetric> nodeMetrics,
        Instant date) {

    public StreamFlattenMetrics {
        streamMetrics = Map.copyOf(streamMetrics);
        computationMetrics = Map.copyOf(computationMetrics);
        nodeMetrics = Map.copyOf(nodeMetrics);
    }

    @Nullable
    public StreamMetric streamMetric(String streamName) {
        return streamMetrics.get(streamName);
    }

    @Nullable
    public ComputationMetric computationMetric(String nodeId, String computationName) {
        return computationMetrics.get(Pair.of(nodeId, computationName));
    }

    public record StreamMetric(@Nullable Number end, Map<String, StreamComputationMetric> computationMetrics) {

        public StreamMetric {
            computationMetrics = Map.copyOf(computationMetrics);
        }

        @Nullable
        public StreamComputationMetric computationMetric(String computationName) {
            return computationMetrics.get(computationName);
        }
    }

    public record StreamComputationMetric(@Nullable Number lag, @Nullable Number latency, @Nullable Number pos) {
    }

    public record ComputationMetric(@Nullable StreamIntrospection.TimerMetric record,
            @Nullable StreamIntrospection.TimerMetric timer, @Nullable Number failure, @Nullable Number skipped) {
    }

    public record NodeMetric(@Nullable Number failure) {
    }
}
