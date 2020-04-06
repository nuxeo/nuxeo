/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.Latency;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.log.internals.LogPartitionGroup;

import io.dropwizard.metrics5.graphite.Graphite;
import io.dropwizard.metrics5.graphite.GraphiteSender;
import io.dropwizard.metrics5.graphite.GraphiteUDP;

/**
 * A computation that sends periodically latencies to graphite.
 *
 * @since 10.3
 */
public class LatencyMonitorComputation extends LatencyTrackerComputation {

    private static final Log log = LogFactory.getLog(LatencyMonitorComputation.class);

    protected final String host;

    protected final int port;

    protected final boolean udp;

    protected final String basePrefix;

    protected GraphiteSender graphite;

    protected final boolean partition;

    @Deprecated
    public LatencyMonitorComputation(LogManager manager, List<Name> logNames, String host, int port, boolean udp,
            String basePrefix, String computationName, int intervalSecond, int count, boolean verbose,
            Codec<Record> codec) {
        this(manager, logNames, host, port, udp, basePrefix, computationName, intervalSecond, count, true, verbose,
                codec);
    }

    public LatencyMonitorComputation(LogManager manager, List<Name> logNames, String host, int port, boolean udp,
                                     String basePrefix, String computationName, int intervalSecond, int count, boolean partition,
                                     boolean verbose, Codec<Record> codec) {
        super(manager, logNames, computationName, intervalSecond, count, verbose, codec, 0);
        this.host = host;
        this.port = port;
        this.udp = udp;
        this.partition = partition;
        this.basePrefix = basePrefix;
    }

    @Override
    public void init(ComputationContext context) {
        super.init(context);
        if (udp) {
            graphite = new GraphiteUDP(host, port);
        } else {
            graphite = new Graphite(host, port);
        }
        try {
            graphite.connect();
        } catch (IOException e) {
            throw new IllegalStateException("Fail to connect to " + host + ":" + port, e);
        }
    }

    @Override
    protected void processLatencies(ComputationContext context, LogPartitionGroup logGroup, List<Latency> latencies) {
        Latency groupLatency = Latency.of(latencies);
        publishMetrics(groupLatency, String.format("%s%s.%s.all.", basePrefix, logGroup.group, logGroup.name));
        if (!partition) {
            return;
        }
        for (int part = 0; part < latencies.size(); part++) {
            publishMetrics(latencies.get(part),
                    String.format("%s%s.%s.p%02d.", basePrefix, logGroup.group, logGroup.name, part));
        }
    }

    protected void publishMetrics(Latency latency, String prefix) {
        if (verbose) {
            log.info(latency.toString());
        }
        // upper is the time when the latency has been measured
        long metricTime = latency.upper() / 1000;
        try {
            graphite.send(prefix + "lag", Long.toString(latency.lag().lag()), metricTime);
            graphite.send(prefix + "end", Long.toString(latency.lag().upper()), metricTime);
            graphite.send(prefix + "pos", Long.toString(latency.lag().lower()), metricTime);
            graphite.send(prefix + "latency", Long.toString(latency.latency()), metricTime);
        } catch (IOException e) {
            log.error("Fail to send metric to graphite " + prefix + " " + latency, e);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (graphite != null) {
            try {
                graphite.close();
            } catch (IOException e) {
                log.debug("Error when closing graphite socket: ", e);
            }
            graphite = null;
        }
    }
}
