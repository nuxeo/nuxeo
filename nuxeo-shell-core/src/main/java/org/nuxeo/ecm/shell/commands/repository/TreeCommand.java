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
 *     fsommavilla
 *
 * $Id$
 */

package org.nuxeo.ecm.shell.commands.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.repository.RepositoryInstance;
import org.nuxeo.ecm.shell.CommandLine;
import org.nuxeo.ecm.shell.commands.ColorHelper;

/**
 * @author <a href="mailto:fsommavilla@nuxeo.com">Fabrice Sommavilla</a>
 *
 */
public class TreeCommand extends AbstractCommand {
    private static final Log log = LogFactory.getLog(TreeCommand.class);
    private int size = 0;

    @Override
    public void run(CommandLine cmdLine) throws Exception {
        long t0 = System.currentTimeMillis();
        getTreeList(null);
        log.info("\n" + getSize() + " docs listed in "
                + (System.currentTimeMillis() - t0) + "ms\n");

    }

    public int getTreeList(DocumentRef ref) throws Exception {
        RepositoryInstance repo = context.getRepositoryInstance();
        DocumentRef docRef = context.getCurrentDocument();

        if (ref != null && !ref.equals(docRef)) {
            docRef = ref;
        }

        DocumentModelList docs = repo.getChildren(docRef);
        for (DocumentModel doc : docs) {
            boolean isFolder = doc.isFolder();
            String docType = doc.getType();
            // Default domain
            if (isFolder && docType.equals("Domain")) {
                log.info(ColorHelper.decorateName(doc, "+ "
                        + doc.getName()));
                log.info(ColorHelper.decorateName(doc, "|"));
                getTreeList(doc.getRef());
                // Default Root Type
            } else if (isFolder
                    && (docType.equals("TemplateRoot")
                            || docType.equals("SectionRoot") || docType.equals("WorkspaceRoot"))) {
                log.info(ColorHelper.decorateName(doc, "| |-+ "
                        + doc.getName()));
                log.info(ColorHelper.decorateName(doc, "| | |"));
                getTreeList(doc.getRef());
                // Default SubType
            } else if (isFolder
                    && (docType.equals("Workspace")
                            || docType.equals("Section") || docType.equals("Template"))) {
                log.info(ColorHelper.decorateName(doc, "| | |-+ "
                        + doc.getName()));
                log.info(ColorHelper.decorateName(doc, "| | | |"));
                getTreeList(doc.getRef());
                // Documents
            } else {
                log.info(ColorHelper.decorateBranchesInBlue("| | | |")
                        + ColorHelper.decorateName(doc, "- " + doc.getName()));
            }
        }
        setSize(size + docs.size());

        return size;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

}
