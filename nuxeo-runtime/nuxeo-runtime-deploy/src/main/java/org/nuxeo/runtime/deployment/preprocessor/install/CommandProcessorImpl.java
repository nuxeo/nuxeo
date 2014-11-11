/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.deployment.preprocessor.install;

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
                if (log != null && log.isInfoEnabled()) {
                    log.debug("Executing: " + cmd.toString(ctx));
                }
                cmd.exec(ctx);
            }
        } catch (Exception e) {
            log.error(e, e);
        }
    }

    @Override
    public void setLogger(Log log) {
        this.log = log;
    }

}
