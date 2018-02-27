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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Watermark;
import org.nuxeo.lib.stream.log.Latency;
import org.nuxeo.lib.stream.log.LogManager;

/**
 * @since 9.3
 */
public class LatencyCommand extends Command {

    protected static final String NAME = "latency";

    protected boolean verbose = false;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void updateOptions(Options options) {
        options.addOption(Option.builder("l")
                                .longOpt("log-name")
                                .desc("Log name of a stream containing computation.Record")
                                .hasArg()
                                .argName("LOG_NAME")
                                .build());
        options.addOption(Option.builder().longOpt("verbose").desc("Display latency for each partition").build());
    }

    @Override
    public boolean run(LogManager manager, CommandLine cmd) {
        String name = cmd.getOptionValue("log-name");
        verbose = cmd.hasOption("verbose");
        if (name != null) {
            latency(manager, name);
        } else {
            latency(manager);
        }
        return true;
    }

    protected void latency(LogManager manager) {
        System.out.println("# " + manager);
        for (String name : manager.listAll()) {
            latency(manager, name);
        }
    }

    protected void latency(LogManager manager, String name) {
        System.out.println("## Log: " + name + " partitions: " + manager.getAppender(name).size());
        List<String> consumers = manager.listConsumerGroups(name);
        if (verbose && consumers.isEmpty()) {
            // add a fake group to get info on end positions
            consumers.add("tools");
        }
        try {
            consumers.forEach(group -> renderLatency(group, manager.<Record> getLatencyPerPartition(name, group,
                    (rec -> Watermark.ofValue(rec.watermark).getTimestamp()))));
        } catch (IllegalStateException e) {
            // happen when this is not a stream of Record
            System.err.println(e.getMessage());
        }
    }

    protected void renderLatency(String group, List<Latency> latencies) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        System.out.println(String.format("### Group: %s", group));
        System.out.println(
                "| partition | lag | latency_ms | latency | watermark_ts | watermark | date | pos | end | posOffset | endOffset |\n"
                        + "| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |");
        Latency all = Latency.of(latencies);
        System.out.println(String.format("|All|%d|%d|%s|%d|%s|%s|%d|%d|%d|%d|", all.lag().lag(), all.latency(),
                formatInterval(all.latency()), all.lower(), dateFormat.format(new Date(all.lower())),
                dateFormat.format(new Date(all.upper())), all.lag().lower(), all.lag().upper(), all.lag().lowerOffset(),
                all.lag().upperOffset()));
        if (verbose && latencies.size() > 1) {
            AtomicInteger i = new AtomicInteger();
            latencies.forEach(lat -> System.out.println(String.format("|%d|%d|%d|%s|%d|%s|%s|%d|%d|%d|%d|",
                    i.getAndIncrement(), lat.lag().lag(), lat.latency(), formatInterval(lat.latency()), lat.lower(),
                    dateFormat.format(new Date(lat.lower())), dateFormat.format(new Date(lat.upper())),
                    lat.lag().lower(), lat.lag().upper(), lat.lag().lowerOffset(), lat.lag().upperOffset())));
        }
    }

    protected static String formatInterval(final long l) {
        if (l == 0) {
            return "NA";
        }
        final long hr = TimeUnit.MILLISECONDS.toHours(l);
        final long min = TimeUnit.MILLISECONDS.toMinutes(l - TimeUnit.HOURS.toMillis(hr));
        final long sec = TimeUnit.MILLISECONDS.toSeconds(
                l - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
        final long ms = TimeUnit.MILLISECONDS.toMillis(
                l - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min) - TimeUnit.SECONDS.toMillis(sec));
        return String.format("%02d:%02d:%02d.%03d", hr, min, sec, ms);
    }
}
