package org.nuxeo.ecm.platform.importer.base;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.importer.factories.DefaultDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.factories.ImporterDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.ecm.platform.importer.threading.DefaultMultiThreadingPolicy;
import org.nuxeo.ecm.platform.importer.threading.ImporterThreadingPolicy;
import org.nuxeo.runtime.api.Framework;

public class GenericMultiThreadedImporter implements Runnable {

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
            String importWritePath, Integer batchSize, Integer nbThreads,
            ImporterLogger log) throws Exception {

        importSource = sourceNode;
        this.importWritePath = importWritePath;
        this.batchSize = batchSize;
        this.nbThreads = nbThreads;
        this.log = log;
    }

    protected CoreSession getCoreSession() throws Exception {
        if (this.session == null) {
            RepositoryManager rm = Framework
                    .getService(RepositoryManager.class);
            Repository repo = rm.getDefaultRepository();
            session = repo.open();
        }
        return session;
    }

    public void run() {
        LoginContext lc = null;
        try {
            lc = Framework.login();
            doRun();
        } catch (Exception e) {
            log.error("Task exec failed", e);
        } finally {
            if (lc != null) {
                try {
                    lc.logout();
                } catch (LoginException e) {
                    log.error("Error during logout", e);
                }
            }
        }
    }

    protected GenericThreadedImportTask initRootTask(CoreSession session,
            SourceNode importSource, DocumentModel targetContainer, ImporterLogger log,
            Integer batchSize) throws Exception {
        GenericThreadedImportTask rootImportTask = new GenericThreadedImportTask(session,
                importSource, targetContainer, log, batchSize, getFactory(), getThreadPolicy());
        return rootImportTask;
    }

    public void doRun() throws Exception {

        targetContainer = getCoreSession().getDocument(
                new PathRef(importWritePath));

        nbCreatedDocsByThreads = new ConcurrentHashMap<String, Long>();

        importTP = new ThreadPoolExecutor(nbThreads, nbThreads, 500L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(100));

        GenericThreadedImportTask rootImportTask = initRootTask(getCoreSession(),
                importSource, targetContainer, log, batchSize);

        rootImportTask.setRootTask();
        long t0 = System.currentTimeMillis();
        importTP.execute(rootImportTask);
        Thread.sleep(200);
        int activeTasks = importTP.getActiveCount();
        int oldActiveTasks = 0;
        while (activeTasks > 0) {
            Thread.sleep(200);
            activeTasks = importTP.getActiveCount();
            if (oldActiveTasks != activeTasks) {
                oldActiveTasks = activeTasks;
                log
                        .debug("currently " + activeTasks
                                + " active import Threads");
                long inbCreatedDocs = getCreatedDocsCounter();
                log.info(inbCreatedDocs + " docs created");
                long ti = System.currentTimeMillis();
                log.info(1000 * ((float) (inbCreatedDocs) / (ti - t0))
                        + " docs/s");
            }
        }
        log.info("All Threads terminated");
        long t1 = System.currentTimeMillis();
        long nbCreatedDocs = getCreatedDocsCounter();
        log.info(nbCreatedDocs + " docs created");
        log.info(1000 * ((float) (nbCreatedDocs) / (t1 - t0)) + " docs/s");
        for (String k : nbCreatedDocsByThreads.keySet()) {
            log.info(k + " --> " + nbCreatedDocsByThreads.get(k));
        }
    }



    public ImporterThreadingPolicy getThreadPolicy() {
        if (threadPolicy==null) {
             threadPolicy = new DefaultMultiThreadingPolicy();
        }
        return threadPolicy;
    }

    public void setThreadPolicy(ImporterThreadingPolicy threadPolicy) {
        this.threadPolicy = threadPolicy;
    }

    public ImporterDocumentModelFactory getFactory() {
        if (factory==null) {
            factory = new DefaultDocumentModelFactory();
        }
        return factory;
    }

    public void setFactory(ImporterDocumentModelFactory factory) {
        this.factory = factory;
    }

}
