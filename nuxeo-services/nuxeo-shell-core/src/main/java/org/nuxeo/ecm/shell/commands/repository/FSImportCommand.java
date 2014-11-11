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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.api.repository.RepositoryInstance;
import org.nuxeo.ecm.core.client.NuxeoClient;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.common.SearchServiceDelegate;
import org.nuxeo.ecm.shell.CommandLine;
import org.nuxeo.runtime.services.streaming.FileSource;
import org.nuxeo.runtime.services.streaming.StreamSource;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FSImportCommand extends AbstractCommand {

    private static final int MAX_IDX_BATCH_SIZE = 50;

    protected static final Log log = LogFactory.getLog(FSImportCommand.class);

    private static final String NO_JMS_OPTION = "no-jms";

    private static final String NO_SYNC_IDX_OPTION = "no-sync-indexing";

    private static final String ONLY_CORE = "only-core";

    private static final String FULL_INDEXING = "full-indexing";

    private static final long RECYCLE_CORE_SESSION_INTERVAL = 100;

    private void printHelp() {
        System.out.println("");
        System.out.println("Syntax: fsimport local_file_path [remote_path] [batch_size] [option]");
        System.out.println(" local_file_path : path to a local directory containing files to import");
        System.out.println(" remote_path (optionnal, default=/): reprository path where documents must be created");
        System.out.println(" batch_size (optionnal, default=50): size of import batch");
        System.out.println(" option : ");
        System.out.println("          " + NO_JMS_OPTION
                + " : desactivate JMS events on document creation");
        System.out.println("          "
                + ONLY_CORE
                + " : desactivate JMS events on document creation and synchronous indexing");
        System.out.println("          " + NO_SYNC_IDX_OPTION
                + " : desactivate synchronous indexing");
        System.out.println("          " + FULL_INDEXING
                + " : synchronous and asynchronous indexing activated");
        System.out.println("           if option is not set, no indexing is done");
    }

    @Override
    public void run(CommandLine cmdLine) throws Exception {

        String[] elements = cmdLine.getParameters();
        if (elements.length == 0) {
            log.error("SYNTAX ERROR: the fsimport command must take at least one argument: "
                    + "fsimport local_file_path [remote_path] [batch_size] ");
            printHelp();
            return;
        }

        File localFile = new File(elements[0]);
        if ("help".equals(elements[0])) {
            printHelp();
            return;
        }

        DocumentModel parent;
        if (elements.length >= 2) {
            Path path = new Path(elements[1]);
            try {
                parent = context.fetchDocument(path);
            } catch (Exception e) {
                log.error("Failed to retrieve the given folder", e);
                return;
            }
        } else {
            parent = context.fetchDocument();
        }

        Integer batchSize = 50;
        if (elements.length >= 3) {
            try {
                batchSize = Integer.parseInt(elements[2]);
            } catch (Throwable t) {
                batchSize = 10;
                log.error(
                        "Failed to parse batch size, using default batchSize="
                                + batchSize, t);
            }
        }

        String optim = ONLY_CORE;

        if (elements.length >= 4) {
            optim = elements[3];
        }

        Boolean blockJMS = false;
        Boolean blockSyncIndexing = false;
        if (ONLY_CORE.equals(optim)) {
            blockJMS = true;
            blockSyncIndexing = true;
            log.info("JMS and Sync indexing will be desactivated during this import");
        } else if (NO_SYNC_IDX_OPTION.equals(optim)) {
            blockJMS = false;
            blockSyncIndexing = true;
            log.info("Sync indexing will be desactivated for during import");
        } else if (NO_JMS_OPTION.equals(optim)) {
            blockJMS = true;
            blockSyncIndexing = false;
            log.info("JMS forwarding will be desactivated for during import");
        } else {
            log.info("Sync indexing abd JMS forwarding will be activated for during import");
        }

        RepositoryInstance repo = context.getRepositoryInstance();

        ImportTask task = new ImportTask(repo, batchSize, blockJMS,
                blockSyncIndexing);
        task.setInputFile(localFile);
        task.setTargetFolder(parent);
        log.info("Starting import task");
        task.run();
    }

    protected ImportTask getTask() {
        // create new task if needed - lookup into the pool of tasks and get a
        // task which is not running
        return null;
    }

    /**
     * A worker is importing a sub-tree
     *
     * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
     */
    protected class ImportTask implements Runnable {

        boolean isRunning = false;

        long t0;

        long uploadedFiles;

        long commits = 0;

        long uploadedKO;

        int batchSize;

        int indexingBatchSize;

        CoreSession session;

        DocumentModel rootDoc;

        File rootFile;

        Boolean blockJMS = false;

        Boolean blockSyncIndexing = false;

        protected ImportTask(CoreSession session, int batchSize,
                Boolean blockJMS, Boolean blockSyncIndexing) {
            this(session, batchSize);
            this.blockJMS = blockJMS;
            this.blockSyncIndexing = blockSyncIndexing;
        }

        protected ImportTask(CoreSession session, int batchSize) {
            if (session == null) {
                // TODO - open new session
            }
            this.session = session;
            this.batchSize = batchSize;
            indexingBatchSize = batchSize;
            uploadedFiles = 0;
        }

        protected ImportTask(File rootFile, DocumentModel rootDoc) {
            this(null, 50);
        }

        protected ImportTask(CoreSession session, File rootFile,
                DocumentModel rootDoc) {
            this(session, 50);
        }

        protected void commit() throws Exception {
            uploadedFiles++;
            long tcomit = System.currentTimeMillis();
            if (uploadedFiles % 10 == 0) {
                log.info(uploadedFiles + " doc created ...");
            }
            if (uploadedFiles % batchSize == 0) {
                log.debug("Comiting Core Session after " + uploadedFiles
                        + " files");
                log.debug(uploadedFiles / ((tcomit - t0) / 1000.0) + " doc/s");
                log.debug(uploadedKO / ((tcomit - t0) / 1000.0) + " KB/s");
                session.save();
                commits += 1;

                if (commits % RECYCLE_CORE_SESSION_INTERVAL == 0) {
                    recyleCoreSession();
                }
            }
        }

        protected void recyleCoreSession() throws Exception {
            log.debug("Recycling core session ");
            String repoName = session.getRepositoryName();
            session.disconnect();
            session = NuxeoClient.getInstance().openRepository(repoName);
        }

        public DocumentModel createDirectory(DocumentModel parent, File file)
                throws Exception {
            String name = getValidNameFromFileName(file.getName());

            Map<String, Object> options = new HashMap<String, Object>();
            options.put("BLOCK_JMS_PRODUCING", true);
            String docType = "Folder";
            DocumentModel doc = session.createDocumentModel(docType, options);
            doc.setPathInfo(parent.getPathAsString(), name);
            doc.setProperty("dublincore", "title", file.getName());
            if (blockJMS) {
                doc.putContextData("BLOCK_JMS_PRODUCING", true);
            }
            if (blockSyncIndexing) {
                doc.putContextData("BLOCK_SYNC_INDEXING", true);
            }
            // doc = session.saveDocument(doc);
            log.debug("Creating Folder " + name + " at "
                    + parent.getPathAsString());
            doc = session.createDocument(doc);
            // save session if needed
            commit();
            return doc;
        }

        public DocumentModel createFile(DocumentModel parent, File file)
                throws Exception {
            if (!file.exists()) {
                log.warn("non readable file: " + file.getName());
                return null;
            }
            String mimeType = getMimeType(file);
            String docType = "File";
            String name = getValidNameFromFileName(file.getName());
            String fileName = file.getName();

            Map<String, Object> options = new HashMap<String, Object>();
            options.put("BLOCK_JMS_PRODUCING", true);
            DocumentModel doc = session.createDocumentModel(docType, options);
            if (blockJMS) {
                doc.putContextData("BLOCK_JMS_PRODUCING", true);
            }
            if (blockSyncIndexing) {
                doc.putContextData("BLOCK_SYNC_INDEXING", true);
            }
            doc.setPathInfo(parent.getPathAsString(), name);
            doc.setProperty("dublincore", "title", file.getName());
            doc.setProperty("file", "filename", fileName);
            // create a stream source - use the PlatformService.xml file for
            // example
            StreamSource src = new FileSource(file);
            // to use the streaming service we need to use StreamingBlob
            // implementation
            StreamingBlob streamingBlob = new StreamingBlob(src, mimeType);
            doc.setProperty("file", "content", streamingBlob);
            // doc = session.saveDocument(doc);
            long kbSize = file.length() / 1024;
            log.debug("Creating doc " + name + " at "
                    + parent.getPathAsString() + " with file " + fileName
                    + " of size " + kbSize + "KB");
            doc = session.createDocument(doc);

            uploadedKO += kbSize;
            // save session if needed
            commit();
            return doc;
        }

        protected void upload(DocumentModel parent, File file) throws Exception {
            if (file.isDirectory()) {
                DocumentModel folder = createDirectory(parent, file);
                File[] files = file.listFiles();
                if (files.length > 0) {
                    // TODO : here get a new TaskImporter if available to start
                    // processing the sub-tree
                    ImportTask task = getTask();
                    if (task != null) {
                        task.run();
                    } else {
                        for (File f : file.listFiles()) {
                            upload(folder, f);
                        }
                    }
                }
            } else {
                createFile(parent, file);
            }
        }

        public void setInputFile(File rootFile) {
            this.rootFile = rootFile;
        }

        public void setTargetFolder(DocumentModel rootDoc) {
            this.rootDoc = rootDoc;
        }

        /**
         * TODO: Modify this to get right mime types depending on the file
         * input.
         */
        protected String getMimeType(File file) {
            // Dummy MimeType detection : plug nuxeo Real MimeType service to
            // have better results
            String fileName = file.getName();

            if (fileName == null) {
                return "application/octet-stream";
            } else if (fileName.endsWith(".doc")) {
                return "application/msword";
            } else if (fileName.endsWith(".xls")) {
                return "application/vnd.ms-excel";
            } else if (fileName.endsWith(".ppt")) {
                return "application/vnd.ms-powerpoint";
            } else if (fileName.endsWith(".txt")) {
                return "text/plain";
            } else if (fileName.endsWith(".html")) {
                return "text/html";
            } else if (fileName.endsWith(".xml")) {
                return "text/xml";
            } else if (fileName.endsWith(".jpg")) {
                return "image/jpeg";
            } else if (fileName.endsWith(".jpeg")) {
                return "image/jpeg";
            } else if (fileName.endsWith(".gif")) {
                return "image/gif";
            } else if (fileName.endsWith(".odt")) {
                return "application/vnd.oasis.opendocument.text";
            } else if (fileName.endsWith(".zip")) {
                return "application/zip";
            } else {
                return "application/octet-stream";
            }
        }

        // TODO isRunning is not yet handled correctly
        public boolean isRunning() {
            synchronized (this) {
                return isRunning;
            }
        }

        public synchronized void run() {
            t0 = System.currentTimeMillis();
            synchronized (this) {

                if (isRunning) {
                    throw new IllegalStateException("Task already running");
                }
                isRunning = true;
                if (rootDoc == null || rootFile == null) {
                    isRunning = false;
                    throw new IllegalArgumentException(
                            "target folder and source file must be specified");
                }
            }
            try {
                SearchService searchService = SearchServiceDelegate.getRemoteSearchService();
                Integer oldBatchSize = null;
                long initialCompletedIndexingTasks = 0;
                if (searchService != null) {
                    oldBatchSize = searchService.getIndexingDocBatchSize();
                    if (batchSize > MAX_IDX_BATCH_SIZE) {
                        indexingBatchSize = MAX_IDX_BATCH_SIZE;
                    } else {
                        indexingBatchSize = batchSize;
                    }
                    searchService.setIndexingDocBatchSize(indexingBatchSize);
                    log.info("Setting indexing batch size to "
                            + searchService.getIndexingDocBatchSize());
                    log.info("Indexing thread pool size = "
                            + searchService.getNumberOfIndexingThreads());

                    initialCompletedIndexingTasks = searchService.getTotalCompletedIndexingTasks();
                    log.debug("Already completed indexing tasks= "
                            + initialCompletedIndexingTasks);
                    if (searchService.getActiveIndexingTasks() > 0) {
                        log.warn("Indexing queue is not empty ");
                    }

                }

                if (blockJMS) {
                    log.info("JMS event production is disabled ");
                }
                if (blockSyncIndexing) {
                    log.info("Synchronous indexing is disabled");
                }

                upload(rootDoc, rootFile);
                session.save();
                log.info("doc upload terminated");
                if (searchService != null) {
                    log.info("sync indexing terminated");
                }
                long t1 = System.currentTimeMillis();
                // Thread.sleep(2000);
                log.info(uploadedFiles + " doc created in " + (t1 - t0) + "ms");
                log.info(uploadedFiles / ((t1 - t0) / 1000.0) + " doc/s");
                log.info(uploadedKO / ((t1 - t0) / 1000.0) + " KB/s");
                if (searchService != null) {
                    log.info("waiting for asynchronous indexing to finish");
                    while (searchService.getActiveIndexingTasks() > 0) {
                        Thread.sleep(2500);
                        long completedTasks = searchService.getTotalCompletedIndexingTasks();
                        log.debug("completed indexing tasks= " + completedTasks);

                        long nbIndexedDocs = searchService.getTotalCompletedIndexingTasks()
                                - initialCompletedIndexingTasks;
                        long indexToGo = uploadedFiles - nbIndexedDocs;
                        if (indexToGo > 0) {
                            log.info(nbIndexedDocs + " doc indexed ("
                                    + indexToGo + " to go ... )");
                        } else {
                            log.info(nbIndexedDocs
                                    + " doc indexed (processing additionnal reindex JMS events)");
                        }
                        // adaptBatchSize(indexToGo, searchService);
                    }

                    long t2 = System.currentTimeMillis();
                    log.info("Async indexing completed");
                    log.info(uploadedFiles + " doc indexed in " + (t2 - t0)
                            + "ms");
                    log.info(uploadedFiles / ((double) ((t2 - t0) / 1000))
                            + " doc/s");
                    searchService.setIndexingDocBatchSize(oldBatchSize);
                }

            } catch (Exception e) {
                log.error("Error during import", e);
            } finally {
                synchronized (this) {
                    isRunning = false;
                }
            }
        }

        protected String getValidNameFromFileName(String fileName) {
            String name = IdUtils.generateId(fileName, "-", true, 100);
            name = name.replace("'", "");
            name = name.replace("(", "");
            name = name.replace(")", "");
            name = name.replace("+", "");
            return name;
        }

        public void dispose() {
            try {
                CoreInstance.getInstance().close(session);
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

}
