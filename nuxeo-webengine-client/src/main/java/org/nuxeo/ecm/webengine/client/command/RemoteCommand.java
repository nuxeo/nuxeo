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

import java.net.URL;

import org.nuxeo.ecm.webengine.client.Client;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RemoteCommand extends Command {

    public static RemoteCommand parse(String line) {
        int p = line.indexOf('\t');
        if (p > -1) {
            return new RemoteCommand(line.substring(0, p), line.substring(p+1));
        }
        return new RemoteCommand(line, "");
    }
    
    public RemoteCommand(String syntax, String synopsis) {        
        this.synopsis = synopsis;        
        this.syntax = CommandSyntax.parse(syntax);
        this.aliases = this.syntax.getCommandToken().getNames();
    }

    @Override
    protected URL getHelpUrl(Client client) {
        try {
            return client.getCommandUrl("/"+getName());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    @Override
    public void run(Client client, CommandLine cmdLine) throws Exception {
        client.execute(this, cmdLine);
    }

}
