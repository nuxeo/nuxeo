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
@Command(name = "cp", help = "Copy a document")
public class Cp implements Runnable {

    @Context
    protected RemoteContext ctx;

    @Parameter(name = "-name", hasValue = true, help = "A new name for the copy. I not specified preserve the source name")
    protected String name;

    @Argument(name = "src", index = 0, required = true, completor = DocRefCompletor.class, help = "The document to copy. To use UID references prefix them with 'doc:'.")
    protected String src;

    @Argument(name = "dst", index = 1, required = true, completor = DocRefCompletor.class, help = "The target parent. To use UID references prefix them with 'doc:'.")
    protected String dst;

    public void run() {
        DocRef srcRef = ctx.resolveRef(src);
        DocRef dstRef = ctx.resolveRef(dst);
        try {
            ctx.getDocumentService().copy(srcRef, dstRef, name);
        } catch (Exception e) {
            throw new ShellException("Failed to copy document " + srcRef
                    + " to " + dstRef, e);
        }

    }
}
