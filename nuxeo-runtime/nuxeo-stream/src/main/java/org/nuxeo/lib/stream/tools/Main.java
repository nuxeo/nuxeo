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
package org.nuxeo.lib.stream.tools;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.chronicle.ChronicleLogManager;
import org.nuxeo.lib.stream.log.kafka.KafkaLogManager;
import org.nuxeo.lib.stream.tools.command.Command;
import org.nuxeo.lib.stream.tools.command.HelpCommand;

/**
 * @since 9.3
 */
public class Main {
    private static final Log log = LogFactory.getLog(Main.class);

    protected static final String NUXEO_KAFKA_FILE_CONF = "nxserver/config/kafka-config.xml";

    protected static final String NUXEO_KAFKA_CONF = "default";

    protected static final String CHRONICLE_OPT = "chronicle";

    protected static final String KAFKA_OPT = "kafka";

    protected final Map<String, Command> commandMap = new HashMap<>();

    protected final Options options = new Options();

    protected String command;

    protected LogManager manager;

    public static void main(final String[] args) {
        boolean success = new Main().run(args);
        if (!success) {
            System.exit(-1);
        }
    }

    public boolean run(String[] args) {
        initDefaultOptions();
        if (args == null || args.length == 0 || args[0].trim().isEmpty()) {
            command = "help";
            return runWithArgs(null);
        }
        command = Objects.requireNonNull(args)[0];
        return runWithArgs(Arrays.copyOfRange(args, 1, args.length));
    }

    protected boolean runWithArgs(String[] args) {
        try {
            Command cmd = getCommand();
            cmd.updateOptions(options);
            CommandLineParser parser = new DefaultParser();
            CommandLine cmdLine = parser.parse(options, args);
            createManager(cmd, cmdLine);
            return cmd.run(manager, cmdLine);
        } catch (ParseException e) {
            log.error("Parse error: " + e.getMessage() + ", try: help " + command);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted: " + e.getMessage());
        }
        return false;
    }

    protected void createManager(Command cmd, CommandLine cmdLine) {
        if (cmd instanceof HelpCommand) {
            return;
        }
        if (cmdLine.hasOption(CHRONICLE_OPT)) {
            createChronicleManager(cmdLine.getOptionValue(CHRONICLE_OPT));
        } else if (cmdLine.hasOption(KAFKA_OPT) || cmdLine.hasOption("k")) {
            String contribPath = cmdLine.getOptionValue(KAFKA_OPT, NUXEO_KAFKA_FILE_CONF);
            createKafkaManager(contribPath, cmdLine.getOptionValue("kafka-config", NUXEO_KAFKA_CONF));
        } else {
            throw new IllegalArgumentException("Missing required option: --chronicle or --kafka");
        }
    }

    protected void createKafkaManager(String kafkaContribution, String kafkaConfig) {
        KafkaConfigParser config = new KafkaConfigParser(Paths.get(kafkaContribution), kafkaConfig);
        manager = new KafkaLogManager(config.getPrefix(), config.getProducerProperties(),
                config.getConsumerProperties());
    }

    protected void createChronicleManager(String basePath) {
        manager = new ChronicleLogManager(Paths.get(basePath));
    }

    protected Command getCommand() {
        if (commandMap.isEmpty()) {
            new CommandRegistry().commands().forEach(cmd -> commandMap.put(cmd.name(), cmd));
        }
        if ("-h".equals(command) || "--help".equals(command)) {
            command = "help";
        } else if (!commandMap.containsKey(command)) {
            throw new IllegalArgumentException("Unknown command: " + command);
        }
        return commandMap.get(command);
    }

    protected void initDefaultOptions() {
        options.addOption(Option.builder()
                                .longOpt(CHRONICLE_OPT)
                                .desc("Base path of the Chronicle Queue LogManager")
                                .hasArg()
                                .argName("PATH")
                                .build());
        options.addOption(Option.builder()
                                .longOpt(KAFKA_OPT)
                                .desc("Nuxeo Kafka configuration contribution file: nxserver/config/kafka-config.xml")
                                .hasArg()
                                .argName("PATH")
                                .build());
        options.addOption(Option.builder()
                                .longOpt("kafka-config")
                                .desc("Config name in the Nuxeo Kafka configuration contribution")
                                .hasArg()
                                .argName("CONFIG")
                                .build());
        options.addOption("k", "nuxeo-kafka", false, "Use the default Nuxeo Kafka configuration");
    }

    public LogManager getLogManager(String[] args) {
        run(args);
        return manager;
    }
}
