/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.shell.commands;

import java.util.List;

import jline.Completor;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.shell.CommandContext;
import org.nuxeo.ecm.shell.CommandLineService;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DocumentNameCompletor implements Completor {

    private final CommandLineService service;

    public DocumentNameCompletor(CommandLineService service) {
        this.service = service;
    }

    @SuppressWarnings({"unchecked"})
    public int complete(String buf, int off, List candidates) {
        if (buf == null) {
            buf = "";
        }
        Path path = new Path(buf);
        String prefix = path.lastSegment();
        if (path.hasTrailingSeparator()) {
            prefix = "";
        } else {
            path = path.removeLastSegments(1);
        }

        if (prefix == null) {
            prefix = "";
        }

        try {
            CommandContext context = service.getCommandContext();
            DocumentModel parent = context.fetchDocument(path);
            if (!parent.hasFacet("Folderish")) {
                return -1;
            }
            DocumentModelList docs = context.getRepositoryInstance().getChildren(parent.getRef());

            if (buf.length() == 0) {
                for (DocumentModel doc : docs) {
                    candidates.add(doc.getName());
                }
            } else {
                for (DocumentModel doc : docs) {
                    String name = doc.getName();
                    if (name.startsWith(prefix)) {
                        candidates.add(name);
                    }
                }
            }

            return buf.length()-prefix.length();

        } catch (Exception e) {
            return -1;
        }
    }

}
