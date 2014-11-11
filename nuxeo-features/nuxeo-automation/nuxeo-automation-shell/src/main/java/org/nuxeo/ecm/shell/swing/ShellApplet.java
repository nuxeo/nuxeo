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

import java.util.ArrayList;

import javax.swing.JApplet;
import javax.swing.SwingUtilities;

import org.nuxeo.ecm.shell.Shell;
import org.nuxeo.ecm.shell.cmds.Interactive;
import org.nuxeo.ecm.shell.cmds.InteractiveShellHandler;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@SuppressWarnings("serial")
public class ShellApplet extends JApplet implements InteractiveShellHandler {

    protected ConsolePanel panel;

    protected String[] getShellArgs() {
        String host = getParameter("host");
        String user = getParameter("user");
        ArrayList<String> args = new ArrayList<String>();
        if (user != null) {
            args.add("-u");
            args.add(user);
        }
        if (host != null) {
            args.add(host);
        }
        return args.toArray(new String[args.size()]);
    }

    public void init() {
        try {
            Shell.get(); // initialize the shell to get default settings
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        panel = new ConsolePanel();
                        add(panel);
                        Interactive.setConsoleReaderFactory(panel.getConsole());
                        Interactive.setHandler(ShellApplet.this);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to start applet", e);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    final Shell shell = Shell.get();
                    shell.main(getShellArgs());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void stop() {
        panel.getConsole().exit(1);
    }

    public void enterInteractiveMode() {
        Interactive.reset();
        requestFocus(); // doesn't work :/
    }

    public boolean exitInteractiveMode(int code) {
        if (code == 1) {
            // applet stop
            Interactive.reset();
            Shell.reset();
            panel.setVisible(false);
            return true;
        } else {
            // reset console
            panel.getConsole().reset();
            return false;
        }
    }
}
