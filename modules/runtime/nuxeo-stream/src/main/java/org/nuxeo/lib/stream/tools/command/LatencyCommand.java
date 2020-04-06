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

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Watermark;
import org.nuxeo.lib.stream.log.Latency;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.Name;

/**
 * Display the current latencies of consumers.
 *
 * @since 9.3
 */
public class LatencyCommand extends Command {
    private static final Log log = LogFactory.getLog(LatencyCommand.class);

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
        options.addOption(Option.builder()
                                .longOpt("codec")
                                .desc("Codec used to read record, can be: java, avro, avroBinary, avroJson")
                                .hasArg()
                                .argName("CODEC")
                                .build());
        options.addOption(Option.builder().longOpt("verbose").desc("Display latency for each partition").build());
    }

    @Override
    public boolean run(LogManager manager, CommandLine cmd) {
        String logName = cmd.getOptionValue("log-name");
        Codec<Record> codec = getRecordCodec(cmd.getOptionValue("codec"));
        verbose = cmd.hasOption("verbose");
        if (logName != null) {
            latency(manager, Name.ofUrn(logName), codec);
        } else {
            latency(manager, codec);
        }
        return true;
    }

    protected void latency(LogManager manager, Codec<Record> codec) {
        log.info("# " + manager);
        for (Name name : manager.listAll()) {
            latency(manager, name, codec);
        }
    }

    protected void latency(LogManager manager, Name name, Codec<Record> codec) {
        log.info("## Log: " + name + " partitions: " + manager.size(name));
        List<Name> consumers = manager.listConsumerGroups(name);
        if (verbose && consumers.isEmpty()) {
            // add a fake group to get info on end positions
            consumers.add(Name.ofUrn("admin/tools"));
        }
        try {
            consumers.forEach(group -> renderLatency(group, manager.<Record> getLatencyPerPartition(name, group, codec,
                    (rec -> Watermark.ofValue(rec.getWatermark()).getTimestamp()), (Record::getKey))));
        } catch (IllegalStateException e) {
            // happen when this is not a stream of Record
            log.error(e.getMessage());
        }
    }

    protected void renderLatency(Name group, List<Latency> latencies) {
        log.info(String.format("### Group: %s", group));
        log.info(
                "| partition | lag | latencyMs | latency | posTimestamp | posDate | curDate | pos | end | posOffset | endOffset | posKey |\n"
                        + "| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |");
        Latency all = Latency.of(latencies);
        log.info(String.format("|All|%d|%d|%s|%d|%s|%s|%d|%d|%d|%d|%s|", all.lag().lag(), all.latency(),
                formatInterval(all.latency()), all.lower(), formatDate(all.lower()), formatDate(all.upper()),
                all.lag().lower(), all.lag().upper(), all.lag().lowerOffset(), all.lag().upperOffset(), all.key()));
        if (verbose && latencies.size() > 1) {
            AtomicInteger i = new AtomicInteger();
            latencies.forEach(lat -> log.info(String.format("|%d|%d|%d|%s|%d|%s|%s|%d|%d|%d|%d|%s|",
                    i.getAndIncrement(), lat.lag().lag(), lat.latency(), formatInterval(lat.latency()), lat.lower(),
                    formatDate(lat.lower()), formatDate(lat.upper()), lat.lag().lower(), lat.lag().upper(),
                    lat.lag().lowerOffset(), lat.lag().upperOffset(), lat.key())));
        }
    }

    protected String formatDate(long timestamp) {
        if (timestamp > 0) {
            return Instant.ofEpochMilli(timestamp).toString();
        }
        return "NA";
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
