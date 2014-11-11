/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.shell.commands.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.shell.CommandLine;

public class MkDirCommand extends AbstractCommand {
    private static final Log log = LogFactory.getLog(MkDirCommand.class);

    @Override
    public void run(CommandLine cmdLine) throws Exception {
        String[] elements = cmdLine.getParameters();
        if (elements.length == 0) {
            log.error("SYNTAX ERROR: the mkdir command must take at least one argument: mkdir path_or_name [type]");
            return;
        }
        DocumentModel parent = null;
        String dirName = null;
        if (elements.length >= 1) {
            Path path = new Path(elements[0]);
            path = path.removeTrailingSeparator();

            if (path.isAbsolute()) {
                // abs path
                Path parentPath = path.removeLastSegments(1);
                parent = context.fetchDocument(parentPath);
                dirName = path.lastSegment();
            } else {
                // relative path
                DocumentRef currentDocRef = context.getCurrentDocument();
                parent = context.getRepositoryInstance().getSession().getDocument(
                        currentDocRef);
                dirName = path.toString();
            }
        }
        String dirType;
        if (elements.length == 2) {
            dirType = elements[1];
        } else {
            dirType = "Folder";
        }

        createDir(parent, dirName, dirType);
    }

    private DocumentModel createDir(DocumentModel parent, String name,
            String docType) throws Exception {
        CoreSession session = context.getRepositoryInstance().getSession();
        DocumentModel doc = session.createDocumentModel(docType);
        doc.setPathInfo(parent.getPathAsString(), name);
        doc.setProperty("dublincore", "title", name);

        doc = session.createDocument(doc);
        session.save();
        return doc;
    }

}
