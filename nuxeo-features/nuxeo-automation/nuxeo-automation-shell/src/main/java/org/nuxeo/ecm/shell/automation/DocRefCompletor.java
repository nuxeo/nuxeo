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
import org.nuxeo.ecm.automation.client.jaxrs.model.Documents;
import org.nuxeo.ecm.shell.Shell;
import org.nuxeo.ecm.shell.ShellException;
import org.nuxeo.ecm.shell.utils.Path;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class DocRefCompletor implements Completor {

    protected RemoteContext ctx;

    public DocRefCompletor() {
        this(Shell.get().getContextObject(RemoteContext.class));
    }

    public DocRefCompletor(RemoteContext ctx) {
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
        Document cdoc = ctx.getDocument();
        String prefix = "";
        if (buffer != null) {
            if (buffer.endsWith("/")) {
                Path path = new Path(buffer).removeTrailingSeparator();
                prefix = buffer;
                buffer = "";
                cdoc = fetchDocument(path.toString());
            } else if (buffer.indexOf('/') != -1) {
                Path path = new Path(buffer);
                buffer = path.lastSegment();
                prefix = path.getParent().toString();
                cdoc = fetchDocument(prefix);
                prefix = prefix + '/';
            }
        }
        if (cdoc == null) {
            return -1;
        }
        try {
            Documents docs = ctx.getDocumentService().getChildren(cdoc);
            for (Document doc : docs) {
                String name = new Path(doc.getPath()).lastSegment();
                if (buffer == null) {
                    clist.add(name);
                } else if (name.startsWith(buffer)) {
                    clist.add(prefix + name);
                }
            }
            Collections.sort(clist);
            if (clist.size() == 1) { // TODO add trailing / only if folderish
                clist.set(0, ((String) clist.get(0)) + '/');
            }
            return clist.isEmpty() ? -1 : 0;
        } catch (Exception e) {
            throw new ShellException("Failed to gather children for " + buffer,
                    e);
        }
    }
}
