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

package org.nuxeo.ecm.webengine.client;

import java.io.File;
import java.util.List;

import org.nuxeo.ecm.webengine.client.command.ExitException;
import org.nuxeo.ecm.webengine.client.console.JLineConsole;
import org.nuxeo.ecm.webengine.client.http.JdkHttpClient;
import org.nuxeo.ecm.webengine.client.util.FileUtils;
import org.nuxeo.ecm.webengine.client.util.PwdReader;



/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Main {


    public static void main(String[] args) throws Exception {
        String username = null;
        String password = null;
        String url = null;
        boolean batchMode = false;
        boolean execMode = false;
        String command = null;
        if (args.length > 0) {
            for (int i=0; i<args.length; i++) {
                String arg = args[i];
                if ("-u".equals(arg)) {
                    if (++i == args.length) { // username
                        error("Invalid option -u without value. Username requried.");
                    }
                    username = args[i];
                } else if ("-p".equals(arg)) { // password
                    if (++i == args.length) { // username
                        error("Invalid option -p without value. Password requried.");
                    }
                    password = args[i];
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
                    execMode = true;
                    command = "help";
                } else if (!arg.startsWith("-")) {
                    url = arg;
                } else {
                    // unknown option
                }
            }
            if (username != null && password == null) {
                password = PwdReader.read();
            }
            if (url != null && url.indexOf("://") == -1) {
                url = "http://"+url;
            }
        }

        Client client = new JdkHttpClient(url, null, username, password);

        
        if (execMode) {
            Console.setDefault(new Console());
            Console.getDefault().start(client);
            JLineConsole.runCommand(client, command);
            return;
        } 
        if (batchMode) {
            Console.setDefault(new Console());
            Console.getDefault().start(client);
            List<String> cmds = null;
            if (command == null) {
                cmds = FileUtils.readLines(System.in);
            } else {
                cmds = FileUtils.readLines(new File(command));
            }
            try {
                for (String cmd : cmds) {
                    System.out.println("Running: "+cmd);
                    Console.runCommand(client, cmd);
                }
                System.out.println("Done.");
            } catch (ExitException e) {
                System.out.println("Bye.");
            } catch (Throwable t) {
                t.printStackTrace();
            }
            return;
        }

        // run in interactive mode
        try {
            //TODO use user profiles to setup console  like prompt and default service to cd in
            Console.setDefault(new JLineConsole());
            Console.getDefault().start(client);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    
    static void error(String msg) {
        System.err.println(msg);
        System.exit(1);
    }
}
