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
import org.nuxeo.lib.stream.tools.renderer.MarkdownRenderer;
import org.nuxeo.lib.stream.tools.renderer.Renderer;
import org.nuxeo.lib.stream.tools.renderer.TextRenderer;

/**
 * @since 9.3
 */
public abstract class Command {
    public abstract String name();

    public abstract void updateOptions(Options options);

    public abstract boolean run(LogManager manager, CommandLine cmd) throws InterruptedException;

    protected Renderer getRecordRenderer(String render) {
        switch (render) {
        case "markdown":
            return new MarkdownRenderer();
        default:
            return new TextRenderer();
        }
    }

}
