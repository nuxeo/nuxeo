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

package org.nuxeo.ecm.platform.importer.executor;

import org.apache.commons.logging.Log;
import org.nuxeo.ecm.platform.importer.base.ImporterRunner;
import org.nuxeo.ecm.platform.importer.factories.DefaultDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.factories.ImporterDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.log.BasicLogger;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.threading.DefaultMultiThreadingPolicy;
import org.nuxeo.ecm.platform.importer.threading.ImporterThreadingPolicy;

/**
 *
 * base class for importers
 *
 * @author Thierry Delprat
 *
 */
public abstract class AbstractImporterExecutor implements ImporterExecutor {

    protected abstract Log getJavaLogger();

    protected static ImporterLogger log;

    protected Thread executorMainThread;

    protected ImporterRunner runner;

    protected ImporterThreadingPolicy threadPolicy;

    protected ImporterDocumentModelFactory factory;

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.importer.executor.ImporterExecutor#getLogger()
     */
    @Override
    public ImporterLogger getLogger() {
        if (log == null) {
            log = new BasicLogger(getJavaLogger());
        }
        return log;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.importer.executor.ImporterExecutor#getStatus()
     */
    @Override
    public String getStatus() {
        if (isRunning()) {
            return "Running";
        } else {
            return "Not Running";
        }
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.importer.executor.ImporterExecutor#isRunning()
     */
    @Override
    public Boolean isRunning() {
        if (executorMainThread == null) {
            return false;
        } else {
            return executorMainThread.isAlive();
        }
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.importer.executor.ImporterExecutor#kill()
     */
    @Override
    public String kill() {
        if (executorMainThread != null) {
            runner.stopImportProcrocess();
            executorMainThread.interrupt();
            return "Importer killed";
        }
        return "Importer is not running";
    }

    protected void startTask(ImporterRunner runner, boolean interactive) {
        executorMainThread = new Thread(runner);
        executorMainThread.setName("ImporterExecutorMainThread");
        if (interactive) {
            executorMainThread.run();
        } else {
            executorMainThread.start();
        }
    }

    protected String doRun(ImporterRunner runner, Boolean interactive)
            throws Exception {
        if (isRunning()) {
            throw new Exception("Task is already running");
        }
        if (interactive == null) {
            interactive = false;
        }
        startTask(runner, interactive);

        if (interactive) {
            return "Task compeleted";
        } else {
            return "Started";
        }
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.importer.executor.ImporterExecutor#getThreadPolicy()
     */
    @Override
    public ImporterThreadingPolicy getThreadPolicy() {
        if (threadPolicy == null) {
            threadPolicy = new DefaultMultiThreadingPolicy();
        }
        return threadPolicy;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.importer.executor.ImporterExecutor#setThreadPolicy(org.nuxeo.ecm.platform.importer.threading.ImporterThreadingPolicy)
     */
    @Override
    public void setThreadPolicy(ImporterThreadingPolicy threadPolicy) {
        this.threadPolicy = threadPolicy;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.importer.executor.ImporterExecutor#getFactory()
     */
    @Override
    public ImporterDocumentModelFactory getFactory() {
        if (factory == null) {
            factory = new DefaultDocumentModelFactory();
        }
        return factory;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.importer.executor.ImporterExecutor#setFactory(org.nuxeo.ecm.platform.importer.factories.ImporterDocumentModelFactory)
     */
    @Override
    public void setFactory(ImporterDocumentModelFactory factory) {
        this.factory = factory;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.importer.executor.ImporterExecutor#run(org.nuxeo.ecm.platform.importer.base.ImporterRunner, java.lang.Boolean)
     */
    @Override
    public String run(ImporterRunner runner, Boolean interactive)
            throws Exception {
        return doRun(runner, interactive);
    }
}
