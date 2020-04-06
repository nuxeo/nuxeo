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
package org.nuxeo.lib.stream.tools.command;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.coursera.metrics.datadog.model.DatadogGauge;
import org.coursera.metrics.datadog.transport.HttpTransport;
import org.coursera.metrics.datadog.transport.Transport;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.Latency;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.log.internals.LogPartitionGroup;

/**
 * A computation that sends periodically latencies to Datadog.
 *
 * @since 11.1
 */
public class LatencyDatadogComputation extends LatencyTrackerComputation {

    private static final Log log = LogFactory.getLog(LatencyDatadogComputation.class);

    protected static final String HOSTNAME_UNKNOWN = "unknown";

    protected final String apiKey;

    protected final List<String> tags;

    protected final String basePrefix;

    protected final boolean partition;

    protected final String hostname;

    protected HttpTransport transport;

    public LatencyDatadogComputation(LogManager manager, List<Name> logNames, String apiKey, List<String> tags,
            String basePrefix, String computationName, int intervalSecond, int count, boolean partition,
            boolean verbose, Codec<Record> codec) {
        super(manager, logNames, computationName, intervalSecond, count, verbose, codec, 0);
        this.apiKey = apiKey;
        this.tags = tags;
        this.basePrefix = basePrefix;
        this.partition = partition;
        hostname = getHostName();
    }

    protected String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName().split("\\.")[0];
        } catch (UnknownHostException e) {
            return HOSTNAME_UNKNOWN;
        }
    }

    @Override
    public void init(ComputationContext context) {
        super.init(context);
        transport = new HttpTransport.Builder().withApiKey(apiKey).build();
    }

    @Override
    protected void processLatencies(ComputationContext context, LogPartitionGroup logGroup, List<Latency> latencies) {
        Latency groupLatency = Latency.of(latencies);
        publishMetrics(groupLatency, basePrefix, "all", logGroup.name, logGroup.group);
        if (!partition) {
            return;
        }
        for (int part = 0; part < latencies.size(); part++) {
            publishMetrics(groupLatency, basePrefix, String.format("%02d", part), logGroup.name, logGroup.group);
        }
    }

    protected void publishMetrics(Latency latency, String prefix, String partition, Name stream, Name group) {
        if (verbose) {
            log.info(latency.toString());
        }
        // upper is the time when the latency has been measured
        long metricTime = latency.upper() / 1000;
        List<String> mTags = new ArrayList<>(tags.size() + 3);
        mTags.addAll(tags);
        mTags.add("stream:" + stream);
        mTags.add("consumer:" + group);
        mTags.add("partition:" + partition);
        try {
            Transport.Request request = transport.prepare();
            request.addGauge(new DatadogGauge(prefix + ".lag", latency.lag().lag(), metricTime, hostname, mTags));
            request.addGauge(new DatadogGauge(prefix + ".end", latency.lag().upper(), metricTime, hostname, mTags));
            request.addGauge(new DatadogGauge(prefix + ".pos", latency.lag().lower(), metricTime, hostname, mTags));
            request.addGauge(new DatadogGauge(prefix + ".latency", latency.latency(), metricTime, hostname, mTags));
            request.send();
        } catch (IOException e) {
            log.error("Fail to prepare metric to datadog " + prefix + " " + latency, e);
        } catch (Exception e) {
            log.error("Fail to send metric to datadog " + prefix + " " + latency, e);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (transport != null) {
            try {
                transport.close();
            } catch (IOException e) {
                log.debug("Error when closing Datadog client: ", e);
            }
            transport = null;
        }
    }
}
