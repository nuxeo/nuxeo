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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import jline.ANSIBuffer;

import org.nuxeo.ecm.shell.Argument;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.CommandRegistry;
import org.nuxeo.ecm.shell.CommandType;
import org.nuxeo.ecm.shell.CommandType.Token;
import org.nuxeo.ecm.shell.Context;
import org.nuxeo.ecm.shell.Parameter;
import org.nuxeo.ecm.shell.Shell;
import org.nuxeo.ecm.shell.ShellConsole;
import org.nuxeo.ecm.shell.ShellException;
import org.nuxeo.ecm.shell.cmds.completors.CommandRegistryCompletor;
import org.nuxeo.ecm.shell.fs.FileCompletor;
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

    @Argument(name = "command", required = false, index = 0, help = "the name of the command to get help for")
    protected CommandType cmd;

    @Parameter(name = "-export", hasValue = true, completor = FileCompletor.class, help = "If used export all the commands available in a wiki formatto the given file. If adirectory is given the export will be made in file help.wiki in that directory.")
    protected File export;

    @Parameter(name = "-ns", hasValue = true, completor = CommandRegistryCompletor.class, help = "[optional] - to be used to filter commands by namespaces when generating the documentation. By default all namespaces are dumped.")
    protected CommandRegistry ns;

    public void run() {
        ShellConsole console = shell.getConsole();
        if (export != null) {
            if (export.isDirectory()) {
                export = new File(export, "help.wiki");
            }
            try {
                if (ns == null) {
                    exportCommands(shell, export);
                } else {
                    exportRegistry(shell, ns, export);
                }
                console.println("Commands wiki exported in " + export);
            } catch (Throwable t) {
                throw new ShellException("Failed to export commands wiki", t);
            }
            return;
        }
        if (cmd == null) {
            showMainPage(console);
        } else {
            console.println(getCommandHelp(cmd, false).toString());
        }
    }

    public void showMainPage(ShellConsole console) {
        ANSIBuffer buf = new ANSIBuffer();
        InputStream in = getClass().getClassLoader().getResourceAsStream(
                "META-INF/help.txt");
        if (in != null) {
            try {
                String content = FileSystem.readContent(in);
                String versionVar = "${version}";
                int i = content.indexOf(versionVar);
                if (i > -1) {
                    content = content.substring(0, i)
                            + Shell.class.getPackage().getImplementationVersion()
                            + content.substring(i + versionVar.length());
                }
                ANSICodes.appendTemplate(buf, content, false);
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

    public void exportRegistry(Shell shell, CommandRegistry reg, File file)
            throws Exception {
        StringBuilder sb = new StringBuilder();
        writeRegistry(reg, sb);
        PrintWriter w = new PrintWriter(new FileWriter(file));
        try {
            w.println(sb.toString());
        } finally {
            w.close();
        }
    }

    public void exportCommands(Shell shell, File file) throws Exception {
        HashMap<String, StringBuilder> wikis = new HashMap<String, StringBuilder>();
        for (String name : shell.getRegistryNames()) {
            StringBuilder sb = new StringBuilder();
            writeRegistry(shell.getRegistry(name), sb);
            wikis.put(name, sb);
        }
        // sort registries: first global, then local, then remote, exclude
        // automation?
        PrintWriter w = new PrintWriter(new FileWriter(file));
        try {
            for (StringBuilder sb : wikis.values()) {
                w.println(sb.toString());
            }
        } finally {
            w.close();
        }
    }

    protected void writeRegistry(CommandRegistry reg, StringBuilder sb) {
        sb.append("{info:title=Namespce: *" + reg.getName() + "*}"
                + reg.getDescription());
        sb.append("{info}\nh1. Index\n{toc:minLevel=2|maxLevel=2}\n\n");
        HashSet<String> aliases = new HashSet<String>();
        for (CommandType cmd : reg.getLocalCommandTypes()) {
            if (!aliases.contains(cmd.getName())) {
                writeCommand(cmd, sb);
                aliases.add(cmd.getName());
            }
        }
    }

    protected void writeCommand(CommandType cmd, StringBuilder sb) {
        ANSIBuffer buf = getCommandHelp(cmd, true);
        sb.append("h2. ").append(cmd.getName()).append("\n").append(
                buf.toString(false));
    }

    protected ANSIBuffer getCommandHelp(CommandType cmd, boolean wiki) {
        ANSIBuffer buf = new ANSIBuffer();
        header(buf, "NAME", wiki).append(ShellConsole.CRLF).append("\t");
        buf.append(cmd.getName()).append(" -- ").append(cmd.getHelp());
        buf.append(ShellConsole.CRLF).append(ShellConsole.CRLF);
        header(buf, "SYNTAX", wiki).append(ShellConsole.CRLF).append("\t");
        syntax(buf, cmd.getSyntax(), wiki);
        buf.append(ShellConsole.CRLF).append(ShellConsole.CRLF);

        String[] aliases = cmd.getAliases();
        if (aliases != null && aliases.length > 0) {
            if (aliases != null && aliases.length > 0) {
                header(buf, "ALIASES", wiki).append(ShellConsole.CRLF).append(
                        "\t").append(StringUtils.join(aliases, ", "));
                buf.append(ShellConsole.CRLF).append(ShellConsole.CRLF);
            }
        }

        List<Token> args = cmd.getArguments();
        Collection<Token> opts = cmd.getParameters().values();

        if (!opts.isEmpty()) {
            header(buf, "OPTIONS", wiki).append(ShellConsole.CRLF);
            for (Token tok : opts) {
                if (wiki) {
                    buf.append("*");
                }
                String flag = tok.isRequired ? " - " : " - [flag] - ";
                buf.append("\t" + tok.name + flag + tok.help).append(
                        ShellConsole.CRLF);
            }
            buf.append(ShellConsole.CRLF);
        }
        if (!args.isEmpty()) {
            header(buf, "ARGUMENTS", wiki).append(ShellConsole.CRLF);
            for (Token tok : args) {
                if (wiki) {
                    buf.append("*");
                }
                String flag = tok.isRequired ? " - [required] - "
                        : " - [optional] -";
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
                ANSICodes.appendTemplate(buf, content, wiki);
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
        return buf;
    }

    protected ANSIBuffer header(ANSIBuffer buf, String text, boolean wiki) {
        if (wiki) {
            buf.append("*").append(text).append("*");
        } else {
            buf.bold(text);
        }
        return buf;
    }

    protected ANSIBuffer boldify(ANSIBuffer buf, String text, boolean wiki) {
        if (wiki) {
            buf.append("*").append(text).append("*");
        } else {
            buf.bold(text);
        }
        return buf;
    }

    protected ANSIBuffer syntax(ANSIBuffer buf, String syntax, boolean wiki) {
        if (wiki) {
            buf.append("{code}").append(syntax).append("{code}");
        } else {
            buf.append(syntax);
        }
        return buf;
    }

    protected void formatCommandForWiki() {

    }

}
