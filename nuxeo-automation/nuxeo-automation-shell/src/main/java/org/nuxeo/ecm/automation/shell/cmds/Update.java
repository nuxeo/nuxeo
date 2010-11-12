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

import org.nuxeo.ecm.automation.shell.RemoteContext;
import org.nuxeo.ecm.shell.Argument;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.Context;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "update", help = "Update document properties")
public class Update implements Runnable {

    @Context
    protected RemoteContext ctx;

    @Argument(name = "properties", index = 0, help = "The propertis to update. Default separator is \n.")
    protected String props;

    @Argument(name = "path", index = 1, required = true, help = "The document path")
    protected String path;

    public void run() {
        // DocRef doc = ctx.resolveRef(path);
        // PathRef parent = new PathRef(p.getParent().toString());
        // try {
        // PropertyMap map = new PropertyMap();
        //
        // ctx.getDocumentService().update(doc, type, p.lastSegment());
        // } catch (Exception e) {
        // throw new ShellException(e);
        // }
    }
}
