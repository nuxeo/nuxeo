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
package org.nuxeo.ecm.webengine.client;

import java.io.IOException;
import java.io.InputStream;

import org.nuxeo.ecm.webengine.client.command.CommandException;
import org.nuxeo.ecm.webengine.client.command.CommandLine;
import org.nuxeo.ecm.webengine.client.command.CommandRegistry;
import org.nuxeo.ecm.webengine.client.util.FileUtils;
import org.nuxeo.ecm.webengine.client.util.PwdReader;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Console {

    protected static Console instance = null;

    protected Client client;


    public static Console getDefault() {
        return instance;
    }

    public static void setDefault(Console console) {
        instance = console;
    }

    public static CommandLine parseCommandLine(CommandRegistry reg, String line) throws CommandException {
        return new CommandLine(reg, line);
    }

    public static void runCommand(Client client, String line) throws Exception {
        parseCommandLine(client.getRegistry(), line).run(client);
    }



    /**
     * Update the current context of the console.
     * On text console this will be a command line prompt.
     */
    public void updatePrompt() {
        // do nothing
    }

    /**
     * Read the stream an print the result on the screen.
     *
     * @param in
     */
    public void print(InputStream in) throws IOException {
        FileUtils.copy(in, System.out);
    }

    /**
     * Read the stream an print the result on the screen.
     * On text console put a new line after printing the result.
     * On non text console it is same as {@link #print(InputStream)}
     * @param in
     */
    public void println(InputStream in) throws IOException {
        FileUtils.copy(in, System.out);
    }

    public void print(String str) throws IOException {
        System.out.print(str);
    }

    public void println(String str) throws IOException {
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
     * @return
     */
    public Client getClient() {
        return client;
    }

    public void error(String message) throws IOException {
        System.err.println(message);
    }

    public void info(String message) throws IOException {
        System.out.println(message);
    }

    public void warn(String message) throws IOException {
        System.out.println(message);
    }

    public String promptPassword() throws IOException {
        return PwdReader.read();
    }

    /**
     *  Start the console
     * @throws IOException
     */
    public void start(Client client) throws IOException {
        if (this.client != null) {
            throw new IllegalStateException("Console already started");
        }
        this.client = client;
    }

    public CommandLine parseCommandLine(String line) throws CommandException {
        if (client != null) {
            return parseCommandLine(client.getRegistry(), line);
        }
        throw new IllegalStateException("Console not started");
    }

    public CommandRegistry getCommandRegistry() {
        if (client != null) {
            return client.getRegistry();
        }
        throw new IllegalStateException("Console not started");
    }

}
