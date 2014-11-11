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
import java.net.URL;

import jline.CandidateListCompletionHandler;
import jline.CompletionHandler;
import jline.ConsoleReader;

import org.nuxeo.ecm.webengine.client.command.CommandException;
import org.nuxeo.ecm.webengine.client.command.ExitException;
import org.nuxeo.ecm.webengine.client.http.HttpClient;

import com.sun.corba.se.spi.orbutil.fsm.Guard.Complement;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Console {

    protected ConsoleReader console;

    protected HttpClient client;

    public Console() throws IOException {
        console = new ConsoleReader();
        initialize("http://localhost:8080");
    }

    public void initialize(String host, int port, String path, String username, String password) throws IOException {
        initialize(new URL("http", host, port, path));
        //TODO username and password
    }

    public void initialize(String url) throws IOException {
        initialize(new URL(url));
    }

    public void initialize(URL url) throws IOException {
        CompletionHandler ch = console.getCompletionHandler();
        if (ch instanceof CandidateListCompletionHandler) {
            ((CandidateListCompletionHandler)ch).setAlwaysIncludeNewline(false);
        }
        console.setDefaultPrompt("|> ");
        client = new HttpClient(url);
        // register completors
        console.addCompletor(new CompositeCompletor(this, client.getRegistry()));
    }

    public ConsoleReader getReader() {
        return console;
    }

    /**
     * @return the client.
     */
    public HttpClient getClient() {
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
            client.run(line);
        } catch (ExitException e) {
            return false;
        } catch (CommandException e) {
            console.printString(e.getMessage());
        }
        return true;
    }


}
