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
 *     matic
 */
package org.nuxeo.ecm.core.repository.jcr.lock;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;

/**
 * @author matic
 */
@SuppressWarnings("unchecked")
public class RepositorySessionLockStress extends RepositoryOSGITestCase {

    public static final String PREFIX = RepositorySessionLockStress.class.getSimpleName();

    protected static final Log log = LogFactory.getLog(RepositorySessionLockStress.class);

    private final int numberOfDocuments;

    private final boolean isThreadSafe;

    private final int numberOfThreads;

    private final int numberOfLoops;

    protected RepositorySessionLockStress(int numberOfDocuments,
            boolean isThreadSafe, boolean isOperationsDelayed,
            int numberOfThreads, int numberOfLoops) {
        super("noop");
        this.numberOfDocuments = numberOfDocuments;
        this.isThreadSafe = isThreadSafe;
        this.shouldDelay = isOperationsDelayed;
        this.numberOfThreads = numberOfThreads;
        this.numberOfLoops = numberOfLoops;
    }

    public void noop() {
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        openRepository();
        populate();
        startRunners();
    }

    protected void populate() throws ClientException {
        log.trace("about to add " + numberOfDocuments + " documents");
        DocumentModel root = coreSession.getRootDocument();
        String rootPath = root.getPathAsString();
        for (int i = 0; i < numberOfDocuments; i++) {
            DocumentModel template = coreSession.createDocumentModel(rootPath,
                    formatName(PREFIX, i, numberOfDocuments), "Note");
            DocumentModel document = coreSession.createDocument(template);
            coreSession.saveDocument(document);
        }
        coreSession.save();
        log.trace(numberOfDocuments + " documents added");
    }

    public static String LOCK_KEY = "LOCKED";

    protected boolean shouldStop = false;

    protected Random random = new Random();

    protected int nextRandomValue(int roundup) {
        return Math.abs(random.nextInt() % roundup);
    }

    protected CountDownLatch canStart;

    protected CountDownLatch canDispose;

    protected RandomRepositoryOperationFactory operationFactory = new RandomRepositoryOperationFactory(
            random, LockDocumentOperation.class, UnlockDocumentOperation.class);

    protected boolean shouldDelay = true;

    protected int getDelay() {
        return nextRandomValue(100);
    }
    protected void sleepForAWhile() {
        try {
            int delay = getDelay();
            Thread.sleep(delay);
        } catch (InterruptedException error) {
            throw new RuntimeException(error);
        }
    }

    public class OperationRunner implements Runnable {

        OperationRunner(String name) {
            this.name = name;
        }

        final String name;

        CoreSession session;

        DocumentRef rootRef;

        protected void acquireSession() {
            if (!isThreadSafe) {
                session = RepositorySessionLockStress.this.coreSession;
            } else {
                Map<String, Serializable> ctx = new HashMap<String, Serializable>();
                ctx.put("username", "Administrator");
                try {
                    session = CoreInstance.getInstance().open(REPOSITORY_NAME,
                            ctx);
                } catch (ClientException e) {
                    throw new RuntimeException(e);
                }
            }
            try {
                rootRef = session.getRootDocument().getRef();
            } catch (ClientException e) {
                throw new RuntimeException(e);
            }
        }

        protected void disposeSession() {
            if (!isThreadSafe) {
                try {
                    session.disconnect();
                } catch (ClientException e) {
                    throw new RuntimeException(e);
                }
            }
            rootRef = null;
            session = null;
        }

        int loopsCount;

        int succeedCount;

        int failedCount;

        List<Exception> errors = new ArrayList<Exception>();

        protected void operate() {
            String documentName = formatName(PREFIX,
                    nextRandomValue(numberOfDocuments), numberOfDocuments);
            DocumentModel document = null;
            try {
                document = session.getChild(rootRef, documentName);
            } catch (ClientException e) {
                throw new IllegalStateException("Cannot access to document "
                        + documentName, e);
            }
            try {
                operationFactory.getRandomOperation(session, document).operateWith();
                succeedCount += 1;
            } catch (DocumentSecurityException e) { // lock exception
                failedCount += 1;
            } catch (Exception e) { // real errors
                errors.add(e);
            }
        }

        public void run() {
            log.trace(Thread.currentThread().getName() + " starting for "
                    + numberOfLoops);
            try {

                try {
                    canStart.await();
                } catch (InterruptedException error) {
                    throw new RuntimeException(error);
                }
                try {
                    acquireSession();
                    sleepForAWhile();
                    while (loopsCount < numberOfLoops && !shouldStop) {
                        if (shouldDelay) {
                            sleepForAWhile();
                        }
                        try {
                            operate();
                        } finally {
                            try {
                                session.save();
                            } catch (ClientException e) {
                                errors.add(e);
                                try {
                                    session.cancel();
                                } catch (ClientException e1) {
                                    errors.add(e);
                                    break;
                                }
                            } catch (Exception e) {
                                errors.add(e);
                                break;
                            }
                        }
                        loopsCount += 1;
                        if (loopsCount % 100 == 0) {
                            log.trace(formatMessage());
                        }
                    }
                } finally {
                    disposeSession();
                }
            } finally {
                canDispose.countDown();
            }
        }

        public String formatMessage() {
            return name + ":" + loopsCount + ":" + succeedCount + ":"
                    + failedCount + ":" + errors.size();
        }
    }

    protected String formatName(String prefix, int index, int count) {
        return String.format("%s-%04d-%04d", prefix, index + 1, count);
    }

    protected void startRunners() throws InterruptedException {
        canDispose = new CountDownLatch(numberOfThreads);
        canStart = new CountDownLatch(1);
        ThreadGroup group = new ThreadGroup(
                RepositorySessionLockStress.class.getSimpleName());
        OperationRunner[] runners = new OperationRunner[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            OperationRunner runner;
            runners[i] = runner = new OperationRunner(
                    formatName(PREFIX, i, numberOfThreads));
            new Thread(group, runner, runner.name).start();
        }
        canStart.countDown();
        canDispose.await();
        for (int i = 0; i < numberOfThreads; i++) {
            OperationRunner runner = runners[i];
            log.info(runner.formatMessage());
            for (Exception error : runner.errors) {
                log.error(runner.name, error);
            }
        }
    }

    public static void main(String[] args) {
        int documentCount = Integer.parseInt(args[0]);
        boolean isThreadsSafe = Boolean.parseBoolean(args[1]);
        boolean isOperationDelayed = Boolean.parseBoolean(args[2]);
        int numberOfThreads = Integer.parseInt(args[3]);
        int numbersOfOperation = Integer.parseInt(args[4]);
        TestCase test = new RepositorySessionLockStress(documentCount,
                isThreadsSafe, isOperationDelayed, numberOfThreads,
                numbersOfOperation);
        TestRunner.run(test);
    }
}
