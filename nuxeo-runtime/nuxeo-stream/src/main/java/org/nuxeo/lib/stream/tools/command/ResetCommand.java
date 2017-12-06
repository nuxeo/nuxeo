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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogLag;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogTailer;

/**
 * @since 9.3
 */
public class ResetCommand extends Command {

    protected static final String NAME = "reset";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void updateOptions(Options options) {
        options.addOption(
                Option.builder().longOpt("name").desc("Log name").required().hasArg().argName("LOG_NAME").build());
        options.addOption(
                Option.builder().longOpt("group").desc("Consumer group").required().hasArg().argName("GROUP").build());
    }

    @Override
    public void run(LogManager manager, CommandLine cmd) throws InterruptedException {
        String name = cmd.getOptionValue("name");
        String group = cmd.getOptionValue("group");
        reset(manager, group, name);
    }

    protected void reset(LogManager manager, String group, String name) {
        LogLag lag = manager.getLag(name, group);
        long pos = lag.lower();
        try (LogTailer<Record> tailer = manager.createTailer(group, name)) {
            tailer.reset();
        }
        System.out.println(String.format("# Reset log %s, group: %s, from: %s to 0", name, group, pos));
    }
}
