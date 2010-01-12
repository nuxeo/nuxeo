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
package org.nuxeo.chemistry.shell;

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


    public static Console getDefault() {
        return instance;
    }

    public static void setDefault(Console console) {
        instance = console;
    }

    public static void runCommand(Application app, String line) throws Exception {
        parseCommandLine(app.getCommandRegistry(), line).run(app);
    }

    public static CommandLine parseCommandLine(CommandRegistry reg, String line) throws CommandException {
        return new CommandLine(reg, line);
    }

    /**
     * Update the current context of the console.
     * On text console this will be a command line prompt.
     */
    public void updatePrompt() {
        // do nothing
    }

    /**
     * Reads the stream an print the result on the screen.
     */
    public void print(InputStream in) throws IOException {
        FileUtils.copy(in, System.out);
    }

    /**
     * Reads the stream an print the result on the screen.
     * <p>
     * On text console put a new line after printing the result.
     * On non text console it is same as {@link #print(InputStream)}
     */
    public void println(InputStream in) throws IOException {
        FileUtils.copy(in, System.out);
    }

    public void print(String str) {
        System.out.print(str);
    }

    public void println(String str) {
        System.out.println(str);
    }

    /**
     * Print a new line.
     * On non text console does nothing
     */
    public void println() throws IOException {
        System.out.println();
    }

    /**
     * Flush pending printing if any.
     */
    public void flush() throws IOException {
        System.out.flush();
    }

    /**
     * Get the current client
     */
    public Application getApplication() {
        return app;
    }

    public void error(String message) {
        System.err.println(message);
    }

    public void info(String message) {
        System.out.println(message);
    }

    public void warn(String message) {
        System.out.println(message);
    }

    public String promptPassword() throws IOException {
        return PwdReader.read();
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

    // Not used
    public CommandLine parseCommandLine(String line) throws CommandException {
        if (app != null) {
            return parseCommandLine(app.getCommandRegistry(), line);
        }
        throw new IllegalStateException("Console not started");
    }

    // Not used
    public CommandRegistry getCommandRegistry() {
        if (app != null) {
            return app.getCommandRegistry();
        }
        throw new IllegalStateException("Console not started");
    }

}
