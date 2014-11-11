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

package org.nuxeo.ecm.webengine.client.command;

import org.nuxeo.ecm.client.Path;
import org.nuxeo.ecm.webengine.client.Client;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class RemoteCommand extends Command {

    protected Path path;

    public static RemoteCommand parse(String line) throws CommandException {
        // GET \t PATH \t syntax \t synopsis
        int p = line.indexOf('\t');
        int q = 0;
        if (p == -1) {
            throw new CommandException("Invalid command format: "+line);
        }
        String method = line.substring(q, p);
        q = p+1;
        p = line.indexOf(q, '\t');
        if (p == -1) {
            throw new CommandException("Invalid command format: "+line);
        }
        String path = line.substring(q, p);
        q = p+1;
        p = line.indexOf(q, '\t');
        if (p == -1) {
            throw new CommandException("Invalid command format: "+line);
        }
        String syntax = line.substring(q, p);
        q = p+1;
        p = line.indexOf(q, '\t');
        String synopsis = "N/A";
        if (p > -1) {
            synopsis = line.substring(q, p);
        }
        if ("GET".equals(method)) {
          return new RemoteGetCommand(path, syntax, synopsis);
        } else if ("POST".equals(method)) {
            return new RemotePostCommand(path, syntax, synopsis);
        } else if ("PUT".equals(method)) {
            return new RemotePutCommand(path, syntax, synopsis);
        } else if ("DELETE".equals(method)) {
            return new RemoteDeleteCommand(path, syntax, synopsis);
        } else if ("HEAD".equals(method)) {
            return new RemoteHeadCommand(path, syntax, synopsis);
        } else { // unuspported method using GET
            return new RemoteGetCommand(path, syntax, synopsis);
        }
    }

    public RemoteCommand(String path, String syntax, String synopsis) {
        if (path != null && path.length() > 0) {
            this.path = new Path(path);
        } else {
            this.path = Path.EMPTY;
        }
        this.synopsis = synopsis;
        this.syntax = CommandSyntax.parse(syntax);
        this.aliases = this.syntax.getCommandToken().getNames();
    }

    @Override
    public String getHelp(Client client) {
        try {
            return client.getHelp(getName());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
