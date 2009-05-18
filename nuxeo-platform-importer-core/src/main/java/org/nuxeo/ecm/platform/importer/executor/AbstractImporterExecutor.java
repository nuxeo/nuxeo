package org.nuxeo.ecm.platform.importer.executor;

import org.apache.commons.logging.Log;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.importer.factories.DefaultDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.factories.ImporterDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.log.BasicLogger;
import org.nuxeo.ecm.platform.importer.threading.DefaultMultiThreadingPolicy;
import org.nuxeo.ecm.platform.importer.threading.ImporterThreadingPolicy;

public abstract class AbstractImporterExecutor {

    protected abstract Log getJavaLogger();

    protected static ImporterLogger log;

    protected static Thread executorMainThread;

    protected ImporterThreadingPolicy threadPolicy;

    protected ImporterDocumentModelFactory factory;

    protected ImporterLogger getLogger() {
        if (log == null) {
            log = new BasicLogger(getJavaLogger());
        }
        return log;
    }

    public String getStatus() {
        if (isRunning()) {
            return "Running";
        } else {
            return "Not Running";
        }
    }

    public Boolean isRunning() {
        if (executorMainThread == null) {
            return false;
        } else {
            return executorMainThread.isAlive();
        }
    }

    public void kill() {
        if (executorMainThread != null) {
            executorMainThread.interrupt();
        }
    }



    protected abstract CoreSession getCoreSession();

    protected void startTask(Runnable task, boolean interactive) {
        executorMainThread = new Thread(task);
        if (interactive) {
            executorMainThread.run();
        } else {
            executorMainThread.start();
        }
    }

    protected String doRun(Runnable task, Boolean interactive) throws Exception {
        if (isRunning()) {
            throw new Exception("Task is already running");
        }
        if (interactive == null) {
            interactive = false;
        }
        startTask(task, interactive);

        if (interactive) {
            return "Task compeleted";
        } else {
            return "Started";
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
