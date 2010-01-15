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

package org.nuxeo.chemistry.shell.command;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.nuxeo.chemistry.shell.app.Application;
import org.nuxeo.chemistry.shell.app.Console;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class Command {

    protected CommandSyntax syntax;
    protected String[] aliases;
    protected String synopsis;

    public Command() {
        Cmd anno = getClass().getAnnotation(Cmd.class);
        synopsis = anno.synopsis();
        syntax = CommandSyntax.parse(anno.syntax());
        aliases = syntax.getCommandToken().getNames();
    }

    public String getName() {
        return aliases[0];
    }

    public String[] getAliases() {
        return aliases;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public CommandSyntax getSyntax() {
        return syntax;
    }

    public void ensureConnected(Application app) throws CommandException {
        if (!app.isConnected()) {
            throw new CommandException("Not connected");
        }
    }

    public String getHelp() {
        URL url = getClass().getResource("/help/"+getName()+".help");
        if (url == null) {
            return "N/A";
        }
        InputStream in = null;
        try {
            in = url.openStream();
            return IOUtils.toString(in);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (in != null) in.close(); } catch (IOException e) {}
        }
        return "N/A";
    }

    public void print(InputStream in) throws IOException {
        Console.getDefault().print(in);
    }

    public void println(String str) {
        Console.getDefault().println(str);
    }

    public boolean isLocal() {
        return true;
    }

    public abstract void run(Application app, CommandLine cmdLine) throws Exception;

}
