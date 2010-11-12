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

import org.nuxeo.ecm.automation.client.jaxrs.model.PathRef;
import org.nuxeo.ecm.automation.shell.DocTypeCompletor;
import org.nuxeo.ecm.automation.shell.RemoteContext;
import org.nuxeo.ecm.shell.Argument;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.Context;
import org.nuxeo.ecm.shell.ShellException;
import org.nuxeo.ecm.shell.utils.Path;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "mkdir", help = "Create a document of the given type")
public class MkDir implements Runnable {

    @Context
    protected RemoteContext ctx;

    @Argument(name = "type", index = 0, required = true, completor = DocTypeCompletor.class, help = "The document type")
    protected String type;

    @Argument(name = "path", index = 1, required = true, help = "The document path")
    protected String path;

    public void run() {
        Path p = ctx.resolvePath(path);
        PathRef parent = new PathRef(p.getParent().toString());
        try {
            ctx.getDocumentService().createDocument(parent, type,
                    p.lastSegment());
        } catch (Exception e) {
            throw new ShellException(e);
        }
    }
}
