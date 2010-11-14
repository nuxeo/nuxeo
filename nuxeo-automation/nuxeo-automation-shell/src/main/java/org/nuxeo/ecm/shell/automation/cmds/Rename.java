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
import org.nuxeo.ecm.shell.Argument;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.Context;
import org.nuxeo.ecm.shell.Parameter;
import org.nuxeo.ecm.shell.ShellException;
import org.nuxeo.ecm.shell.automation.DocRefCompletor;
import org.nuxeo.ecm.shell.automation.RemoteContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "rename", help = "Rename a document")
public class Rename implements Runnable {

    @Context
    protected RemoteContext ctx;

    @Parameter(name = "-name", hasValue = true, help = "A new name for the document. This parameter is required.")
    protected String name;

    @Argument(name = "doc", index = 0, required = false, completor = DocRefCompletor.class, help = "The document to rename. To use UID references prefix them with 'doc:'.")
    protected String src;

    public void run() {
        if (name == null) {
            throw new ShellException("-name parameter is required!");
        }
        DocRef srcRef = ctx.resolveRef(src);
        try {
            DocRef dstRef = ctx.getDocumentService().getParent(srcRef);
            ctx.getDocumentService().move(srcRef, dstRef, name);
        } catch (Exception e) {
            throw new ShellException("Failed to rename document " + srcRef, e);
        }

    }
}
