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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.client.console;

import java.io.IOException;

import jline.CandidateListCompletionHandler;
import jline.CompletionHandler;
import jline.ConsoleReader;

import org.nuxeo.ecm.webengine.client.Client;
import org.nuxeo.ecm.webengine.client.Console;
import org.nuxeo.ecm.webengine.client.command.CommandException;
import org.nuxeo.ecm.webengine.client.command.ExitException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class JLineConsole extends Console {


    protected ConsoleReader console;


    public JLineConsole() throws IOException {
        if (JLineConsole.instance != null) {
            throw new IllegalAccessError("Console is already instantiated");
        }
        JLineConsole.instance = this;
        console = new ConsoleReader();
        CompletionHandler ch = console.getCompletionHandler();
        if (ch instanceof CandidateListCompletionHandler) {
            ((CandidateListCompletionHandler)ch).setAlwaysIncludeNewline(false);
        }
    }

    public ConsoleReader getReader() {
        return console;
    }


    protected boolean execute(String line) throws Exception {
        try {
            runCommand(client, line);
        } catch (ExitException e) {
            return false;
        } catch (CommandException e) {
            console.printString(e.getMessage());
        }
        return true;
    }


    @Override
    public void start(Client client) throws IOException {
        super.start(client);
        console.setDefaultPrompt("|> ");
        // register completors
        console.addCompletor(new CompositeCompletor(this, client.getRegistry()));
        String line = console.readLine().trim();
        while (true) {
            try {
                if (line.trim().length() > 0) {
                    if (!execute(line)) break;
                    println();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            line = console.readLine().trim();
        }
        console.printString("Bye");
        console.printNewline();
    }


    @Override
    public String promptPassword() throws IOException {
        return console.readLine(new Character('*'));
    }

    @Override
    public void updatePrompt() {
        if (client.isConnected()) {
            String path = client.getWorkingDirectory().lastSegment();
            if (path == null) {
                path = "/";
            }
            console.setDefaultPrompt("|"+client.getHost()+":"+path+"> ");
        } else {
            console.setDefaultPrompt("|> ");
        }
    }


    @Override
    public void println() throws IOException {
        console.flushConsole();
        console.printNewline();
    }

    @Override
    public void flush() throws IOException {
        console.flushConsole();
    }

}
