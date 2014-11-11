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

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.nuxeo.ecm.shell.swing.widgets.HistoryFinder;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@SuppressWarnings("serial")
public class ConsolePanel extends JPanel {

    protected Console console;

    protected HistoryFinder finder;

    public ConsolePanel() throws Exception {
        setLayout(new BorderLayout());
        console = new Console();
        finder = new HistoryFinder(console);
        finder.setVisible(false);
        console.setFinder(finder);
        add(new JScrollPane(console), BorderLayout.CENTER);
        add(finder, BorderLayout.SOUTH);
    }

    public Console getConsole() {
        return console;
    }

    public HistoryFinder getFinder() {
        return finder;
    }

}
