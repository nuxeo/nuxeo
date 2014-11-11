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
import org.nuxeo.ecm.automation.client.jaxrs.model.Documents;
import org.nuxeo.ecm.shell.Argument;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.Context;
import org.nuxeo.ecm.shell.Parameter;
import org.nuxeo.ecm.shell.ShellConsole;
import org.nuxeo.ecm.shell.ShellException;
import org.nuxeo.ecm.shell.automation.DocRefCompletor;
import org.nuxeo.ecm.shell.automation.DocumentHelper;
import org.nuxeo.ecm.shell.automation.RemoteContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "getrel", help = "Get realtions between two documents")
public class GetRelations implements Runnable {

    @Context
    protected RemoteContext ctx;

    @Parameter(name = "-out", hasValue = false, help = "Is the document the relation subject? This flag is by default on true.")
    protected boolean outgoing = true;

    @Parameter(name = "-in", hasValue = false, help = "Is the document the relation object?")
    protected boolean ingoing;

    @Parameter(name = "-predicate", hasValue = true, help = "The relation predicate - requested.")
    protected String predicate;

    @Argument(name = "doc", index = 0, required = false, completor = DocRefCompletor.class, help = "The document involved in the relation")
    protected String path;

    public void run() {
        if (predicate == null) {
            throw new ShellException("Relation predicate is required!");
        }
        if (ingoing) {
            outgoing = false;
        }
        ShellConsole console = ctx.getShell().getConsole();
        DocRef docRef = ctx.resolveRef(path);
        try {
            Documents docs = (Documents) ctx.getDocumentService().getRelations(
                    docRef, predicate, outgoing);
            for (Document doc : docs) {
                DocumentHelper.printPath(console, doc);
            }
        } catch (Exception e) {
            throw new ShellException(e);
        }
    }
}
