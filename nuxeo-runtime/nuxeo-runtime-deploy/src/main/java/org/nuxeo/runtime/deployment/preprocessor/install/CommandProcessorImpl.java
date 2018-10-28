/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.deployment.preprocessor.install;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class CommandProcessorImpl implements CommandProcessor {

    final List<Command> commands = new ArrayList<Command>();

    Log log;

    @Override
    public List<Command> getCommands() {
        return commands;
    }

    // TODO: I think this should throw an exception in case of failure.
    @Override
    public void exec(CommandContext ctx) {
        try {
            for (Command cmd : commands) {
                if (log != null && log.isDebugEnabled()) {
                    log.debug("Executing: " + cmd.toString(ctx));
                }
                cmd.exec(ctx);
            }
        } catch (IOException e) {
            if (log != null) {
                log.error(e, e);
            }
        }
    }

    @Override
    public void setLogger(Log log) {
        this.log = log;
    }

}
