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
package org.nuxeo.lib.stream.tools.command;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.nuxeo.lib.stream.computation.Settings;
import org.nuxeo.lib.stream.computation.StreamManager;
import org.nuxeo.lib.stream.computation.StreamProcessor;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.lib.stream.computation.log.LogStreamManager;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.Name;

/**
 * Monitors consumer latencies to graphite
 *
 * @since 10.3
 */
public class MonitorCommand extends Command {

    public static final String COMPUTATION_NAME = "LatencyMonitor";

    public static final String INPUT_STREAM = "log_null";

    public static final String INTERNAL_LOG_PREFIX = "_";

    protected static final String NAME = "monitor";

    protected static final String DEFAULT_INTERVAL = "60";

    protected static final String DEFAULT_COUNT = "-1";

    protected static final String ALL_LOGS = "all";

    protected static final String DEFAULT_PORT = "2003";

    protected boolean verbose = false;

    protected boolean partition = false;

    protected List<Name> logNames;

    protected int interval;

    protected int count;

    protected Topology topology;

    protected StreamProcessor processor;

    protected String codec;

    protected String host;

    protected int port;

    protected boolean udp;

    protected String prefix;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void updateOptions(Options options) {
        options.addOption(Option.builder("l")
                                .longOpt("log-name")
                                .desc("Monitor consumers latency for this LOG, must be a computation Record, "
                                        + "can be a comma separated list of log names or ALL")
                                .required()
                                .hasArg()
                                .argName("LOG_NAME")
                                .build());
        options.addOption(Option.builder("h")
                                .longOpt("host")
                                .desc("The carbon server host")
                                .required()
                                .hasArg()
                                .argName("HOST")
                                .build());
        options.addOption(Option.builder("p")
                                .longOpt("port")
                                .desc("The carbon server port if not 2003")
                                .hasArg()
                                .argName("PORT")
                                .build());
        options.addOption("u", "udp", false, "Carbon instance is listening using UDP");
        options.addOption(Option.builder().longOpt("partition").desc("Report metrics for each partition").build());
        options.addOption(Option.builder("i")
                                .longOpt("interval")
                                .desc("send latency spaced at the specified interval in seconds")
                                .hasArg()
                                .argName("INTERVAL")
                                .build());
        options.addOption(Option.builder("c")
                                .longOpt("count")
                                .desc("number of times the latency information is sent")
                                .hasArg()
                                .argName("COUNT")
                                .build());
        options.addOption(Option.builder()
                                .longOpt("prefix")
                                .desc("The metric prefix to use if not server.<hostname>.nuxeo.streams.")
                                .hasArg()
                                .argName("PREFIX")
                                .build());
        options.addOption(Option.builder()
                                .longOpt("codec")
                                .desc("Codec used to read record, can be: java, avro, avroBinary, avroJson")
                                .hasArg()
                                .argName("CODEC")
                                .build());
        options.addOption(Option.builder().longOpt("verbose").build());
    }

    @Override
    public boolean run(LogManager manager, CommandLine cmd) {
        logNames = getLogNames(manager, cmd.getOptionValue("log-name"));
        codec = cmd.getOptionValue("codec");
        partition = cmd.hasOption("partition");
        verbose = cmd.hasOption("verbose");
        interval = Integer.parseInt(cmd.getOptionValue("interval", DEFAULT_INTERVAL));
        count = Integer.parseInt(cmd.getOptionValue("count", DEFAULT_COUNT));
        port = Integer.parseInt(cmd.getOptionValue("port", DEFAULT_PORT));
        host = cmd.getOptionValue("host");
        udp = cmd.hasOption("udp");
        prefix = cmd.getOptionValue("prefix", getDefaultPrefix());
        initTopology(manager);
        return runProcessor(manager);
    }

    protected List<Name> getLogNames(LogManager manager, String names) {
        if (ALL_LOGS.equalsIgnoreCase(names)) {
            return manager.listAll()
                          .stream()
                          .filter(name -> !name.getUrn().startsWith(INTERNAL_LOG_PREFIX))
                          .filter(name -> !name.getUrn().startsWith(INPUT_STREAM))
                          .collect(Collectors.toList());
        }
        List<Name> ret = Arrays.asList(names.split(",")).stream().map(Name::ofUrn).collect(Collectors.toList());
        if (ret.isEmpty()) {
            throw new IllegalArgumentException("No log name provided or found.");
        }
        for (Name name : ret) {
            if (!manager.exists(name)) {
                throw new IllegalArgumentException("Unknown log name: " + name);
            }
        }
        return ret;
    }

    protected void initTopology(LogManager manager) {
        topology = Topology.builder()
                           .addComputation(
                                   () -> new LatencyMonitorComputation(manager, logNames, host, port, udp, prefix,
                                           COMPUTATION_NAME, interval, count, partition, verbose,
                                           getRecordCodec(codec)),
                                   Collections.singletonList("i1:" + INPUT_STREAM))
                           .build();
    }

    public String getDefaultPrefix() {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName().split("\\.")[0];
        } catch (UnknownHostException e) {
            hostname = "unknown";
        }
        return "servers." + hostname + ".nuxeo.streams.";
    }

    protected boolean runProcessor(LogManager manager) {
        StreamManager streamManager = new LogStreamManager(manager);
        Settings settings = new Settings(1, 1, getRecordCodec(codec));
        processor = streamManager.registerAndCreateProcessor("monitor", topology, settings);
        processor.start();
        while (!processor.isTerminated()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                processor.shutdown();
                return false;
            }
        }
        return true;
    }

}
