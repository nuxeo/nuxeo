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

import static org.nuxeo.common.concurrent.ThreadFactories.newThreadFactory;

import org.javasimon.SimonManager;
import org.javasimon.Stopwatch;
import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.importer.factories.DefaultDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.factories.ImporterDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.filter.ImporterFilter;
import org.nuxeo.ecm.platform.importer.filter.ImportingDocumentFilter;
import org.nuxeo.ecm.platform.importer.listener.ImporterListener;
import org.nuxeo.ecm.platform.importer.listener.JobHistoryListener;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.log.PerfLogger;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.ecm.platform.importer.threading.DefaultMultiThreadingPolicy;
import org.nuxeo.ecm.platform.importer.threading.ImporterThreadingPolicy;
import org.nuxeo.runtime.transaction.TransactionHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generic importer
 *
 * @author Thierry Delprat
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

    protected Integer transactionTimeout = 0;

    protected ImporterLogger log;

    protected CoreSession session;

    protected String importWritePath;

    protected Boolean skipRootContainerCreation = false;

    protected String jobName;

    protected boolean enablePerfLogging = true;

    protected List<ImporterFilter> filters = new ArrayList<ImporterFilter>();

    protected List<ImporterListener> listeners = new ArrayList<ImporterListener>();

    protected List<ImportingDocumentFilter> importingDocumentFilters = new ArrayList<ImportingDocumentFilter>();

    protected GenericThreadedImportTask rootImportTask;

    protected final static int DEFAULT_QUEUE_SIZE = 10000;

    protected int queueSize = DEFAULT_QUEUE_SIZE;

    protected String repositoryName;

    protected static final String[] PERF_HEADERS = { "nbDocs", "average", "imediate" };

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

    public GenericMultiThreadedImporter(SourceNode sourceNode, String importWritePath,
            Boolean skipRootContainerCreation, Integer batchSize, Integer nbThreads, ImporterLogger log, int queueSize) {
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

    public GenericMultiThreadedImporter(SourceNode sourceNode, String importWritePath,
            Boolean skipRootContainerCreation, Integer batchSize, Integer nbThreads, ImporterLogger log) {
        this(sourceNode, importWritePath, skipRootContainerCreation, batchSize, nbThreads, log, DEFAULT_QUEUE_SIZE);
    }

    public GenericMultiThreadedImporter(SourceNode sourceNode, String importWritePath, Integer batchSize,
            Integer nbThreads, ImporterLogger log) {
        this(sourceNode, importWritePath, false, batchSize, nbThreads, log);
    }

    public GenericMultiThreadedImporter(SourceNode sourceNode, String importWritePath,
            Boolean skipRootContainerCreation, Integer batchSize, Integer nbThreads, String jobName, ImporterLogger log) {

        this(sourceNode, importWritePath, skipRootContainerCreation, batchSize, nbThreads, log);
        this.jobName = jobName;
        if (jobName != null) {
            listeners.add(new JobHistoryListener(jobName));
        }
    }

    public GenericMultiThreadedImporter(SourceNode sourceNode, String importWritePath, Integer batchSize,
            Integer nbThreads, String jobName, ImporterLogger log) {
        this(sourceNode, importWritePath, false, batchSize, nbThreads, jobName, log);
    }

    public GenericMultiThreadedImporter(ImporterRunnerConfiguration configuration) {
        this(configuration.sourceNode, configuration.importWritePath, configuration.skipRootContainerCreation,
                configuration.batchSize, configuration.nbThreads, configuration.jobName, configuration.log);
        repositoryName = configuration.repositoryName;
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

    @Override
    public void run() {
        Exception finalException = null;
        if (!TransactionHelper.isTransactionActive()) {
            TransactionHelper.startTransaction();
        }
        try (CloseableCoreSession closeableCoreSession = CoreInstance.openCoreSessionSystem(repositoryName)) {
            session = closeableCoreSession;
            for (ImporterFilter filter : filters) {
                log.debug(String.format(
                        "Running filter with %s, on the importer with the hash code %s. The source node name is %s",
                        filter.toString(), this.hashCode(), importSource.getName()));
                filter.handleBeforeImport();
            }
            if (filters.size() == 0) {
                log.debug(String.format(
                        "No filters are registered on the importer with hash code %s, while importing the source node with name %s",
                        this.hashCode(), importSource.getName()));
            }
            doRun();
        } catch (Exception e) { // deals with interrupt below
            ExceptionUtils.checkInterrupt(e);
            log.error("Task exec failed", e);
            finalException = e;
        } finally {
            for (ImporterFilter filter : filters) {
                filter.handleAfterImport(finalException);
            }
            session = null;
        }
    }

    public void setRootImportTask(GenericThreadedImportTask rootImportTask) {
        this.rootImportTask = rootImportTask;
    }

    protected GenericThreadedImportTask initRootTask(SourceNode importSource, DocumentModel targetContainer,
            boolean skipRootContainerCreation, ImporterLogger log, Integer batchSize, String jobName) {
        if (rootImportTask == null) {
            setRootImportTask(new GenericThreadedImportTask(repositoryName, importSource, targetContainer,
                    skipRootContainerCreation, log, batchSize, getFactory(), getThreadPolicy(), jobName));
        } else {
            rootImportTask.setInputSource(importSource);
            rootImportTask.setTargetFolder(targetContainer);
            rootImportTask.setSkipContainerCreation(skipRootContainerCreation);
            rootImportTask.setRsLogger(log);
            rootImportTask.setFactory(getFactory());
            rootImportTask.setThreadPolicy(getThreadPolicy());
            rootImportTask.setJobName(jobName);
            rootImportTask.setBatchSize(batchSize);
        }
        rootImportTask.addListeners(listeners);
        rootImportTask.addImportingDocumentFilters(importingDocumentFilters);
        rootImportTask.setTransactionTimeout(transactionTimeout);
        return rootImportTask;
    }

    protected void doRun() throws IOException {

        targetContainer = getTargetContainer();

        nbCreatedDocsByThreads = new ConcurrentHashMap<String, Long>();

        importTP = new ThreadPoolExecutor(nbThreads, nbThreads, 500L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(queueSize), newThreadFactory("Nuxeo-Importer"));

        initRootTask(importSource, targetContainer, skipRootContainerCreation, log, batchSize, jobName);

        rootImportTask.setRootTask();
        long t0 = System.currentTimeMillis();

        notifyBeforeImport();

        importTP.execute(rootImportTask);
        sleep(200);
        int activeTasks = importTP.getActiveCount();
        int oldActiveTasks = 0;
        long lastLogProgressTime = System.currentTimeMillis();
        long lastCreatedDocCounter = 0;

        PerfLogger perfLogger = enablePerfLogging ? new PerfLogger(PERF_HEADERS) : null;
        while (activeTasks > 0) {
            sleep(500);
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
                    Double[] perfData = { new Double(inbCreatedDocs), averageSpeed, imediateSpeed };
                    perfLogger.log(perfData);
                }

                lastLogProgressTime = ti;
                lastCreatedDocCounter = inbCreatedDocs;
            }
        }
        stopImportProcrocess();
        log.info("All Threads terminated");
        if (enablePerfLogging) {
            perfLogger.release();
        }
        notifyAfterImport();

        long t1 = System.currentTimeMillis();
        long nbCreatedDocs = getCreatedDocsCounter();
        log.info(nbCreatedDocs + " docs created");
        log.info(1000 * ((float) (nbCreatedDocs) / (t1 - t0)) + " docs/s");
        for (String k : nbCreatedDocsByThreads.keySet()) {
            log.info(k + " --> " + nbCreatedDocsByThreads.get(k));
        }
        Stopwatch stopwatch;
        for (String name : SimonManager.simonNames()) {
            if (name == null || name.isEmpty() || !name.startsWith("org.nuxeo.ecm.platform.importer")) {
                continue;
            }
            stopwatch = SimonManager.getStopwatch(name);
            if (stopwatch.getCounter() > 0) {
                log.info(stopwatch.toString());
            }
        }

    }

    protected static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NuxeoException(e);
        }
    }

    protected DocumentModel getTargetContainer() {
        if (targetContainer == null) {
            targetContainer = createTargetContainer();
        }
        return targetContainer;
    }

    /**
     * Creates the target container where the import will // TODO Auto-generated constructor stub }be done. Can be
     * overridden in subclasses.
     *
     * @return
     */
    protected DocumentModel createTargetContainer() {
        try {
            return session.getDocument(new PathRef(importWritePath));
        } catch (DocumentNotFoundException e) {
            log.error(e.getMessage());
            throw e;
        }
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

    /**
     * @since 5.9.4
     */
    public void setTransactionTimeout(int transactionTimeout) {
        this.transactionTimeout = transactionTimeout;
    }

    public void setEnablePerfLogging(boolean enablePerfLogging) {
        this.enablePerfLogging = enablePerfLogging;
    }

    public void stopImportProcrocess() {
        if (importTP != null && !importTP.isTerminated() && !importTP.isTerminating()) {
            importTP.shutdownNow();
        }
    }

    protected void notifyBeforeImport() {
        for (ImporterListener listener : listeners) {
            listener.beforeImport();
        }
    }

    protected void notifyAfterImport() {
        for (ImporterListener listener : listeners) {
            listener.afterImport();
        }
    }

    /**
     * @since 7.1
     */
    public String getRepositoryName() {
        return repositoryName;
    }

    /**
     * @since 7.1
     */
    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

}
