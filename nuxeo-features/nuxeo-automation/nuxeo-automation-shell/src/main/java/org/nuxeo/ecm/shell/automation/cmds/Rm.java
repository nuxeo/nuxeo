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

import org.nuxeo.ecm.automation.client.jaxrs.model.DocRef;
import org.nuxeo.ecm.automation.client.jaxrs.model.Document;
import org.nuxeo.ecm.shell.Argument;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.Context;
import org.nuxeo.ecm.shell.ShellException;
import org.nuxeo.ecm.shell.automation.DocRefCompletor;
import org.nuxeo.ecm.shell.automation.RemoteContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "rm", help = "Remove a document")
public class Rm implements Runnable {

    @Context
    protected RemoteContext ctx;

    @Argument(name = "path", index = 1, required = false, completor = DocRefCompletor.class, help = "The document path. To use UID references prefix them with 'doc:'.")
    protected String path;

    public void run() {
        DocRef ref = ctx.resolveRef(path);
        try {
            Document parent = null;
            if (path == null) {
                parent = ctx.getDocumentService().getParent(ref);
            }
            ctx.getDocumentService().remove(ref);
            if (path == null) {
                ctx.setDocument(parent);
            }
        } catch (Exception e) {
            throw new ShellException(e);
        }
    }
}
