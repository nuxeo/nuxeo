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

import jline.ConsoleReader;
import jline.Terminal;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class SwingTerminal extends Terminal {

    protected Console console;

    public SwingTerminal(Console console) {
        this.console = console;
    }

    @Override
    public boolean isSupported() {
        return false;
    }

    @Override
    public boolean getEcho() {
        return true;
    }

    @Override
    public boolean isANSISupported() {
        return false;
    }

    public void initializeTerminal() {
        // nothing we need to do (or can do) for windows.
    }

    public boolean isEchoEnabled() {
        return true;
    }

    public void enableEcho() {
    }

    public void disableEcho() {
    }

    /**
     * Always returng 80, since we can't access this info on Windows.
     */
    public int getTerminalWidth() {
        return 80;
    }

    /**
     * Always returng 24, since we can't access this info on Windows.
     */
    public int getTerminalHeight() {
        return 80;
    }

    @Override
    public void beforeReadLine(ConsoleReader reader, String prompt,
            Character mask) {
        if (mask != null) {
            console.setMask(mask);
        }
    }

    @Override
    public void afterReadLine(ConsoleReader reader, String prompt,
            Character mask) {
        console.setMask(null);
    }
}
