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
package org.nuxeo.ecm.shell.automation;

import java.util.Collections;
import java.util.List;

import jline.Completor;

import org.nuxeo.ecm.automation.client.jaxrs.model.Document;
import org.nuxeo.ecm.automation.client.jaxrs.model.OperationDocumentation;
import org.nuxeo.ecm.shell.Shell;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class ChainCompletor implements Completor {

    protected RemoteContext ctx;

    public ChainCompletor() {
        this(Shell.get().getContextObject(RemoteContext.class));
    }

    public ChainCompletor(RemoteContext ctx) {
        this.ctx = ctx;
    }

    protected Document fetchDocument(String path) {
        try {
            return ctx.resolveDocument(path);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public int complete(String buffer, int cursor, List clist) {
        if (buffer == null) {
            buffer = "";
        }
        for (OperationDocumentation op : ctx.getSession().getOperations().values()) {
            if ("Chain".equals(op.category)) {
                if (op.id.startsWith(buffer)) {
                    clist.add(op.id);
                }
            }
        }
        if (clist.isEmpty()) {
            return -1;
        }
        if (clist.size() == 1) {
            return 0;
        }
        Collections.sort(clist);
        return 0;
    }
}
