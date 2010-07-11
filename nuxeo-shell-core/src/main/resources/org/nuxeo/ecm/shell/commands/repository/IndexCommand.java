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

package org.nuxeo.ecm.shell.commands.repository;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.common.SearchServiceDelegate;
import org.nuxeo.ecm.shell.CommandLine;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class IndexCommand extends AbstractCommand {
    private static final Log log = LogFactory.getLog(IndexCommand.class);

    private static final String REPO_NAME = "default";

    private static final long SLEEPING_DURATION = 10000;

    private void printHelp() {
        System.out.println("");
        System.out.println("Syntax: index [repository_name] [path] [batch_size] [full_text]");
        System.out.println(" repository_name (optionnal, default=default): name of the repository to reindex");
        System.out.println(" path (optionnal, default=/): path used for reindex");
        System.out.println(" batch_size (optionnal, default=20): size of reindex batch");
        System.out.println(" full_text (optionnal, default=true : include fullText in indexing true/false");
    }

    @Override
    public void run(CommandLine cmdLine) throws Exception {
        String repo = REPO_NAME;
        String path = "/";
        Boolean fullText = true;
        int batchSize = 20;

        String[] elements = cmdLine.getParameters();

        if (elements.length >= 1) {
            if ("help".equals(elements[0])) {
                printHelp();
                return;
            }
            repo = elements[0];
        }
        if (elements.length >= 2) {
            path = elements[1];
        }
        if (elements.length >= 3) {
            try {
                batchSize = Integer.parseInt(elements[2]);
            } catch (Throwable t) {
                batchSize = 20;
                log.error(
                        "Failed to parse batch size, using default batchSize="
                                + batchSize, t);
            }
        }

        if (elements.length >= 4) {
            try {
                fullText = Boolean.parseBoolean(elements[3]);
            } catch (Throwable t) {
                log.error("Failed to parse fullText option : skipping", t);
                fullText = true;
            }
        }

        index(repo, path, batchSize, fullText);
    }

    public void index(String repoName, String path, int batchSize,
            Boolean fullText) {
        SimpleDateFormat timeFormater = new SimpleDateFormat("HH:mm:ss");
        try {
            SearchService searchService = SearchServiceDelegate.getRemoteSearchService();

            if (searchService == null) {
                throw new IndexingException("Cannot find search service");
            }

            long initialIndexingThreadNumber = searchService.getTotalCompletedIndexingTasks();

            int orgBatchSize = searchService.getIndexingDocBatchSize();
            searchService.setIndexingDocBatchSize(batchSize);
            // Reindex from the root and do not compute fulltext
            searchService.reindexAll(repoName, path, fullText);
            log.info(timeFormater.format(new Date()) + " Indexing: " + path
                    + " indexingDocBatchSize: " + orgBatchSize + " fullText: "
                    + fullText);
            double s = System.currentTimeMillis();
            long lastNbIndexedDocs = 0;
            long lastDiff = 0;
            double lastTm = s / 1000;

            while (true) {
                Thread.sleep(SLEEPING_DURATION);
                long nbIndexedDocs = searchService.getTotalCompletedIndexingTasks()
                        - initialIndexingThreadNumber;
                long diff = nbIndexedDocs - lastNbIndexedDocs;
                lastNbIndexedDocs = nbIndexedDocs;
                if ((diff == 0) && (lastDiff == 0)
                        && (searchService.getActiveIndexingTasks() <= 0)) {
                    break;
                }
                lastDiff = diff;
                double tm = (System.currentTimeMillis() - s) / 1000;
                double flow = nbIndexedDocs / tm;
                double dflow = diff / (tm - lastTm);
                lastTm = tm;
                log.info(String.format(
                        "%s indexed %5d docs at %6.2f docs/s (%7d docs at %6.2f docs/s) %d threads %d queued (batch %d). \n",
                        timeFormater.format(new Date()), diff, dflow,
                        nbIndexedDocs, flow,
                        searchService.getActiveIndexingTasks(),
                        searchService.getIndexingWaitingQueueSize(),
                        searchService.getIndexingDocBatchSize()));
            }
            searchService.setIndexingDocBatchSize(orgBatchSize);
            double tm = (System.currentTimeMillis() - s) / 1000;

            log.info("Indexing " + lastNbIndexedDocs + " done in " + tm
                    + " seconds");

        } catch (Exception e) {
            log.error(e);
        }
    }
}
