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
@Command(name = "query", help = "Query documents")
public class Query implements Runnable {

    @Context
    protected RemoteContext ctx;

    @Parameter(name = "-uid", hasValue = false, help = "If used the matching documents will be printed using the document UID.")
    protected boolean uid = false;

    @Argument(name = "query", index = 1, required = false, completor = DocRefCompletor.class, help = "The document path")
    protected String query;

    public void run() {
        try {
            Documents docs = ctx.getDocumentService().query(query);
            ShellConsole console = ctx.getShell().getConsole();
            for (Document doc : docs) {
                if (uid) {
                    console.println(doc.getId());
                } else {
                    DocumentHelper.printPath(console, doc);
                }
            }
        } catch (Exception e) {
            throw new ShellException(e);
        }
    }
}
