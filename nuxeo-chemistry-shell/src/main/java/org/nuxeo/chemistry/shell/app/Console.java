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

import java.io.IOException;
import java.io.InputStream;

import org.nuxeo.chemistry.shell.command.CommandException;
import org.nuxeo.chemistry.shell.command.CommandLine;
import org.nuxeo.chemistry.shell.command.CommandRegistry;
import org.nuxeo.chemistry.shell.util.FileUtils;
import org.nuxeo.chemistry.shell.util.PwdReader;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Console {

    protected static Console instance;

    protected Application app;

    protected StringBuffer buffer = new StringBuffer();
    protected String lastResult;

    public static Console getDefault() {
        return instance;
    }

    public static void setDefault(Console console) {
        instance = console;
    }

    /**
     * Starts the console.
     */
    public void start(Application app) throws IOException {
        if (this.app != null) {
            throw new IllegalStateException("Console already started");
        }
        this.app = app;
    }

    /**
     * Gets the current client
     */
    public Application getApplication() {
        return app;
    }

    /**
     * Gets the result of the last command.
     */
    public String getLastResult() {
        return lastResult;
    }

    public void runCommand(String line) throws Exception {
        CommandLine commandLine = parseCommandLine(app.getCommandRegistry(), line);
        lastResult = buffer.toString();
        buffer = new StringBuffer();
        commandLine.run(app);
        if ("match".equals(commandLine.getCommand().getName())) {
            // Keep previous result in case we have several 'match' commands
            buffer = new StringBuffer(lastResult);
        }
    }

    public static CommandLine parseCommandLine(CommandRegistry reg, String line) throws CommandException {
        return new CommandLine(reg, line);
    }

    /**
     * Update the current context of the console.
     * Overridden in the JLine console.
     */
    public void updatePrompt() {
        // do nothing
    }

    /**
     * Reads the stream an prints the result on the screen.
     */
    public void print(InputStream in) throws IOException {
        FileUtils.copy(in, System.out);
        System.out.flush();
    }

    public void println(String str) {
        buffer.append(str + "\n");
        System.out.println(str);
    }

    /**
     * Print a new line.
     * On non text console does nothing
     */
    public void println() throws IOException {
        System.out.println();
    }

    public void error(String message) {
        System.err.println(message);
    }

    public String promptPassword() throws IOException {
        return PwdReader.read();
    }

}
