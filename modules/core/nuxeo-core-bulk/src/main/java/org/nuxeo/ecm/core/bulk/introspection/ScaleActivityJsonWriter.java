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

import org.nuxeo.common.function.ThrowableConsumer;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 2025.11
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class ScaleActivityJsonWriter extends AbstractJsonWriter<ScaleActivity> {

    @Override
    public void write(ScaleActivity scaleActivity, JsonGenerator jg) throws IOException {
        jg.writeStartObject();

        jg.writeFieldName("scale");
        write(scaleActivity.scale(), jg);

        jg.writeArrayFieldStart("nodes");
        scaleActivity.nodes().forEach(ThrowableConsumer.asConsumer(node -> write(node, jg)));
        jg.writeEndArray();

        jg.writeArrayFieldStart("computations");
        scaleActivity.computations().forEach(ThrowableConsumer.asConsumer(computation -> write(computation, jg)));
        jg.writeEndArray();

        jg.writeEndObject();
    }

    protected void write(ScaleActivity.Scale scale, JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        jg.writeNumberField("currentNodes", scale.currentNodes());
        jg.writeNumberField("bestNodes", scale.bestNodes());
        jg.writeNumberField("optimalNodes", scale.optimalNodes());
        jg.writeNumberField("metric", scale.metric());
        jg.writeEndObject();
    }

    protected void write(ScaleActivity.ClusterNode node, JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("hostname", node.hostname());
        jg.writeStringField("cpuCores", String.valueOf(node.cpuCores()));
        jg.writeStringField("created", node.created().toString());
        jg.writeStringField("ip", node.ip());
        jg.writeStringField("nodeId", node.nodeId());
        jg.writeStringField("jvmHeapSize", String.valueOf(node.jvmHeapSize().toBytes()));
        jg.writeStringField("alive", node.alive().toString());
        jg.writeStringField("type", node.type());
        jg.writeEndObject();
    }

    protected void write(ScaleActivity.ActiveComputation computation, JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("computation", computation.computation());

        jg.writeObjectFieldStart("streams");
        for (var entry : computation.streams().entrySet()) {
            jg.writeFieldName(entry.getKey());
            write(entry.getValue(), jg);
        }
        jg.writeEndObject();

        jg.writeArrayFieldStart("nodes");
        computation.nodes().forEach(ThrowableConsumer.asConsumer(node -> write(node, jg)));
        jg.writeEndArray();

        if (computation.current() != null) {
            jg.writeFieldName("current");
            write(computation.current(), jg);
        }
        if (computation.best() != null) {
            jg.writeFieldName("best");
            write(computation.best(), jg);
        }
        jg.writeEndObject();
    }

    protected void write(ScaleActivity.ActiveComputationStream stream, JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("stream", stream.stream());
        jg.writeNumberField("partitions", stream.partitions());
        jg.writeNumberField("lag", stream.lag());
        if (stream.end() != null) {
            jg.writeNumberField("end", stream.end());
        }
        if (stream.latency() != null) {
            jg.writeNumberField("latency", stream.latency().toMillis());
        }
        jg.writeEndObject();
    }

    protected void write(ScaleActivity.ActiveComputationNode node, JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("nodeId", node.nodeId());
        jg.writeNumberField("threads", node.threads());
        jg.writeNumberField("timestamp", node.timestamp().getEpochSecond());
        jg.writeNumberField("count", node.count());
        double sum = node.sum();
        double flooredSum = Math.floor(node.sum());
        if (flooredSum == sum) {
            // no decimals, cast to long to avoid exponential notation
            jg.writeNumberField("sum", (long) flooredSum);
        } else {
            // there's decimals, write as double
            jg.writeNumberField("sum", sum);
        }
        jg.writeNumberField("rate1m", node.rate1m());
        jg.writeNumberField("rate5m", node.rate5m());
        jg.writeNumberField("min", node.min());
        jg.writeNumberField("p50", node.p50());
        jg.writeNumberField("mean", node.mean());
        jg.writeNumberField("p95", node.p95());
        jg.writeNumberField("max", node.max());
        jg.writeNumberField("stddev", node.stddev());
        jg.writeEndObject();
    }

    protected void write(ScaleActivity.ActiveComputationRecommendation recommendationCurrent, JsonGenerator jg)
            throws IOException {
        jg.writeStartObject();
        jg.writeNumberField("nodes", recommendationCurrent.nodes());
        jg.writeNumberField("threads", recommendationCurrent.threads());
        jg.writeNumberField("rate1m", recommendationCurrent.rate1m());
        jg.writeNumberField("eta", recommendationCurrent.eta());
        jg.writeBooleanField("relevant", recommendationCurrent.relevant());
        jg.writeEndObject();
    }
}
