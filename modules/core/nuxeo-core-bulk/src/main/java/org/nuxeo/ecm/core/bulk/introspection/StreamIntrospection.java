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

import org.nuxeo.common.stream.MapMultis;
import org.nuxeo.common.utils.ByteSize;

/**
 * This POJO represents what is serialized to the {@link StreamIntrospectionComputation#INTROSPECTION_KV_STORE}.
 * 
 * @since 2025.12
 */
public record StreamIntrospection(List<Stream> streams, List<Processor> processors, List<Metrics> metrics)
        implements Serializable {

    public StreamIntrospection {
        streams = List.copyOf(streams);
        processors = List.copyOf(processors);
        metrics = List.copyOf(metrics);
    }

    public List<ProcessorTopology> consumers(String streamName) {
        return processors().stream()
                           .mapMulti(
                                   MapMultis.eachInstanceOf(Processor::topology, ProcessorStreamConsumerTopology.class))
                           .filter(consumer -> consumer.stream().equals(streamName))
                           .map(ProcessorTopology.class::cast)
                           .toList();
    }

    public record Stream(String name, int partitions, String codec) implements Serializable {
    }

    public record Processor(ProcessorMetadata metadata, List<ProcessorComputation> computations,
            List<ProcessorTopology> topology) implements Serializable {

        public Processor {
            computations = List.copyOf(computations);
            topology = List.copyOf(topology);
        }
    }

    public record ProcessorMetadata(String processorName, String nodeId, String hostname, String ip, int cpuCores,
            ByteSize jvmHeapSize, Instant created) implements Serializable {
    }

    public record ProcessorComputation(String name, int threads, boolean continueOnFailure, int batchCapacity,
            Duration batchThreshold, int maxRetries, Duration retryDelay) implements Serializable {
    }

    public sealed interface ProcessorTopology extends Serializable {

        String source();

        String target();

        static ProcessorTopology valueOf(String source, String target) {
            if (source.startsWith("stream:") && target.startsWith("computation:")) {
                return new ProcessorStreamConsumerTopology(source, target);
            } else if (source.startsWith("computation:") && target.startsWith("stream:")) {
                return new ProcessorStreamProducerTopology(source, target);
            } else {
                throw new IllegalArgumentException(
                        "Unknown processor topology: (source: " + source + ", target: " + target + ")");
            }
        }
    }

    public record ProcessorStreamConsumerTopology(String source, String target) implements ProcessorTopology {

        public String stream() {
            return source().substring("stream:".length());
        }

        public String consumer() {
            return target().substring("computation:".length());
        }
    }

    public record ProcessorStreamProducerTopology(String source, String target) implements ProcessorTopology {

        public String stream() {
            return target().substring("stream:".length());
        }

        public String producer() {
            return source().substring("computation:".length());
        }
    }

    public record Metrics(Instant timestamp, String hostname, String ip, String nodeId, List<Metric> metrics)
            implements Serializable {

        public Metrics {
            metrics = List.copyOf(metrics);
        }
    }

    public sealed interface Metric extends Serializable {

        String key();

        Number valueOrCount();

        Map<String, String> tags();

        default String tag(String key) {
            return tags().get(key);
        }

        default boolean hasTag(String key) {
            return tags().containsKey(key);
        }
    }

    public record ValueMetric(String key, Number value, Map<String, String> tags) implements Metric {

        public ValueMetric {
            tags = Map.copyOf(tags);
        }

        @Override
        public Number valueOrCount() {
            return value();
        }
    }

    public record EmptyTimerMetric(String key, Map<String, String> tags) implements Metric {

        public EmptyTimerMetric {
            tags = Map.copyOf(tags);
        }

        @Override
        public Number valueOrCount() {
            return 0;
        }
    }

    public record TimerMetric(String key, long count, double rate1m, double rate5m, double sum, double max, double mean,
            double min, double stddev, double p50, double p95, double p99, Map<String, String> tags) implements Metric {

        public TimerMetric {
            tags = Map.copyOf(tags);
        }

        @Override
        public Number valueOrCount() {
            return count();
        }
    }
}
