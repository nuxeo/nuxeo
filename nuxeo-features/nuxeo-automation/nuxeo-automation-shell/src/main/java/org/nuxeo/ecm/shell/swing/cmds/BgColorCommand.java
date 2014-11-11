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

import java.awt.Color;

import javax.swing.JColorChooser;
import javax.swing.colorchooser.DefaultColorSelectionModel;

import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.Context;
import org.nuxeo.ecm.shell.Shell;
import org.nuxeo.ecm.shell.ShellException;
import org.nuxeo.ecm.shell.swing.Console;
import org.nuxeo.ecm.shell.swing.Theme;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "background", help = "Modify the background color used by the shell. This command is available only in UI mode.")
public class BgColorCommand implements Runnable {

    @Context
    protected Shell shell;

    @Context
    protected Console console;

    public void run() {
        try {
            DefaultColorSelectionModel model = new DefaultColorSelectionModel(
                    console.getBackground());
            JColorChooser cc = new JColorChooser();
            cc.setSelectionModel(model);
            Color color = JColorChooser.showDialog(console,
                    "Select the background color", console.getBackground());
            if (color != null) {
                Theme theme = console.getTheme();
                theme.setName("Custom");
                theme.setBgColor(color);
                shell.setSetting("theme.Custom", theme.toString());
                console.setTheme(theme);
            }
        } catch (Exception e) {
            throw new ShellException(e);
        }
    }

}
