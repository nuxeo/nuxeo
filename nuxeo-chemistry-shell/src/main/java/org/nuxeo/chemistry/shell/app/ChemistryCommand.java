/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.chemistry.shell.app;

import org.nuxeo.chemistry.shell.Application;
import org.nuxeo.chemistry.shell.Console;
import org.nuxeo.chemistry.shell.command.AnnotatedCommand;
import org.nuxeo.chemistry.shell.command.CommandLine;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class ChemistryCommand extends AnnotatedCommand {

    @Override
    public void run(Application app, CommandLine cmdLine) throws Exception {
        if (app instanceof ChemistryApp) {
            execute((ChemistryApp) app, cmdLine);
        } else {
            Console.getDefault().error("Chemistry commands cannot be run outside chemistry context");
        }
    }

    protected abstract void execute(ChemistryApp app, CommandLine cmdLine) throws Exception;

}
