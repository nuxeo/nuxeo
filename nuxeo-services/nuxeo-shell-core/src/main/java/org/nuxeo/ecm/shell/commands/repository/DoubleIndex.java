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
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.shell.CommandLine;

public class DoubleIndex extends AbstractCommand {

    private static final Log log = LogFactory.getLog(DoubleIndex.class);

    private void printHelp() {
        System.out.println("");
        System.out.println("Syntax: DoubleIndex doc_path [nb] ");
        System.out.println(" doc_path path of the doc to index");
        System.out.println(" nb number of sync indexing request inside transaction");
    }

    @Override
    public void run(CommandLine cmdLine) throws Exception {
        String docPath = null;
        int nb = 2;
        int batchSize;
        String[] elements = cmdLine.getParameters();

        if (elements.length >= 1) {
            if ("help".equals(elements[0])) {
                printHelp();
                return;
            }
            docPath = elements[0];
        }

        if (elements.length >= 2) {
            try {
                nb = Integer.parseInt(elements[1]);
            } catch (Throwable t) {
                log.error("Failed to parse nb");
                printHelp();
                return;
            }
        } else {
            printHelp();
            return;
        }

        index(docPath, nb);
    }

    public void index(String path, int nb) throws Exception {
        DocumentModel dm = context.getRepositoryInstance().getDocument(
                new PathRef(path));
        String title = (String) dm.getProperty("dublincode", "title");
        for (int i = 0; i < nb - 1; i++) {
            dm.setProperty("dublincore", "title", title + i);
            context.getRepositoryInstance().saveDocument(dm);
        }

        dm.setProperty("dublincore", "title", title);
        context.getRepositoryInstance().saveDocument(dm);
        context.getRepositoryInstance().save();
    }

}
