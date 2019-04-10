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

package org.nuxeo.ecm.platform.importer.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.importer.factories.ImporterDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.filter.ImportingDocumentFilter;
import org.nuxeo.ecm.platform.importer.listener.ImporterListener;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.ecm.platform.importer.threading.ImporterThreadingPolicy;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * Generic importer task
 *
 * @author Thierry Delprat
 *
 */
class GenericThreadedImportTask implements Runnable {

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

    protected TxHelper txHelper = new TxHelper();

    protected static final int TX_TIMEOUT = 600;

    protected ImporterThreadingPolicy threadPolicy;

    protected ImporterDocumentModelFactory factory;

    protected String jobName;

    protected List<ImporterListener> listeners = new ArrayList<ImporterListener>();

    protected List<ImportingDocumentFilter> importingDocumentFilters = new ArrayList<ImportingDocumentFilter>();

    private static synchronized int getNextTaskId() {
        taskCounter += 1;
        return taskCounter;
    }

    protected ImporterLogger rsLogger = null;

    public GenericThreadedImportTask(CoreSession session,
            SourceNode rootSource, DocumentModel rootDoc,
            boolean skipContainerCreation, ImporterLogger rsLogger,
            int batchSize, ImporterDocumentModelFactory factory,
            ImporterThreadingPolicy threadPolicy) throws Exception {
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

    public GenericThreadedImportTask(CoreSession session,
            SourceNode rootSource, DocumentModel rootDoc,
            boolean skipContainerCreation, ImporterLogger rsLogger,
            int batchSize, ImporterDocumentModelFactory factory,
            ImporterThreadingPolicy threadPolicy, String jobName)
            throws Exception {
        this(session, rootSource, rootDoc, skipContainerCreation, rsLogger,
                batchSize, factory, threadPolicy);
        this.jobName = jobName;
    }

    protected CoreSession getCoreSession() throws Exception {
        if (this.session == null) {
            RepositoryManager rm = Framework.getService(RepositoryManager.class);
            Repository repo = rm.getDefaultRepository();
            session = repo.open();
        }
        return session;
    }

    protected void commit() throws Exception {
        commit(false);
    }

    protected void commit(boolean force) throws Exception {
        uploadedFiles++;
        if (uploadedFiles % 10 == 0) {
            GenericMultiThreadedImporter.addCreatedDoc(taskId, uploadedFiles);
        }

        if (uploadedFiles % batchSize == 0 || force) {
            fslog("Comiting Core Session after " + uploadedFiles + " files",
                    true);
            getCoreSession().save();
            txHelper.commitOrRollbackTransaction();
            txHelper.beginNewTransaction(TX_TIMEOUT);
        }
    }

    protected DocumentModel doCreateFolderishNode(DocumentModel parent,
            SourceNode node) throws Exception {
        if (!shouldImportDocument(node)) {
            return null;
        }

        DocumentModel folder = getFactory().createFolderishNode(
                getCoreSession(), parent, node);
        if (folder != null) {
            String parentPath = (parent == null) ? "null"
                    : parent.getPathAsString();
            fslog("Created Folder " + folder.getName() + " at " + parentPath,
                    true);
        }
        // save session if needed
        commit();
        return folder;

    }

    protected DocumentModel doCreateLeafNode(DocumentModel parent,
            SourceNode node) throws Exception {
        if (!shouldImportDocument(node)) {
            return null;
        }

        DocumentModel leaf = getFactory().createLeafNode(getCoreSession(),
                parent, node);
        if (leaf != null && node.getBlobHolder() != null) {
            long fileSize = node.getBlobHolder().getBlob().getLength();
            String fileName = node.getBlobHolder().getBlob().getFilename();
            if (fileSize > 0) {
                long kbSize = fileSize / 1024;
                String parentPath = (parent == null) ? "null"
                        : parent.getPathAsString();
                fslog("Created doc " + leaf.getName() + " at " + parentPath
                        + " with file " + fileName + " of size " + kbSize
                        + "KB", true);
            }

            uploadedKO += fileSize;
        }
        // save session if needed
        commit();
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

    protected GenericThreadedImportTask createNewTask(DocumentModel parent,
            SourceNode node, ImporterLogger log, Integer batchSize)
            throws Exception {
        GenericThreadedImportTask newTask = new GenericThreadedImportTask(null, node, parent,
                skipContainerCreation, log, batchSize, factory, threadPolicy);
        newTask.addListeners(listeners);
        newTask.addImportingDocumentFilters(importingDocumentFilters);
        return newTask;
    }

    protected GenericThreadedImportTask createNewTaskIfNeeded(
            DocumentModel parent, SourceNode node) {
        if (isRootTask) {
            isRootTask = false; // don't fork Root thread on first folder
            return null;
        }
        int scheduledTasks = GenericMultiThreadedImporter.getExecutor().getQueue().size();
        boolean createTask = getThreadPolicy().needToCreateThreadAfterNewFolderishNode(
                parent, node, uploadedFiles, batchSize, scheduledTasks);

        if (createTask) {
            GenericThreadedImportTask newTask;
            try {
                newTask = createNewTask(parent, node, rsLogger, batchSize);
            } catch (Exception e) {
                log.error("Error while starting new thread", e);
                return null;
            }
            newTask.setBatchSize(getBatchSize());
            newTask.setSkipContainerCreation(true);
            return newTask;
        } else {
            return null;
        }
    }

    protected void recursiveCreateDocumentFromNode(DocumentModel parent,
            SourceNode node) throws Exception {

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

            List<SourceNode> nodes = node.getChildren();
            if (nodes != null && nodes.size() > 0) {
                // get a new TaskImporter if available to start
                // processing the sub-tree
                GenericThreadedImportTask task = null;
                if (!newThread) {
                    task = createNewTaskIfNeeded(folder, node);
                }
                if (task != null) {
                    // force comit before starting new thread
                    commit(true);
                    GenericMultiThreadedImporter.getExecutor().execute(task);
                } else {
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

    public synchronized void run() {
        txHelper.beginNewTransaction(TX_TIMEOUT);
        synchronized (this) {
            if (isRunning) {
                throw new IllegalStateException("Task already running");
            }
            isRunning = true;
            // versions have no path, target document can be null
            if (rootSource == null) {
                isRunning = false;
                throw new IllegalArgumentException(
                        "source node must be specified");
            }
        }
        LoginContext lc = null;
        try {
            lc = Framework.login();
            recursiveCreateDocumentFromNode(rootDoc, rootSource);
            getCoreSession().save();
            GenericMultiThreadedImporter.addCreatedDoc(taskId, uploadedFiles);
            txHelper.commitOrRollbackTransaction();
        } catch (Exception e) {
            try {
                notifyImportError();
            } catch (Exception e1) {
                log.error("Error during import", e1);
            }
            log.error("Error during import", e);
        } finally {
            if (session != null) {
                CoreInstance.getInstance().close(session);
                session = null;
            }
            if (lc != null) {
                try {
                    lc.logout();
                } catch (LoginException e) {
                    log.error("Error while loging out!", e);
                }
            }
            synchronized (this) {
                isRunning = false;
            }
        }
    }

    public void dispose() {
        try {
            if (session != null) {
                CoreInstance.getInstance().close(session);
                session = null;
            }
        } catch (Exception e) {
            e.printStackTrace();// TODO
        }
    }

    // This should be done with log4j but I did not find a way to configure it
    // the way I wanted ...
    private void fslog(String msg, boolean debug) {
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

    protected void notifyImportError() throws Exception {
        for (ImporterListener listener : listeners) {
            listener.importError();
        }
    }

}
