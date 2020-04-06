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

import java.util.Arrays;
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
 * Track consumer positions so they can be restored in case of fail-over
 *
 * @since 10.1
 */
public class TrackerCommand extends Command {

    public static final String COMPUTATION_NAME = "LatencyTracker";

    public static final String INPUT_STREAM = "input/null";

    public static final String INTERNAL_LOG_PREFIX = "_";

    protected static final String NAME = "tracker";

    protected static final String DEFAULT_INTERVAL = "60";

    protected static final String DEFAULT_COUNT = "-1";

    protected static final String ALL_LOGS = "all";

    protected static final String DEFAULT_LATENCIES_LOG = "_consumer_latencies";

    protected boolean verbose = false;

    protected String output;

    protected List<Name> logNames;

    protected int interval;

    protected int count;

    protected Topology topology;

    protected StreamProcessor processor;

    protected String codec;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void updateOptions(Options options) {
        options.addOption(Option.builder("l")
                                .longOpt("log-name")
                                .desc("Track consumers latency for this LOG, must be a computation Record, "
                                        + "can be a comma separated list of log names or ALL")
                                .required()
                                .hasArg()
                                .argName("LOG_NAME")
                                .build());
        options.addOption(Option.builder("o")
                                .longOpt("log-output")
                                .desc("Log name of the output")
                                .hasArg()
                                .argName("LOG_OUTPUT")
                                .build());
        options.addOption(Option.builder("i")
                                .longOpt("interval")
                                .desc("send latency spaced at the specified interval in seconds")
                                .hasArg()
                                .argName("INTERVAL")
                                .build());
        options.addOption(Option.builder("c")
                                .longOpt("count")
                                .desc("number of time to send the latency information")
                                .hasArg()
                                .argName("COUNT")
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
        output = cmd.getOptionValue("log-output", DEFAULT_LATENCIES_LOG);
        codec = cmd.getOptionValue("codec");
        verbose = cmd.hasOption("verbose");
        interval = Integer.parseInt(cmd.getOptionValue("interval", DEFAULT_INTERVAL));
        count = Integer.parseInt(cmd.getOptionValue("count", DEFAULT_COUNT));

        initTopology(manager);
        return runProcessor(manager);
    }

    protected List<Name> getLogNames(LogManager manager, String names) {
        if (ALL_LOGS.equalsIgnoreCase(names)) {
            return manager.listAll().stream().filter(name -> !INPUT_STREAM.equals(name.getName()))
                          .collect(Collectors.toList());
        }
        List<Name> ret = Arrays.stream(names.split(",")).map(Name::ofUrn).collect(Collectors.toList());
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
                                   () -> new LatencyTrackerComputation(manager, logNames, COMPUTATION_NAME, interval,
                                           count, verbose, getRecordCodec(codec), 1),
                                   Arrays.asList("i1:" + INPUT_STREAM, "o1:" + output))
                           .build();
    }

    protected boolean runProcessor(LogManager manager) {
        StreamManager streamManager = new LogStreamManager(manager);
        Settings settings = new Settings(1, 1, getRecordCodec(codec));
        processor = streamManager.registerAndCreateProcessor("tracker", topology, settings);
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
