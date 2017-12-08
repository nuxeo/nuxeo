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
import org.nuxeo.lib.stream.log.LogLag;
import org.nuxeo.lib.stream.log.LogManager;

/**
 * @since 9.3
 */
public class LagCommand extends Command {

    protected static final String NAME = "lag";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void updateOptions(Options options) {
        options.addOption(
                Option.builder("l").longOpt("log-name").desc("Log name").hasArg().argName("LOG_NAME").build());
    }

    @Override
    public boolean run(LogManager manager, CommandLine cmd) throws InterruptedException {
        String name = cmd.getOptionValue("log-name");
        if (name != null) {
            lag(manager, name);
        } else {
            lag(manager);
        }
        return true;
    }

    protected void lag(LogManager manager) {
        System.out.println("# " + manager);
        for (String name : manager.listAll()) {
            lag(manager, name);
        }
    }

    protected void lag(LogManager manager, String name) {
        System.out.println("## Log: " + name);
        manager.listConsumerGroups(name).forEach(group -> renderLag(group, manager.getLag(name, group)));
    }

    protected void renderLag(String group, LogLag lag) {
        System.out.println("### Group: " + group);
        System.out.println("| lag | pos | end | posOffset | endOffset |\n" + "| --- | ---: | ---: | ---: | ---: |");
        System.out.println(String.format("|%d|%d|%d|%d|%d|", lag.lag(), lag.lower(), lag.upper(), lag.lowerOffset(),
                lag.upperOffset()));
    }
}
