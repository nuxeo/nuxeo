/*
 * (C) Copyright 2012-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.connect.tools.report.viewer;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Prints a report in an human friendly way.
 *
 *
 * @since 8.4
 *
 */
public class Viewer {

    public static void main(String[] varargs) throws IOException, ParseException {

        class Arguments {
            Options options = new Options()
                    .addOption(Option.builder("i").longOpt("input").hasArg().argName("file").desc("report input file").build())
                    .addOption(Option.builder("o").longOpt("output").hasArg().argName("file").desc("thread dump output file").build());
            final CommandLine commandline = new DefaultParser().parse(options, varargs);

            Arguments() throws ParseException {

            }

            InputStream input() throws IOException {
                if (!commandline.hasOption('i')) {
                    return System.in;
                }
                return Files.newInputStream(Paths.get(commandline.getOptionValue('i')));
            }

            PrintStream output() throws IOException {
                if (!commandline.hasOption('o')) {
                    return System.out;
                }
                return new PrintStream(commandline.getOptionValue('o'));
            }

        }

        Arguments arguments = new Arguments();
        final JsonFactory jsonFactory = new JsonFactory();
        PrintStream output = arguments.output();
        JsonParser parser = jsonFactory.createParser(arguments.input());
        ObjectMapper mapper = new ObjectMapper();
        while (!parser.isClosed() && parser.nextToken() != JsonToken.NOT_AVAILABLE) {
            String hostid = parser.nextFieldName();
            output.println(hostid);
            {
                parser.nextToken();
                while (parser.nextToken() == JsonToken.FIELD_NAME) {
                    if ("mx-thread-dump".equals(parser.getCurrentName())) {
                        parser.nextToken(); // start mx-thread-dump report
                        while (parser.nextToken() == JsonToken.FIELD_NAME) {
                            if ("value".equals(parser.getCurrentName())) {
                                parser.nextToken();
                                printThreadDump(mapper.readTree(parser), output);
                            } else {
                                parser.nextToken();
                                parser.skipChildren();
                            }
                        }
                    } else if ("mx-thread-deadlocked".equals(parser.getCurrentName())) {
                        parser.nextToken();
                        while (parser.nextToken() == JsonToken.FIELD_NAME) {
                            if ("value".equals(parser.getCurrentName())) {
                                if (parser.nextToken() == JsonToken.START_ARRAY) {
                                    printThreadDeadlocked(mapper.readerFor(Long.class).readValue(parser), output);
                                }
                            } else {
                                parser.nextToken();
                                parser.skipChildren();
                            }
                        }
                    } else if ("mx-thread-monitor-deadlocked".equals(parser.getCurrentName())) {
                        parser.nextToken();
                        while (parser.nextToken() == JsonToken.FIELD_NAME) {
                            if ("value".equals(parser.getCurrentName())) {
                                if (parser.nextToken() == JsonToken.START_ARRAY) {
                                    printThreadMonitorDeadlocked(mapper.readerFor(Long.class).readValues(parser), output);
                                }
                            } else {
                                parser.nextToken();
                                parser.skipChildren();
                            }
                        }
                    } else {
                        parser.nextToken();
                        parser.skipChildren();
                    }
                }
            }
        }

    }

    public static PrintStream printThreadDump(ArrayNode infos, PrintStream out) throws IOException {
        StringBuilder sb = new StringBuilder();
        new ThreadDumpPrinter(infos).print(sb);
        out.print(sb.toString());
        return out;
    }

    public static PrintStream printThreadDeadlocked(MappingIterator<Long> values, PrintStream out) throws IOException {
        out.print("deadlocked " + Arrays.toString(values.readAll().toArray()));
        return out;
    }

    public static PrintStream printThreadMonitorDeadlocked(MappingIterator<Long> values, PrintStream out) throws IOException {
        out.print("monitor deadlocked " + Arrays.toString(values.readAll().toArray()));
        return out;
    }

}
