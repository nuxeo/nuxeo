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

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import org.nuxeo.common.function.ThrowableBiConsumer;
import org.nuxeo.common.function.ThrowableConsumer;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 2025.12
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class StreamIntrospectionJsonWriter extends AbstractJsonWriter<StreamIntrospection> {

    @Override
    public void write(StreamIntrospection entity, JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        // streams
        writeEntityArrayField("streams", entity.streams(), jg);
        // processors
        jg.writeArrayFieldStart("processors");
        entity.processors().forEach(ThrowableConsumer.asConsumer(processor -> write(processor, jg)));
        jg.writeEndArray();
        // metrics
        jg.writeArrayFieldStart("metrics");
        entity.metrics().forEach(ThrowableConsumer.asConsumer(metrics -> write(metrics, jg)));
        jg.writeEndArray();
        jg.writeEndObject();
    }

    protected void write(StreamIntrospection.Processor processor, JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        // metadata
        jg.writeFieldName("metadata");
        write(processor.metadata(), jg);
        // computation
        jg.writeArrayFieldStart("computations");
        processor.computations().forEach(ThrowableConsumer.asConsumer(computation -> write(computation, jg)));
        jg.writeEndArray();
        // topology
        writeEntityArrayField("topology", processor.topology(), jg);
        jg.writeEndObject();
    }

    protected void write(StreamIntrospection.ProcessorMetadata metadata, JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("processorName", metadata.processorName());
        if (metadata.nodeId() != null) {
            jg.writeStringField("nodeId", metadata.nodeId());
        }
        jg.writeStringField("hostname", metadata.hostname());
        jg.writeStringField("ip", metadata.ip());
        jg.writeNumberField("cpuCores", metadata.cpuCores());
        jg.writeStringField("jvmHeapSize", String.valueOf(metadata.jvmHeapSize().toBytes()));
        jg.writeStringField("created", String.valueOf(metadata.created().getEpochSecond()));
        jg.writeEndObject();
    }

    protected void write(StreamIntrospection.ProcessorComputation computation, JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("name", computation.name());
        jg.writeNumberField("threads", computation.threads());
        jg.writeBooleanField("continueOnFailure", computation.continueOnFailure());
        jg.writeNumberField("batchCapacity", computation.batchCapacity());
        jg.writeNumberField("batchThresholdMs", computation.batchThreshold().toMillis());
        jg.writeNumberField("maxRetries", computation.maxRetries());
        jg.writeNumberField("retryDelayMs", computation.retryDelay().toMillis());
        jg.writeEndObject();
    }

    protected void write(StreamIntrospection.Metrics metrics, JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("timestamp", String.valueOf(metrics.timestamp().getEpochSecond()));
        jg.writeStringField("hostname", metrics.hostname());
        jg.writeStringField("ip", metrics.ip());
        jg.writeStringField("nodeId", metrics.nodeId());
        jg.writeArrayFieldStart("metrics");
        metrics.metrics().forEach(ThrowableConsumer.asConsumer(metric -> write(metric, jg)));
        jg.writeEndArray();
        jg.writeEndObject();
    }

    protected void write(StreamIntrospection.Metric metric, JsonGenerator jg) throws IOException {
        if (metric instanceof StreamIntrospection.ValueMetric valueMetric) {
            write(valueMetric, jg);
        } else if (metric instanceof StreamIntrospection.EmptyTimerMetric valueMetric) {
            write(valueMetric, jg);
        } else if (metric instanceof StreamIntrospection.TimerMetric valueMetric) {
            write(valueMetric, jg);
        }
    }

    protected void write(StreamIntrospection.ValueMetric metric, JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("k", metric.key());
        writeNumberFieldIfNotNull("v", metric.value(), jg);
        metric.tags().forEach(ThrowableBiConsumer.asBiConsumer(jg::writeStringField));
        jg.writeEndObject();
    }

    protected void write(StreamIntrospection.EmptyTimerMetric metric, JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("k", metric.key());
        jg.writeNumberField("count", 0);
        metric.tags().forEach(ThrowableBiConsumer.asBiConsumer(jg::writeStringField));
        jg.writeEndObject();
    }

    protected void write(StreamIntrospection.TimerMetric metric, JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("k", metric.key());
        jg.writeNumberField("count", metric.count());
        jg.writeNumberField("rate1m", metric.rate1m());
        jg.writeNumberField("rate5m", metric.rate5m());
        jg.writeNumberField("sum", metric.sum());
        jg.writeNumberField("max", metric.max());
        jg.writeNumberField("mean", metric.mean());
        jg.writeNumberField("min", metric.min());
        jg.writeNumberField("stddev", metric.stddev());
        jg.writeNumberField("p50", metric.p50());
        jg.writeNumberField("p95", metric.p95());
        jg.writeNumberField("p99", metric.p99());
        metric.tags().forEach(ThrowableBiConsumer.asBiConsumer(jg::writeStringField));
        jg.writeEndObject();
    }
}
