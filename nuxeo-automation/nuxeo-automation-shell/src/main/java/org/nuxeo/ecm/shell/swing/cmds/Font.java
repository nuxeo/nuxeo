/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.shell.swing.cmds;

import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.Context;
import org.nuxeo.ecm.shell.Parameter;
import org.nuxeo.ecm.shell.Shell;
import org.nuxeo.ecm.shell.ShellException;
import org.nuxeo.ecm.shell.swing.FontFamilyCompletor;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "font", help = "Print or modify the font used by the shell. This command is available only in UI mode.")
public class Font implements Runnable {

    @Context
    protected Shell shell;

    @Parameter(name = "-name", hasValue = true, completor = FontFamilyCompletor.class, help = "The font name. Default is 'Monospace'.")
    protected String name;

    @Parameter(name = "-size", hasValue = true, help = "The font size. Default is 14.")
    protected String size;

    @Parameter(name = "-weight", hasValue = true, help = "The font weight. Default is 'plain'.")
    protected String weight;

    public void run() {
        try {
            // shell.setSetting(name, value);
        } catch (Exception e) {
            throw new ShellException(e);
        }
    }
}
