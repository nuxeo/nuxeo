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

package org.nuxeo.chemistry.shell;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.nuxeo.chemistry.shell.app.ChemistryApp;
import org.nuxeo.chemistry.shell.app.Console;
import org.nuxeo.chemistry.shell.command.ExitException;
import org.nuxeo.chemistry.shell.jline.JLineConsole;
import org.nuxeo.chemistry.shell.util.FileUtils;
import org.nuxeo.chemistry.shell.util.PwdReader;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Main {

    String username;
    String password;
    String url;
    boolean batchMode;
    boolean execMode;
    boolean testMode;
    String command;
    private ChemistryApp app;

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        main.parseArgs(args);
        main.run();
    }

    public void parseArgs(String[] args) throws IOException {
        if (args.length > 0) {
            for (int i=0; i<args.length; i++) {
                String arg = args[i];
                if ("-u".equals(arg)) {
                    if (++i == args.length) { // username
                        error("Invalid option -u without value. Username required.");
                    }
                    username = args[i];
                } else if ("-p".equals(arg)) { // password
                    if (++i == args.length) { // username
                        error("Invalid option -p without value. Password required.");
                    }
                    password = args[i];
                } else if ("-t".equals(arg)) { // test mode
                    testMode = true;
                } else if ("-e".equals(arg)) { // execute mode
                    // execute one command
                    execMode = true;
                    StringBuilder buf = new StringBuilder();
                    for (i++; i<args.length; i++) {
                        buf.append(args[i]).append(" ");
                    }
                    command = buf.toString();
                    break;
                } else if ("-b".equals(arg)) { // batch mode
                    // execute commands in the given file or if no specified read from stdin
                    batchMode = true;
                    if (++i < args.length) {
                        // read commands from a file
                        command = args[i];
                    }
                    break;
                } else if ("-h".equals(arg)) { // help
                    // execute help command
                    usage();
                    System.exit(0);
                } else if (!arg.startsWith("-")) {
                    url = arg;
                } else {
                    // unknown option
                }
            }
            if (username != null && password == null) {
                password = PwdReader.read();
            }
            if (url != null && !url.contains("://")) {
                url = "http://"+url;
            }
        }
    }

    public void run() throws Exception {
        app = new ChemistryApp();
        if (username != null){
            app.login(username, password == null ? new char[0] : password.toCharArray());
        }
        if (url != null) {
            app.connect(url);
        }

        if (execMode) {
            runInExecMode();
        } else if (batchMode) {
            runInBatchMode();
        } else {
            runInInteractiveMode();
        }
    }

    private void runInExecMode() throws Exception {
        Console.setDefault(new Console());
        Console.getDefault().start(app);
        Console.getDefault().runCommand(command);
    }

    private void runInBatchMode() throws IOException {
        Console.setDefault(new Console());
        Console.getDefault().start(app);
        List<String> cmds;
        if (command == null) {
            cmds = FileUtils.readLines(System.in);
        } else {
            cmds = FileUtils.readLines(new File(command));
        }
        for (String cmd : cmds) {
            // Ignore empty lines / comments
            if (cmd.length() == 0 || cmd.startsWith("#")) {
                continue;
            }
            Console.getDefault().println("Running: " + cmd);
            try {
                Console.getDefault().runCommand(cmd);
            } catch (ExitException e) {
                Console.getDefault().println("Bye.");
                return;
            } catch (Exception e) {
                Console.getDefault().error(e.getMessage());
                if (testMode) {
                    e.printStackTrace();
                    Console.getDefault().println("Exiting on error.");
                    System.exit(1);
                    return;
                }
            }
        }
        Console.getDefault().println("Done.");
    }

    private void runInInteractiveMode() {
        try {
            //TODO use user profiles to setup console like prompt and default service to cd in
            Console.setDefault(new JLineConsole());
            Console.getDefault().println(
                    "CMIS Shell by Nuxeo (www.nuxeo.com). Type 'help' for help.");
            Console.getDefault().start(app);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void error(String msg) {
        System.err.println(msg);
        System.exit(1);
    }

    static void usage() throws IOException {
        URL url = Main.class.getResource("/help/usage.help");
        String help = FileUtils.read(url.openStream());
        System.out.print(help);
    }

}
