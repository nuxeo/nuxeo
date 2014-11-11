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
package org.nuxeo.ecm.shell.cmds;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;

import jline.ANSIBuffer;
import jline.ConsoleReader;
import jline.Terminal;

import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.Context;
import org.nuxeo.ecm.shell.Shell;
import org.nuxeo.ecm.shell.ShellConsole;
import org.nuxeo.ecm.shell.ShellException;
import org.nuxeo.ecm.shell.cmds.completors.ShellCompletor;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "interactive", help = "Interactive shell")
public class Interactive implements Runnable, ShellConsole {

    protected static ConsoleReaderFactory factory;

    public static void setConsoleReaderFactory(ConsoleReaderFactory factory) {
        Interactive.factory = factory;
    }

    protected static String currentCmdLine;

    @Context
    protected Shell shell;

    protected ConsoleReader console;

    private static boolean isRunning = false;

    private static InteractiveShellHandler handler;

    public static void setHandler(InteractiveShellHandler exitHandler) {
        Interactive.handler = exitHandler;
    }

    public static void reset() {
        isRunning = false;
    }

    public Interactive() throws IOException {
        console = factory != null ? factory.getConsoleReader()
                : new ConsoleReader();
    }

    /**
     * Used in GUI mode
     * 
     * @param shell
     * @param in
     * @param out
     * @throws IOException
     */
    public Interactive(Shell shell, InputStream in, Writer out, Terminal term)
            throws IOException {
        this.shell = shell;
        console = new ConsoleReader(in, out, null, term);
    }

    public static String getCurrentCmdLine() {
        return currentCmdLine;
    }

    public ConsoleReader getConsole() {
        return console;
    }

    public Shell getShell() {
        return shell;
    }

    public void run() {
        if (handler != null) {
            handler.enterInteractiveMode();
        }
        if (isRunning) { // avoid entering twice this command
            return;
        }
        isRunning = true;
        console.addCompletor(new ShellCompletor(this));
        shell.setConsole(this);
        console.setDefaultPrompt(getPrompt());
        try {
            loadHistory();
            shell.hello();
            try {
                shell.getActiveRegistry().autorun(shell);
            } catch (Throwable t) {
                handleError(t);
            }
            while (true) {
                try {
                    String cmdline = console.readLine(getPrompt());
                    currentCmdLine = cmdline;
                    shell.run(cmdline);
                } catch (Throwable t) {
                    int r = handleError(t);
                    if (r != 0) {
                        isRunning = false;
                        if (handler != null) {
                            if (handler.exitInteractiveMode(r)) {
                                return;
                            }
                        } else { // default exit mechanism
                            shell.bye();
                            System.exit(r < 0 ? 0 : r);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            closeHistory();
        }
    }

    /**
     * Return non zero to stop the application. If a negative code is returned
     * the application will stop normally otherwise it will stop using
     * System.exit with the exit code as argument.
     * 
     * @param t
     * @return
     * @throws IOException
     */
    protected int handleError(Throwable t) throws IOException {
        if (t instanceof ShellException) {
            ShellException e = (ShellException) t;
            int r = e.getErrorCode();
            if (r != 0) {
                return r;
            } else {
                shell.setProperty("last.error", e);
                console.printString(e.getMessage());
                console.printNewline();
                // console.printString(sw.toString());
            }
        } else {
            ANSIBuffer buf = shell.newANSIBuffer();
            buf.red(Trace.getStackTrace(t));
            console.printString(buf.toString());
        }
        return 0;
    }

    public String getPrompt() {
        return shell.getActiveRegistry().getPrompt(shell);
    }

    public void print(String msg) {
        try {
            console.printString(msg);
        } catch (IOException e) {
            throw new ShellException(e).setErrorCode(1);
        }
    }

    public void println(String msg) {
        try {
            console.printString(msg);
            console.printNewline();
        } catch (IOException e) {
            throw new ShellException(e).setErrorCode(1);
        }
    }

    public void println() {
        try {
            console.printNewline();
        } catch (IOException e) {
            throw new ShellException(e).setErrorCode(1);
        }
    }

    public String readLine(String prompt, Character mask) {
        try {
            return console.readLine(prompt, mask);
        } catch (IOException e) {
            throw new ShellException(e).setErrorCode(1);
        }
    }

    public void loadHistory() throws IOException {
        if (!shell.getBooleanSetting("history", true)) {
            return;
        }
        console.getHistory().setHistoryFile(shell.getHistoryFile());
        console.getHistory().moveToEnd();
    }

    public void closeHistory() {
        PrintWriter pw = console.getHistory().getOutput();
        if (pw != null) {
            pw.close();
        }
    }

    public void removeHistory() {
        closeHistory();
        shell.getHistoryFile().delete();
    }

}
