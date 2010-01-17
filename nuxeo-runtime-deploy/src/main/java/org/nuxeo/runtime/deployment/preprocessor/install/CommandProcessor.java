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

import java.util.List;

import org.apache.commons.logging.Log;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface CommandProcessor {

    /**
     * Gets the list of commands to execute when install() is called.
     * <p>
     * The returned list is editable so that you can add new commands or modify existing commands
     * by modifying the returned list.
     *
     * @return the list of commands to execute by this installer
     */
    List<Command> getCommands();

    /**
     * Execute commands.
     *
     * @throws Exception
     */
    void exec(CommandContext ctx) throws Exception;

    void setLogger(Log log);

}
