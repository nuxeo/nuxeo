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
@Command(name = "mkrel", help = "Create a relation between two documents")
public class MkRelation implements Runnable {

    @Context
    protected RemoteContext ctx;

    @Parameter(name = "-predicate", hasValue = true, help = "The relation predicate - requested.")
    protected String predicate;

    @Argument(name = "subject", index = 0, completor = DocRefCompletor.class, required = true, help = "The subject of the relation")
    protected String subject;

    @Argument(name = "object", index = 1, required = true, completor = DocRefCompletor.class, help = "The object of the relation")
    protected String object;

    public void run() {
        if (predicate == null) {
            throw new ShellException("Relation predicate is required!");
        }
        DocRef s = ctx.resolveRef(subject);
        DocRef o = ctx.resolveRef(object);
        try {
            ctx.getDocumentService().createRelation(s, predicate, o);
        } catch (Exception e) {
            throw new ShellException(e);
        }
    }
}
