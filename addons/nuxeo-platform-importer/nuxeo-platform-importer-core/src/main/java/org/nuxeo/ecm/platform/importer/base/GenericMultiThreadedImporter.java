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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.importer.factories.DefaultDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.factories.ImporterDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.filter.ImportingDocumentFilter;
import org.nuxeo.ecm.platform.importer.filter.ImporterFilter;
import org.nuxeo.ecm.platform.importer.listener.ImporterListener;
import org.nuxeo.ecm.platform.importer.listener.JobHistoryListener;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.log.PerfLogger;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.ecm.platform.importer.threading.DefaultMultiThreadingPolicy;
import org.nuxeo.ecm.platform.importer.threading.ImporterThreadingPolicy;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * Generic importer
 *
 * @author Thierry Delprat
 *
 */
public class GenericMultiThreadedImporter implements ImporterRunner {

    protected static ThreadPoolExecutor importTP;

    protected static Map<String, Long> nbCreatedDocsByThreads = new ConcurrentHashMap<String, Long>();

    protected ImporterThreadingPolicy threadPolicy;

    protected ImporterDocumentModelFactory factory;

    protected SourceNode importSource;

    protected DocumentModel targetContainer;

    protected Integer batchSize = 50;

    protected Integer nbThreads = 5;

    protected ImporterLogger log;

    protected CoreSession session;

    protected String importWritePath;

    protected Boolean skipRootContainerCreation = false;

    protected String jobName;

    protected boolean enablePerfLogging = true;

    protected List<ImporterFilter> filters = new ArrayList<ImporterFilter>();

    protected List<ImporterListener> listeners = new ArrayList<ImporterListener>();

    protected List<ImportingDocumentFilter> importingDocumentFilters = new ArrayList<ImportingDocumentFilter>();

    public static ThreadPoolExecutor getExecutor() {
        return importTP;
    }

    public static synchronized void addCreatedDoc(String taskId, long nbDocs) {
        String tid = Thread.currentThread().getName();
        nbCreatedDocsByThreads.put(tid + "-" + taskId, nbDocs);
    }

    public static synchronized long getCreatedDocsCounter() {
        long counter = 0;
        for (String tid : nbCreatedDocsByThreads.keySet()) {
            Long tCounter = nbCreatedDocsByThreads.get(tid);
            if (tCounter != null) {
                counter += tCounter;
            }
        }
        return counter;
    }

    public GenericMultiThreadedImporter(SourceNode sourceNode,
            String importWritePath, Boolean skipRootContainerCreation,
            Integer batchSize, Integer nbThreads, ImporterLogger log)
            throws Exception {
        importSource = sourceNode;
        this.importWritePath = importWritePath;
        this.log = log;
        if (batchSize != null) {
            this.batchSize = batchSize;
        }
        if (nbThreads != null) {
            this.nbThreads = nbThreads;
        }
        if (skipRootContainerCreation != null) {
            this.skipRootContainerCreation = skipRootContainerCreation;
        }
    }

    public GenericMultiThreadedImporter(SourceNode sourceNode,
            String importWritePath, Integer batchSize, Integer nbThreads,
            ImporterLogger log) throws Exception {
        this(sourceNode, importWritePath, false, batchSize, nbThreads, log);
    }

    public GenericMultiThreadedImporter(SourceNode sourceNode,
            String importWritePath, Boolean skipRootContainerCreation,
            Integer batchSize, Integer nbThreads, String jobName,
            ImporterLogger log) throws Exception {

        this(sourceNode, importWritePath, skipRootContainerCreation, batchSize,
                nbThreads, log);
        this.jobName = jobName;
        if (jobName != null) {
            listeners.add(new JobHistoryListener(jobName));
        }
    }

    public GenericMultiThreadedImporter(SourceNode sourceNode,
            String importWritePath, Integer batchSize, Integer nbThreads,
            String jobName, ImporterLogger log) throws Exception {
        this(sourceNode, importWritePath, false, batchSize, nbThreads, jobName,
                log);
    }

    public GenericMultiThreadedImporter(
            ImporterRunnerConfiguration configuration) throws Exception {
        this(configuration.sourceNode, configuration.importWritePath,
                configuration.skipRootContainerCreation,
                configuration.batchSize, configuration.nbThreads,
                configuration.jobName, configuration.log);
    }

    public void addFilter(ImporterFilter filter) {
        log.debug(String.format(
                "Filter with %s, was added on the importer with the hash code %s. The source node name is %s",
                filter.toString(), this.hashCode(), importSource.getName()));
        filters.add(filter);
    }

    public void addListeners(ImporterListener... listeners) {
        addListeners(Arrays.asList(listeners));
    }

    public void addListeners(Collection<ImporterListener> listeners) {
        this.listeners.addAll(listeners);
    }

    public void addImportingDocumentFilters(ImportingDocumentFilter... importingDocumentFilters) {
        addImportingDocumentFilters(Arrays.asList(importingDocumentFilters));
    }

    public void addImportingDocumentFilters(Collection<ImportingDocumentFilter> importingDocumentFilters) {
        this.importingDocumentFilters.addAll(importingDocumentFilters);
    }

    protected CoreSession getCoreSession() throws Exception {
        if (this.session == null) {
            RepositoryManager rm = Framework.getService(RepositoryManager.class);
            Repository repo = rm.getDefaultRepository();
            session = repo.open();
        }
        return session;
    }

    public void run() {
        LoginContext lc = null;
        Exception finalException = null;
        try {
            lc = Framework.login();
            for (ImporterFilter filter : filters) {
                log.debug(String.format(
                        "Running filter with %s, on the importer with the hash code %s. The source node name is %s",
                        filter.toString(), this.hashCode(),
                        importSource.getName()));
                filter.handleBeforeImport();
            }
            if (filters.size() == 0) {
                log.debug(String.format(
                        "No filters are registered on the importer with hash code %s, while importing the source node with name ",
                        this.hashCode(), importSource.getName()));
            }
            doRun();
        } catch (Exception e) {
            log.error("Task exec failed", e);
            finalException = e;
        } finally {
            for (ImporterFilter filter : filters) {
                filter.handleAfterImport(finalException);
            }
            if (session != null) {
                CoreInstance.getInstance().close(session);
                session = null;
            }
            if (lc != null) {
                try {
                    lc.logout();
                } catch (LoginException e) {
                    log.error("Error during logout", e);
                }
            }
        }
    }

    protected GenericThreadedImportTask initRootTask(SourceNode importSource,
            DocumentModel targetContainer, boolean skipRootContainerCreation,
            ImporterLogger log, Integer batchSize, String jobName)
            throws Exception {
        GenericThreadedImportTask rootImportTask = new GenericThreadedImportTask(
                null, importSource, targetContainer, skipRootContainerCreation,
                log, batchSize, getFactory(), getThreadPolicy(), jobName);
        rootImportTask.addListeners(listeners);
        rootImportTask.addImportingDocumentFilters(importingDocumentFilters);
        return rootImportTask;
    }

    protected void doRun() throws Exception {

        targetContainer = getTargetContainer();

        nbCreatedDocsByThreads = new ConcurrentHashMap<String, Long>();

        importTP = new ThreadPoolExecutor(nbThreads, nbThreads, 500L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(100));

        GenericThreadedImportTask rootImportTask = initRootTask(importSource,
                targetContainer, skipRootContainerCreation, log, batchSize,
                jobName);

        rootImportTask.setRootTask();
        long t0 = System.currentTimeMillis();

        notifyBeforeImport();

        importTP.execute(rootImportTask);
        Thread.sleep(200);
        int activeTasks = importTP.getActiveCount();
        int oldActiveTasks = 0;
        long lastLogProgressTime = System.currentTimeMillis();
        long lastCreatedDocCounter = 0;

        String[] headers = { "nbDocs", "average", "imediate" };
        PerfLogger perfLogger = new PerfLogger(headers);
        while (activeTasks > 0) {
            Thread.sleep(500);
            activeTasks = importTP.getActiveCount();
            boolean logProgress = false;
            if (oldActiveTasks != activeTasks) {
                oldActiveTasks = activeTasks;
                log.debug("currently " + activeTasks + " active import Threads");
                logProgress = true;

            }
            long ti = System.currentTimeMillis();
            if (ti - lastLogProgressTime > 5000) {
                logProgress = true;
            }
            if (logProgress) {
                long inbCreatedDocs = getCreatedDocsCounter();
                long deltaT = ti - lastLogProgressTime;
                double averageSpeed = 1000 * ((float) (inbCreatedDocs) / (ti - t0));
                double imediateSpeed = averageSpeed;
                if (deltaT > 0) {
                    imediateSpeed = 1000 * ((float) (inbCreatedDocs - lastCreatedDocCounter) / (deltaT));
                }
                log.info(inbCreatedDocs + " docs created");
                log.info("average speed = " + averageSpeed + " docs/s");
                log.info("immediate speed = " + imediateSpeed + " docs/s");

                if (enablePerfLogging) {
                    Double[] perfData = { new Double(inbCreatedDocs),
                            averageSpeed, imediateSpeed };
                    perfLogger.log(perfData);
                }

                lastLogProgressTime = ti;
                lastCreatedDocCounter = inbCreatedDocs;
            }
        }
        log.info("All Threads terminated");
        perfLogger.release();

        notifyAfterImport();

        long t1 = System.currentTimeMillis();
        long nbCreatedDocs = getCreatedDocsCounter();
        log.info(nbCreatedDocs + " docs created");
        log.info(1000 * ((float) (nbCreatedDocs) / (t1 - t0)) + " docs/s");
        for (String k : nbCreatedDocsByThreads.keySet()) {
            log.info(k + " --> " + nbCreatedDocsByThreads.get(k));
        }
    }

    protected DocumentModel getTargetContainer() throws Exception {
        if (targetContainer == null) {
            targetContainer = createTargetContainer();
        }
        return targetContainer;
    }

    /**
     * Creates the target container where the import will be done. Can be
     * overridden in subclasses.
     *
     * @return
     * @throws Exception
     */
    protected DocumentModel createTargetContainer() throws Exception {
        return getCoreSession().getDocument(new PathRef(importWritePath));
    }

    public ImporterThreadingPolicy getThreadPolicy() {
        if (threadPolicy == null) {
            threadPolicy = new DefaultMultiThreadingPolicy();
        }
        return threadPolicy;
    }

    public void setThreadPolicy(ImporterThreadingPolicy threadPolicy) {
        this.threadPolicy = threadPolicy;
    }

    public ImporterDocumentModelFactory getFactory() {
        if (factory == null) {
            factory = new DefaultDocumentModelFactory();
        }
        return factory;
    }

    public void setFactory(ImporterDocumentModelFactory factory) {
        this.factory = factory;
    }

    public void setEnablePerfLogging(boolean enablePerfLogging) {
        this.enablePerfLogging = enablePerfLogging;
    }

    public void stopImportProcrocess() {
        if (importTP != null && !importTP.isTerminated()
                && !importTP.isTerminating()) {
            importTP.shutdownNow();
        }
    }

    protected void notifyBeforeImport() throws Exception {
        for (ImporterListener listener : listeners) {
            listener.beforeImport();
        }
    }

    protected void notifyAfterImport() throws Exception {
        for (ImporterListener listener : listeners) {
            listener.afterImport();
        }
    }

}
