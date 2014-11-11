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
import java.util.Collection;
import java.util.List;

import jline.ANSIBuffer;

import org.nuxeo.ecm.shell.Argument;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.CommandType;
import org.nuxeo.ecm.shell.CommandType.Token;
import org.nuxeo.ecm.shell.Context;
import org.nuxeo.ecm.shell.Shell;
import org.nuxeo.ecm.shell.ShellConsole;
import org.nuxeo.ecm.shell.ShellException;
import org.nuxeo.ecm.shell.fs.FileSystem;
import org.nuxeo.ecm.shell.utils.ANSICodes;
import org.nuxeo.ecm.shell.utils.StringUtils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "help", help = "The help command")
public class Help implements Runnable {

    @Context
    protected Shell shell;

    @Argument(name = "<command>", required = false, index = 0, help = "the name of the command to get help for")
    protected CommandType cmd;

    public void run() {
        ShellConsole console = shell.getConsole();
        if (cmd == null) {
            showMainPage(console);
        } else {
            ANSIBuffer buf = new ANSIBuffer();
            buf.bold("NAME").append(ShellConsole.CRLF).append("\t");
            buf.append(cmd.getName()).append(" -- ").append(cmd.getHelp());
            buf.append(ShellConsole.CRLF).append(ShellConsole.CRLF);
            buf.bold("SYNTAX").append(ShellConsole.CRLF).append("\t");
            buf.append(cmd.getSyntax());
            buf.append(ShellConsole.CRLF).append(ShellConsole.CRLF);

            String[] aliases = cmd.getAliases();
            if (aliases != null && aliases.length > 0) {
                if (aliases != null && aliases.length > 0) {
                    buf.bold("ALIASES").append(StringUtils.join(aliases, ", "));
                    buf.append(ShellConsole.CRLF).append(ShellConsole.CRLF);
                }
            }

            List<Token> args = cmd.getArguments();
            Collection<Token> opts = cmd.getParameters().values();

            if (!opts.isEmpty()) {
                buf.bold("OPTIONS").append(ShellConsole.CRLF);
                for (Token tok : opts) {
                    String flag = tok.isRequired ? " - " : " - [flag] - ";
                    buf.append("\t" + tok.name + flag + tok.help).append(
                            ShellConsole.CRLF);
                }
                buf.append(ShellConsole.CRLF);
            }
            if (!args.isEmpty()) {
                buf.bold("ARGUMENTS").append(ShellConsole.CRLF);
                for (Token tok : args) {
                    String flag = tok.isRequired ? " - [required] " : " - ";
                    buf.append("\t" + tok.name + flag + tok.help).append(
                            ShellConsole.CRLF);
                }
                buf.append(ShellConsole.CRLF);
            }

            InputStream in = cmd.getCommandClass().getResourceAsStream(
                    cmd.getCommandClass().getSimpleName() + ".help");
            if (in != null) {
                try {
                    String content = FileSystem.readContent(in);
                    ANSICodes.appendTemplate(buf, content);
                    buf.append(ShellConsole.CRLF);
                } catch (IOException e) {
                    throw new ShellException(e);
                } finally {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            console.println(buf.toString());
        }
    }

    public void showMainPage(ShellConsole console) {
        ANSIBuffer buf = new ANSIBuffer();
        InputStream in = getClass().getClassLoader().getResourceAsStream(
                "META-INF/help.txt");
        if (in != null) {
            try {
                String content = FileSystem.readContent(in);
                ANSICodes.appendTemplate(buf, content);
                buf.append(ShellConsole.CRLF);
            } catch (IOException e) {
                throw new ShellException(e);
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        console.println(buf.toString());

    }
}
