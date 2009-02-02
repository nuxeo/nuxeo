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
import org.nuxeo.ecm.webengine.client.command.CommandException;
import org.nuxeo.ecm.webengine.client.command.CommandLine;
import org.nuxeo.ecm.webengine.client.command.CommandRegistry;
import org.nuxeo.ecm.webengine.client.command.ExitException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Console {

    protected static Console instance;
    
    protected ConsoleReader console;

    protected Client client;

    public static Console getDefault() {
        return instance;
    }
    
    public static void updatePrompt() {
        if (instance != null) {
            instance.updateDefaultPrompt();
        }        
    }
    
    public static void setPrompt(String prompt) {
        if (instance != null) {
            instance.console.setDefaultPrompt(prompt);
        }
    }

    public static CommandLine parseCommandLine(CommandRegistry reg, String line) throws CommandException {
        return new CommandLine(reg, line);
    }

    public static void runCommand(Client client, String line) throws Exception {
        parseCommandLine(client.getRegistry(), line).run(client);
    }

    public Console(Client client) throws IOException {
        if (Console.instance != null) {
            throw new IllegalAccessError("Console is already instantiated");
        }
        Console.instance = this;
        this.client = client;
        console = new ConsoleReader();
        CompletionHandler ch = console.getCompletionHandler();
        if (ch instanceof CandidateListCompletionHandler) {
            ((CandidateListCompletionHandler)ch).setAlwaysIncludeNewline(false);
        }
        console.setDefaultPrompt("|> ");
        // register completors
        console.addCompletor(new CompositeCompletor(this, client.getRegistry()));
    }

    public void updateDefaultPrompt() {
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
    
    public ConsoleReader getReader() {
        return console;
    }

    /**
     * @return the client.
     */
    public Client getClient() {
        return client;
    }

    public void run() throws IOException {
        String line = console.readLine().trim();
        while (true) {
            try {
                if (line.trim().length() > 0) {
                    if (!execute(line)) break;
                    newLine();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            line = console.readLine().trim();
        }
        console.printString("Bye");
        console.printNewline();
    }

    protected void newLine() throws IOException {
        console.flushConsole();
        console.printNewline();
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

    public CommandLine parseCommandLine(String line) throws CommandException {
        return parseCommandLine(client.getRegistry(), line);
    }

    public CommandRegistry getCommandRegistry() {
        return client.getRegistry();
    }    
    
}
