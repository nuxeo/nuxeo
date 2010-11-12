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
package org.nuxeo.ecm.automation.shell.cmds;

import org.nuxeo.ecm.automation.client.jaxrs.model.Document;
import org.nuxeo.ecm.automation.shell.RemoteContext;
import org.nuxeo.ecm.shell.CommandRegistry;
import org.nuxeo.ecm.shell.Shell;
import org.nuxeo.ecm.shell.cmds.GlobalCommands;
import org.nuxeo.ecm.shell.impl.DefaultCommandType;
import org.nuxeo.ecm.shell.utils.Path;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class RemoteCommands extends CommandRegistry {

    public final static RemoteCommands INSTANCE = new RemoteCommands();

    public RemoteCommands() {
        super(GlobalCommands.INSTANCE, "remote");
        addCommandType(DefaultCommandType.fromAnnotatedClass(Disconnect.class));
        addCommandType(DefaultCommandType.fromAnnotatedClass(Ls.class));
        addCommandType(DefaultCommandType.fromAnnotatedClass(Cd.class));
        addCommandType(DefaultCommandType.fromAnnotatedClass(Pwd.class));
        addCommandType(DefaultCommandType.fromAnnotatedClass(Popd.class));
        addCommandType(DefaultCommandType.fromAnnotatedClass(Pushd.class));
        addCommandType(DefaultCommandType.fromAnnotatedClass(MkDir.class));
        addCommandType(DefaultCommandType.fromAnnotatedClass(Rm.class));
        addCommandType(DefaultCommandType.fromAnnotatedClass(Query.class));
        addCommandType(DefaultCommandType.fromAnnotatedClass(Cat.class));
        addCommandType(DefaultCommandType.fromAnnotatedClass(Tree.class));
        addCommandType(DefaultCommandType.fromAnnotatedClass(Script.class));
        addCommandType(DefaultCommandType.fromAnnotatedClass(RunChainWithDoc.class));
        addCommandType(DefaultCommandType.fromAnnotatedClass(RunChainWithFile.class));
    }

    public String getPrompt(Shell shell) {
        RemoteContext ctx = shell.getContextObject(RemoteContext.class);
        Document doc = ctx.getDocument();
        Path path = new Path(doc.getPath());
        String p = path.isRoot() ? "/" : path.lastSegment();
        return ctx.getUserName() + "@" + ctx.getHost() + ":" + p + "> ";
    }

}
