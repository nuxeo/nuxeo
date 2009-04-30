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
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.common.SearchServiceDelegate;
import org.nuxeo.ecm.shell.CommandLine;

public class IndexOverloadCommand extends AbstractCommand {
    private static final Log log = LogFactory.getLog(IndexOverloadCommand.class);

    private void printHelp() {
        System.out.println("");
        System.out.println("Syntax: indexOverLoad doc_path nb [batch_size]");
        System.out.println(" doc_path path of the doc to index");
        System.out.println(" nb number of concurrent indexing job to start");
        System.out.println(" size of indexing batch");
    }

    @Override
    public void run(CommandLine cmdLine) throws Exception {
        String docPath = null;
        int nb = 0;
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
                log.error("Failed to parse nb", t);
                printHelp();
                return;
            }
        } else {
            printHelp();
            return;
        }

        if (elements.length >= 3) {
            try {
                batchSize = Integer.parseInt(elements[2]);
            } catch (Throwable t) {
                log.error("Failed to parse batch size", t);
                printHelp();
                return;
            }
        } else {
            batchSize = -1;
        }

        index(docPath, nb, batchSize);
    }

    public void index(String path, int nb, int batchSize) throws Exception {
        SearchService searchService;
        try {
            searchService = SearchServiceDelegate.getRemoteSearchService();
            if (searchService == null) {
                throw new IndexingException("Cannot find search service");
            }
        } catch (Exception e) {
            throw new IndexingException("Cannot find search service", e);
        }

        int oldBatchSize = searchService.getIndexingDocBatchSize();
        DocumentModel dm = context.getRepositoryInstance().getDocument(
                new PathRef(path));
        if (batchSize != -1 && batchSize > 0) {
            searchService.setIndexingDocBatchSize(batchSize);
        }

        if (nb > 100) {
            nb = 100;
            log.info("Nb of indexing tasks reduced to max queue size (100)");
        }
        log.info("Sending tasks to search service thread pool");
        for (int i = 0; i < nb; i++) {
            log.info("!!! explicit indexing is disabled !!!");
        }

        while (searchService.getActiveIndexingTasks() > 0) {
            Thread.sleep(500);
            long nbTasksToGo = searchService.getActiveIndexingTasks()
                    + searchService.getIndexingWaitingQueueSize();
            log.info("still " + nbTasksToGo + " tasks to run");
        }

        log.info("Terminated : doc indexed " + nb + " times");

        searchService.setIndexingDocBatchSize(oldBatchSize);
    }

}
