/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.importer.base;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.platform.importer.factories.ImporterDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.filter.ImportingDocumentFilter;
import org.nuxeo.ecm.platform.importer.listener.ImporterListener;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.ecm.platform.importer.threading.ImporterThreadingPolicy;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Generic importer task
 *
 * @author Thierry Delprat
 */
public class GenericThreadedImportTask implements Runnable {

    public static final String DOC_IMPORTED_EVENT = "documentImportedWithPlatformImporter";

    private static final Log log = LogFactory.getLog(GenericThreadedImportTask.class);

    protected static int taskCounter = 0;

    protected boolean isRunning = false;

    protected long uploadedFiles = 0;

    protected long uploadedKO;

    protected int batchSize;

    protected CoreSession session;

    protected DocumentModel rootDoc;

    protected SourceNode rootSource;

    protected Boolean skipContainerCreation = false;

    protected Boolean isRootTask = false;

    protected String taskId = null;

    public static final int TX_TIMEOUT = 600;

    protected int transactionTimeout = TX_TIMEOUT;

    protected ImporterThreadingPolicy threadPolicy;

    protected ImporterDocumentModelFactory factory;

    protected String jobName;

    protected List<ImporterListener> listeners = new ArrayList<ImporterListener>();

    protected List<ImportingDocumentFilter> importingDocumentFilters = new ArrayList<ImportingDocumentFilter>();

    protected String repositoryName;

    private static synchronized int getNextTaskId() {
        taskCounter += 1;
        return taskCounter;
    }

    protected ImporterLogger rsLogger = null;

    protected GenericThreadedImportTask(CoreSession session) {
        this.session = session;
        uploadedFiles = 0;
        taskId = "T" + getNextTaskId();
    }

    protected GenericThreadedImportTask(CoreSession session, SourceNode rootSource, DocumentModel rootDoc,
            boolean skipContainerCreation, ImporterLogger rsLogger, int batchSize,
            ImporterDocumentModelFactory factory, ImporterThreadingPolicy threadPolicy) {
        this.rsLogger = rsLogger;
        this.session = session;
        this.batchSize = batchSize;
        uploadedFiles = 0;
        taskId = "T" + getNextTaskId();
        this.rootSource = rootSource;
        this.rootDoc = rootDoc;
        this.skipContainerCreation = skipContainerCreation;
        this.factory = factory;
        this.threadPolicy = threadPolicy;

        // there are documents without path, like versions
        if (rootSource == null) {
            throw new IllegalArgumentException("source node must be specified");
        }
    }

    public GenericThreadedImportTask(String repositoryName, SourceNode rootSource, DocumentModel rootDoc,
            boolean skipContainerCreation, ImporterLogger rsLogger, int batchSize,
            ImporterDocumentModelFactory factory, ImporterThreadingPolicy threadPolicy, String jobName) {
        this(null, rootSource, rootDoc, skipContainerCreation, rsLogger, batchSize, factory, threadPolicy);
        this.jobName = jobName;
        this.repositoryName = repositoryName;
    }

    protected CoreSession getCoreSession() {
        return session;
    }

    protected void commit() {
        commit(false);
    }

    protected void commit(boolean force) {
        uploadedFiles++;
        if (uploadedFiles % 10 == 0) {
            GenericMultiThreadedImporter.addCreatedDoc(taskId, uploadedFiles);
        }

        if (uploadedFiles % batchSize == 0 || force) {
            Stopwatch stopwatch = SimonManager.getStopwatch("org.nuxeo.ecm.platform.importer.session_save");
            Split split = stopwatch.start();
            fslog("Committing Core Session after " + uploadedFiles + " files", true);
            session.save();
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction(transactionTimeout);
            split.stop();
        }
    }

    protected DocumentModel doCreateFolderishNode(DocumentModel parent, SourceNode node) {
        if (!shouldImportDocument(node)) {
            return null;
        }
        Stopwatch stopwatch = SimonManager.getStopwatch("org.nuxeo.ecm.platform.importer.create_folder");
        Split split = stopwatch.start();
        DocumentModel folder = null;
        try {
            folder = getFactory().createFolderishNode(session, parent, node);
        } catch (IOException e) {
            String errorMsg = "Unable to create folderish document for " + node.getSourcePath() + ":" + e
                    + (e.getCause() != null ? e.getCause() : "");
            fslog(errorMsg, true);
            log.error(errorMsg);
            // Process folderish node creation error and check if the global
            // import task should continue
            boolean shouldImportTaskContinue = getFactory().processFolderishNodeCreationError(session, parent, node);
            if (!shouldImportTaskContinue) {
                throw new NuxeoException(e);
            }
        } finally {
            split.stop();
        }
        if (folder != null) {
            String parentPath = (parent == null) ? "null" : parent.getPathAsString();
            fslog("Created Folder " + folder.getName() + " at " + parentPath, true);

            // save session if needed
            commit();
        }
        return folder;

    }

    protected DocumentModel doCreateLeafNode(DocumentModel parent, SourceNode node) throws IOException {
        if (!shouldImportDocument(node)) {
            return null;
        }
        Stopwatch stopwatch = SimonManager.getStopwatch("org.nuxeo.ecm.platform.importer.create_leaf");
        Split split = stopwatch.start();
        DocumentModel leaf = null;
        try {
            leaf = getFactory().createLeafNode(session, parent, node);
        } catch (IOException e) {
            String errMsg = "Unable to create leaf document for " + node.getSourcePath() + ":" + e
                    + (e.getCause() != null ? e.getCause() : "");
            fslog(errMsg, true);
            log.error(errMsg);
            // Process leaf node creation error and check if the global
            // import task should continue
            boolean shouldImportTaskContinue = getFactory().processLeafNodeCreationError(session, parent, node);
            if (!shouldImportTaskContinue) {
                throw new NuxeoException(e);
            }
        } finally {
            split.stop();
        }
        BlobHolder bh = node.getBlobHolder();
        if (leaf != null && bh != null) {
            Blob blob = bh.getBlob();
            if (blob != null) {
                long fileSize = blob.getLength();
                String fileName = blob.getFilename();
                if (fileSize > 0) {
                    long kbSize = fileSize / 1024;
                    String parentPath = (parent == null) ? "null" : parent.getPathAsString();
                    fslog("Created doc " + leaf.getName() + " at " + parentPath + " with file " + fileName
                            + " of size " + kbSize + "KB", true);
                }
                uploadedKO += fileSize;
            }

            // send an event about the imported document
            EventProducer eventProducer = Framework.getService(EventProducer.class);
            EventContextImpl eventContext = new DocumentEventContext(session, session.getPrincipal(), leaf);
            Event event = eventContext.newEvent(DOC_IMPORTED_EVENT);
            eventProducer.fireEvent(event);

            // save session if needed
            commit();
        }
        return leaf;
    }

    protected boolean shouldImportDocument(SourceNode node) {
        for (ImportingDocumentFilter importingDocumentFilter : importingDocumentFilters) {
            if (!importingDocumentFilter.shouldImportDocument(node)) {
                return false;
            }
        }
        return true;
    }

    protected GenericThreadedImportTask createNewTask(DocumentModel parent, SourceNode node, ImporterLogger log,
            Integer batchSize) {
        GenericThreadedImportTask newTask = new GenericThreadedImportTask(repositoryName, node, parent,
                skipContainerCreation, log, batchSize, factory, threadPolicy, null);
        newTask.addListeners(listeners);
        newTask.addImportingDocumentFilters(importingDocumentFilters);
        return newTask;
    }

    protected GenericThreadedImportTask createNewTaskIfNeeded(DocumentModel parent, SourceNode node) {
        if (isRootTask) {
            isRootTask = false; // don't fork Root thread on first folder
            return null;
        }
        int scheduledTasks = GenericMultiThreadedImporter.getExecutor().getQueue().size();
        boolean createTask = getThreadPolicy().needToCreateThreadAfterNewFolderishNode(parent, node, uploadedFiles,
                batchSize, scheduledTasks);

        if (createTask) {
            GenericThreadedImportTask newTask = createNewTask(parent, node, rsLogger, batchSize);
            newTask.setBatchSize(getBatchSize());
            newTask.setSkipContainerCreation(true);
            newTask.setTransactionTimeout(transactionTimeout);
            return newTask;
        } else {
            return null;
        }
    }

    protected void recursiveCreateDocumentFromNode(DocumentModel parent, SourceNode node) throws IOException {

        if (getFactory().isTargetDocumentModelFolderish(node)) {
            DocumentModel folder;
            Boolean newThread = false;
            if (skipContainerCreation) {
                folder = parent;
                skipContainerCreation = false;
                newThread = true;
            } else {
                folder = doCreateFolderishNode(parent, node);
                if (folder == null) {
                    return;
                }
            }

            // get a new TaskImporter if available to start
            // processing the sub-tree
            GenericThreadedImportTask task = null;
            if (!newThread) {
                task = createNewTaskIfNeeded(folder, node);
            }
            if (task != null) {
                // force comit before starting new thread
                commit(true);
                try {
                    GenericMultiThreadedImporter.getExecutor().execute(task);
                } catch (RejectedExecutionException e) {
                    log.error("Import task rejected", e);
                }

            } else {
                Stopwatch stopwatch = SimonManager.getStopwatch("org.nuxeo.ecm.platform.importer.node_get_children");
                Split split = stopwatch.start();
                List<SourceNode> nodes = node.getChildren();
                split.stop();
                if (nodes != null) {
                    for (SourceNode child : nodes) {
                        recursiveCreateDocumentFromNode(folder, child);
                    }
                }
            }
        } else {
            doCreateLeafNode(parent, node);
        }
    }

    public void setInputSource(SourceNode node) {
        this.rootSource = node;
    }

    public void setTargetFolder(DocumentModel rootDoc) {
        this.rootDoc = rootDoc;
    }

    // TODO isRunning is not yet handled correctly
    public boolean isRunning() {
        synchronized (this) {
            return isRunning;
        }
    }

    @Override
    public synchronized void run() {
        synchronized (this) {
            if (isRunning) {
                throw new IllegalStateException("Task already running");
            }
            isRunning = true;
            // versions have no path, target document can be null
            if (rootSource == null) {
                isRunning = false;
                throw new IllegalArgumentException("source node must be specified");
            }
        }
        TransactionHelper.startTransaction(transactionTimeout);
        boolean completedAbruptly = true;
        try (CloseableCoreSession closeableCoreSession = CoreInstance.openCoreSessionSystem(repositoryName)) {
            session = closeableCoreSession;
            log.info("Starting new import task");
            if (rootDoc != null) {
                // reopen the root to be sure the session is valid
                rootDoc = session.getDocument(rootDoc.getRef());
            }
            recursiveCreateDocumentFromNode(rootDoc, rootSource);
            session.save();
            GenericMultiThreadedImporter.addCreatedDoc(taskId, uploadedFiles);
            completedAbruptly = false;
        } catch (Exception e) { // deals with interrupt below
            log.error("Error during import", e);
            ExceptionUtils.checkInterrupt(e);
            notifyImportError();
        } finally {
            log.info("End of task");
            session = null;
            if (completedAbruptly) {
                TransactionHelper.setTransactionRollbackOnly();
            }
            TransactionHelper.commitOrRollbackTransaction();
            synchronized (this) {
                isRunning = false;
            }
        }
    }

    // This should be done with log4j but I did not find a way to configure it
    // the way I wanted ...
    protected void fslog(String msg, boolean debug) {
        if (debug) {
            rsLogger.debug(msg);
        } else {
            rsLogger.info(msg);
        }
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setSkipContainerCreation(Boolean skipContainerCreation) {
        this.skipContainerCreation = skipContainerCreation;
    }

    public void setRootTask() {
        isRootTask = true;
        taskCounter = 0;
        taskId = "T0";
    }

    protected ImporterThreadingPolicy getThreadPolicy() {
        return threadPolicy;
    }

    protected ImporterDocumentModelFactory getFactory() {
        return factory;
    }

    public void addImportingDocumentFilters(ImportingDocumentFilter... importingDocumentFilters) {
        addImportingDocumentFilters(Arrays.asList(importingDocumentFilters));
    }

    public void addImportingDocumentFilters(Collection<ImportingDocumentFilter> importingDocumentFilters) {
        this.importingDocumentFilters.addAll(importingDocumentFilters);
    }

    public void addListeners(ImporterListener... listeners) {
        addListeners(Arrays.asList(listeners));
    }

    public void addListeners(Collection<ImporterListener> listeners) {
        this.listeners.addAll(listeners);
    }

    public void setTransactionTimeout(int transactionTimeout) {
        this.transactionTimeout = transactionTimeout < 1 ? TX_TIMEOUT : transactionTimeout;
    }

    protected void notifyImportError() {
        for (ImporterListener listener : listeners) {
            listener.importError();
        }
    }

    protected void setRootDoc(DocumentModel rootDoc) {
        this.rootDoc = rootDoc;
    }

    protected void setRootSource(SourceNode rootSource) {
        this.rootSource = rootSource;
    }

    protected void setFactory(ImporterDocumentModelFactory factory) {
        this.factory = factory;
    }

    protected void setRsLogger(ImporterLogger rsLogger) {
        this.rsLogger = rsLogger;
    }

    protected void setThreadPolicy(ImporterThreadingPolicy threadPolicy) {
        this.threadPolicy = threadPolicy;
    }

    protected void setJobName(String jobName) {
        this.jobName = jobName;
    }

}
