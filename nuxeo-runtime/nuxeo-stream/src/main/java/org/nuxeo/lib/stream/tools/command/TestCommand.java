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
import org.apache.commons.cli.Options;
import org.nuxeo.lib.stream.log.LogManager;

/**
 * @since 10.1
 */
public class TestCommand extends Command {

    protected static final String NAME = "test";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void updateOptions(Options options) {
    }

    @Override
    public boolean run(LogManager manager, CommandLine cmd) {
        return true;
    }
}
