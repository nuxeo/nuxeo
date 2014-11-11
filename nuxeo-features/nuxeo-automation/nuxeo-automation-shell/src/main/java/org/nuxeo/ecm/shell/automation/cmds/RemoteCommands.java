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
package org.nuxeo.ecm.shell.automation.cmds;

import java.util.Map;

import org.nuxeo.ecm.automation.client.jaxrs.model.Document;
import org.nuxeo.ecm.shell.CommandRegistry;
import org.nuxeo.ecm.shell.Shell;
import org.nuxeo.ecm.shell.ShellException;
import org.nuxeo.ecm.shell.automation.AutomationFeature;
import org.nuxeo.ecm.shell.automation.RemoteContext;
import org.nuxeo.ecm.shell.cmds.GlobalCommands;
import org.nuxeo.ecm.shell.utils.Path;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class RemoteCommands extends CommandRegistry {

    public final static RemoteCommands INSTANCE = new RemoteCommands();

    public RemoteCommands() {
        super(GlobalCommands.INSTANCE, "remote");
        onDisconnect();
    }

    public void onConnect() {
        addAnnotatedCommand(Disconnect.class);
        addAnnotatedCommand(Ls.class);
        addAnnotatedCommand(Cd.class);
        addAnnotatedCommand(Pwd.class);
        addAnnotatedCommand(Popd.class);
        addAnnotatedCommand(Pushd.class);
        addAnnotatedCommand(MkDir.class);
        addAnnotatedCommand(Update.class);
        addAnnotatedCommand(Rm.class);
        addAnnotatedCommand(Query.class);
        addAnnotatedCommand(Cat.class);
        addAnnotatedCommand(Tree.class);
        addAnnotatedCommand(Script.class);
        addAnnotatedCommand(SetBlob.class);
        addAnnotatedCommand(GetBlob.class);
        addAnnotatedCommand(GetBlobs.class);
        addAnnotatedCommand(RemoveBlob.class);
        addAnnotatedCommand(RunChainWithDoc.class);
        addAnnotatedCommand(RunChainWithFile.class);
        addAnnotatedCommand(MkRelation.class);
        addAnnotatedCommand(GetRelations.class);
        addAnnotatedCommand(SetProperty.class);
        addAnnotatedCommand(Lock.class);
        addAnnotatedCommand(Unlock.class);
        addAnnotatedCommand(Cp.class);
        addAnnotatedCommand(Mv.class);
        addAnnotatedCommand(Rename.class);
        addAnnotatedCommand(Publish.class);
        addAnnotatedCommand(Perms.class);
        addAnnotatedCommand(LifeCycleState.class);
        addAnnotatedCommand(Fire.class);
        addAnnotatedCommand(Audit.class);
    }

    public void onDisconnect() {
        clear();
        addAnnotatedCommand(Connect.class);
    }

    public String getPrompt(Shell shell) {
        RemoteContext ctx = shell.getContextObject(RemoteContext.class);
        if (ctx == null) {
            return "remote> ";
        }
        Document doc = ctx.getDocument();
        Path path = new Path(doc.getPath());
        String p = path.isRoot() ? "/" : path.lastSegment();
        return ctx.getUserName() + "@" + ctx.getHost() + ":" + p + "> ";
    }

    @Override
    public String getTitle() {
        return "Nuxeo Server Commands";
    }

    @Override
    public String getDescription() {
        return "High level commands exposed by a remote Nuxeo Server";
    }

    @Override
    public void autorun(Shell shell) {
        // check if connection info is already available and connect to remote
        // if so.
        Map<String, String> args = (Map<String, String>) shell.getMainArguments();
        if (args != null) {
            String url = args.get("#1");
            String username = args.get("-u");
            String password = args.get("-p");
            if (url != null && username != null && password != null) {
                try {
                    shell.getConsole().println("Connecting to " + url + " ...");
                    shell.getFeature(AutomationFeature.class).connect(url,
                            username, password);
                } catch (Throwable t) {
                    throw new ShellException("Failed to connect to " + url, t);
                }
            }
        }
    }

}
