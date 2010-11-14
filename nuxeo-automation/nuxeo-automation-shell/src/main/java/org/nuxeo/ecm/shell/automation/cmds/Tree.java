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

import org.nuxeo.ecm.automation.client.jaxrs.model.Document;
import org.nuxeo.ecm.shell.Argument;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.Context;
import org.nuxeo.ecm.shell.ShellConsole;
import org.nuxeo.ecm.shell.ShellException;
import org.nuxeo.ecm.shell.automation.DocRefCompletor;
import org.nuxeo.ecm.shell.automation.DocumentHelper;
import org.nuxeo.ecm.shell.automation.RemoteContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "tree", help = "List a subtree")
public class Tree implements Runnable {

    @Context
    protected RemoteContext ctx;

    @Argument(name = "doc", index = 0, required = false, completor = DocRefCompletor.class, help = "A document to list its subtree. If not specified list the current document subtree. To use UID references prefix them with 'doc:'.")
    protected String path;

    public void run() {
        ShellConsole console = ctx.getShell().getConsole();
        Document root = ctx.resolveDocument(path);
        printTree(console, root, "");
    }

    protected void printTree(ShellConsole console, Document root, String prefix) {
        try {
            DocumentHelper.printName(console, root,
                    prefix.length() == 0 ? prefix : prefix + "+- ");
            prefix += "|  ";
            for (Document doc : ctx.getDocumentService().getChildren(root)) {
                printTree(console, doc, prefix);
            }
        } catch (Exception e) {
            throw new ShellException("Failed to list document " + path, e);
        }
    }
}
