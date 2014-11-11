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
package org.nuxeo.ecm.shell.swing;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.nuxeo.ecm.shell.Shell;
import org.nuxeo.ecm.shell.cmds.Interactive;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@SuppressWarnings("serial")
public class ShellFrame extends JFrame {

    protected Console console;

    public ShellFrame() throws Exception {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("Nuxeo Shell");
        JPanel content = (JPanel) getContentPane();
        ConsolePanel panel = new ConsolePanel();
        console = panel.getConsole();
        // Set the window's bounds, centering the window
        content.add(panel, BorderLayout.CENTER);
        // content.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        setResizable(true);
    }

    public static void main(String[] args) throws Exception {
        Shell shell = Shell.get();
        ShellFrame term = new ShellFrame();
        term.pack();
        term.setSize(800, 600);
        // term.setExtendedState(Frame.MAXIMIZED_BOTH);
        term.setLocationRelativeTo(null);
        term.setVisible(true);
        term.console.requestFocus();
        Interactive.setConsoleReaderFactory(term.console);
        shell.main(args);
    }

}
