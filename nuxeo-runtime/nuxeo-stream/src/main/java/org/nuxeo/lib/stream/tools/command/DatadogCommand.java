/*
 * (C) Copyright 2020 Nuxeo SA (http://nuxeo.com/) and others.
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.lib.stream.log.LogManager;

/**
 * Monitors consumer latencies to Datadog.
 *
 * @since 11.1
 */
public class DatadogCommand extends MonitorCommand {

    public static final String COMPUTATION_NAME = "LatencyMonitorDatadog";

    protected static final String NAME = "datadog";

    private static final String DEFAULT_PREFIX = "nuxeo.streams";

    protected String apiKey;

    protected List<String> tags;

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
        options.addOption(Option.builder()
                                .longOpt("api-key")
                                .desc("Datadog API KEY")
                                .required()
                                .hasArg()
                                .argName("API_KEY")
                                .build());
        options.addOption(Option.builder()
                                .longOpt("tags")
                                .desc("A comma separated list of Datadog tags, for instance: project:foo,staging:bar")
                                .hasArg()
                                .argName("TAGS")
                                .build());
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
                                .desc("The metric prefix to use if not nuxeo.streams.")
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
        verbose = cmd.hasOption("verbose");
        interval = Integer.parseInt(cmd.getOptionValue("interval", DEFAULT_INTERVAL));
        count = Integer.parseInt(cmd.getOptionValue("count", DEFAULT_COUNT));
        apiKey = cmd.getOptionValue("api-key");
        tags = getTags(cmd.getOptionValue("tags"));
        prefix = cmd.getOptionValue("prefix", getDefaultPrefix());
        initTopology(manager);
        return runProcessor(manager);
    }

    protected List<String> getTags(String tags) {
        if (tags == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(tags.split(",")).map(String::trim).collect(Collectors.toList());
    }

    @Override
    protected void initTopology(LogManager manager) {
        topology = Topology.builder()
                           .addComputation(() -> new LatencyDatadogComputation(manager, logNames, apiKey, tags, prefix,
                                   COMPUTATION_NAME, interval, count, partition, verbose, getRecordCodec(codec)),
                                   Collections.singletonList("i1:" + INPUT_STREAM))
                           .build();
    }

    @Override
    public String getDefaultPrefix() {
        return DEFAULT_PREFIX;
    }

}
