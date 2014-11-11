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

import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.client.NuxeoClient;
import org.nuxeo.ecm.shell.CommandLine;

public class RepoStatsCommand extends AbstractCommand {

    private static final Log log = LogFactory.getLog(RepoStatsCommand.class);

    private static class StatInfo {
        private final Map<String, Long> docsPerTypes;

        private long totalBlobSize = 0;

        private long totalBlobNb = 0;

        private long lastTotalNbDocs = 0;

        private long lastTotalBlobSize = 0;

        private long lastTotalBlobNb = 0;

        private long maxDepth = 0;

        private String maxDepthPath;

        private long maxChildren;

        private String maxChildrenPath;

        private long maxBlobSize;

        private String maxBlobSizePath;

        StatInfo() {
            docsPerTypes = new ConcurrentHashMap<String, Long>();
        }

        public Map<String, Long> getDocsPerType() {
            return docsPerTypes;
        }

        public synchronized void addDoc(String type, Path path) {
            Long counter = docsPerTypes.get(type);
            if (path.segmentCount() > maxDepth) {
                maxDepth = path.segmentCount();
                maxDepthPath = path.toString();
            }
            if (counter == null) {
                counter = 1L;
            } else {
                counter += 1;
            }
            docsPerTypes.put(type, counter);
        }

        public synchronized void addBlob(long size, Path path) {
            totalBlobSize += size;
            totalBlobNb += 1;
            if (size > maxBlobSize) {
                maxBlobSize = size;
                maxBlobSizePath = path.toString();
            }
        }

        public synchronized void childrenCount(long children, Path path) {
            if (children > maxChildren) {
                maxChildren = children;
                maxChildrenPath = path.toString();
            }
        }

        public long getTotalNbDocs() {
            long total = 0;
            for (String k : docsPerTypes.keySet()) {
                total += docsPerTypes.get(k);
            }
            lastTotalNbDocs = total;
            return total;
        }

        public long getTotalBlobSize() {
            lastTotalBlobSize = totalBlobSize;
            return totalBlobSize;
        }

        public long getTotalBlobNumber() {
            lastTotalBlobNb = totalBlobNb;
            return totalBlobNb;
        }

        public long getLastTotalNbDocs() {
            return lastTotalNbDocs;
        }

        public long getLastTotalBlobSize() {
            return lastTotalBlobSize;
        }

        public long getLastTotalBlobNumber() {
            return lastTotalBlobNb;
        }

    }

    public static StatInfo info;

    protected Boolean includeBlob = true;

    private long t0;

    private long lastStatTime;

    private long printStatCount;

    private static final String printStatHeader = "time     threads      docs  docs/s     blobs   size(M)     M/s tdocs/s tdoc/th/s";

    private static final SimpleDateFormat timeFormater = new SimpleDateFormat(
            "HH:mm:ss");

    protected class StatTask implements Runnable {

        private final CoreSession session;

        private final DocumentModel rootDoc;

        protected StatTask(DocumentModel rootDoc) throws Exception {
            session = NuxeoClient.getInstance().openRepository();
            this.rootDoc = rootDoc;
        }

        public void dispose() {
            try {
                CoreInstance.getInstance().close(session);
            } catch (Exception e) {
                log.error(e);
            }
        }

        public synchronized void run() {
            try {
                recurse(rootDoc);
            } catch (ClientException e) {
                try {
                    CoreInstance.getInstance().close(session);
                } catch (Exception e1) {
                    log.error(e);
                }
            }
        }

        private StatTask getNextTask(DocumentModel root) {
            if (pool.getQueue().size() > 1) {
                return null;
            }
            StatTask newTask;
            try {
                newTask = new StatTask(root);
            } catch (Exception e) {
                return null;
            }
            return newTask;
        }

        private void recurse(DocumentModel doc) throws ClientException {
            fetchInfoFromDoc(session, doc);
            if (doc.isFolder()) {
                long children = 0;
                for (DocumentModel child : session.getChildren(doc.getRef())) {
                    children += 1;
                    if (child.isFolder()) {
                        StatTask newTask = getNextTask(child);
                        if (newTask != null) {
                            pool.execute(newTask);
                        } else {
                            recurse(child);
                        }
                    } else {
                        fetchInfoFromDoc(session, child);
                    }
                }
                RepoStatsCommand.info.childrenCount(children, doc.getPath());
            }
        }

        private void fetchInfoFromDoc(CoreSession session, DocumentModel doc)
                throws UnsupportedOperationException, ClientException {
            RepoStatsCommand.info.addDoc(doc.getType(), doc.getPath());

            // XXX check versions too
            if (includeBlob && doc.hasSchema("file")) {
                // Long size = (Long)
                // doc.getPart("file").resolvePath("content/length").getValue();
                Long size = (Long) doc.getPart("file").get("content").get(
                        "length").getValue();
                if (size != null) {
                    RepoStatsCommand.info.addBlob(size, doc.getPath());
                }
                /*
                 * Blob blob = (Blob) doc.getProperty("file", "content"); if
                 * (blob!=null) RepoStatsCommand.info.addBlob(blob.getLength());
                 */
            }
        }
    }

    private void printHelp() {
        System.out.println("");
        System.out.println("Synthax: repostats doc_path");
        System.out.println(" doc_path: reprository path from where stats must be gathered");
        System.out.println(" [nbThreads]: defines the number of cucurrent threads (optional, default=5)");
        System.out.println(" [includeBlobs]: Boolean indicating if Blob data should introspected (optional, default=false)");
        System.out.println("Information displayed while gathering data:");
        System.out.println(" time, number of running threads,");
        System.out.println(" total number of documents processed, average of documents per second processed,");
        System.out.println(" number of blobs processed, blobs size in megabytes, average megabytes per second (M/s),");
        System.out.println(" trend of document per second processed (tdocs/s), trend of document per second and per active thread.");
    }

    private void printStats(int activeThread) {
        long now = System.currentTimeMillis();
        long lastProcessed = info.getLastTotalNbDocs();
        long processed = info.getTotalNbDocs();
        long blobProcessed = info.getTotalBlobNumber();
        long blobSize = info.getTotalBlobSize();
        long t1;
        if ((activeThread == 0) && (processed == lastProcessed)) {
            t1 = lastStatTime;
        } else {
            t1 = now;
        }

        double trend = (processed - lastProcessed)
                / ((t1 - lastStatTime) / 1000.);
        double trendThread = trend / activeThread;

        if ((printStatCount % 25) == 0) {
            log.info(printStatHeader);
        }
        log.info(String.format("%s %7d %9d %7.2f %9d %9.2f %7.3f %7.2f %9.2f",
                timeFormater.format(now), activeThread, processed,
                (double) processed / (t1 - t0) * 1000, blobProcessed,
                blobSize / 1024. / 1024., (double) blobSize / (t1 - t0) / 1024.
                        / 1024. * 1000, trend, trendThread));
        printStatCount++;
        lastStatTime = t1;
    }

    protected static ThreadPoolExecutor pool;

    @Override
    public void run(CommandLine cmdLine) throws Exception {

        String[] elements = cmdLine.getParameters();

        if (elements.length == 0) {
            log.error("SYNTAX ERROR: the repostats command must take at least one argument");
            printHelp();
            return;
        }

        if ("help".equals(elements[0])) {
            printHelp();
            return;
        }

        DocumentModel root = null;
        if (elements.length >= 1) {
            Path path = new Path(elements[0]);
            try {
                root = context.fetchDocument(path);
            } catch (Exception e) {
                log.error("Failed to retrieve the given folder", e);
                return;
            }
        }

        int nbThreads = 5;
        if (elements.length >= 2) {
            try {
                nbThreads = Integer.parseInt(elements[1]);
            } catch (Exception e) {
                log.error("Failed to parse number of threads", e);
                return;
            }
        } else {
            log.info(" Using default Thread number: " + nbThreads);
        }

        if (elements.length >= 3) {
            try {
                includeBlob = Boolean.parseBoolean(elements[2]);
            } catch (Exception e) {
                log.error("Failed to parse the includeBlob parameter", e);
                return;
            }
        } else {
            includeBlob = false;
        }

        info = new StatInfo();

        StatTask task = new StatTask(root);

        pool = new ThreadPoolExecutor(nbThreads, nbThreads, 500L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(100));

        t0 = System.currentTimeMillis();

        pool.execute(task);

        Thread.sleep(100);

        do {
            printStats(pool.getActiveCount());
            Thread.sleep(1000);
        } while (pool.getActiveCount() > 0);
        printStats(pool.getActiveCount());

        log.info("Total number of documents:" + info.getTotalNbDocs());
        Map<String, Long> docsPerType = info.getDocsPerType();
        for (String type : docsPerType.keySet()) {
            log.info("   Number of " + type + " docs: " + docsPerType.get(type));
        }
        if (includeBlob) {
            log.info("Total number of blobs:" + info.getTotalBlobNumber());
            log.info("   Total size of blobs (M): "
                    + ((float) info.getTotalBlobSize() / (1024 * 1024)));
            log.info("   Average blob size (K): "
                    + ((float) info.getTotalBlobSize() / 1024)
                    / info.getTotalBlobNumber());
            log.info("   Maximum blob size (M): "
                    + ((float) info.maxBlobSize / 1024. / 1024.) + " in "
                    + info.maxBlobSizePath);
        }
        log.info("Folders");
        log.info("   Maximum depth: " + info.maxDepth + " in "
                + info.maxDepthPath);
        log.info("   Maximum children: " + info.maxChildren + " in "
                + info.maxChildrenPath);

        long t1 = System.currentTimeMillis();
        log.info("Repository performance during stats was " + 1000
                * ((float) info.getTotalNbDocs()) / (t1 - t0) + " doc/s");
    }

}
