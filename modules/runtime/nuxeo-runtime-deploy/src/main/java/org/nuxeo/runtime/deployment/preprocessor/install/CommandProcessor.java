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

import java.util.List;

import org.apache.commons.logging.Log;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface CommandProcessor {

    /**
     * Gets the list of commands to execute when install() is called.
     * <p>
     * The returned list is editable so that you can add new commands or modify existing commands by modifying the
     * returned list.
     *
     * @return the list of commands to execute by this installer
     */
    List<Command> getCommands();

    /**
     * Execute commands.
     */
    void exec(CommandContext ctx);

    void setLogger(Log log);

}
