/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.importer.queue.consumer;

import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.queue.AbstractTaskRunner;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.runtime.transaction.TransactionHelper;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.nuxeo.runtime.transaction.TransactionHelper.startTransaction;

/**
 * @since 8.3
 */
public abstract class AbstractConsumer extends AbstractTaskRunner implements Consumer {

    protected final Batch batch;

    protected final String repositoryName;

    protected final BlockingQueue<SourceNode> queue;

    protected final DocumentRef rootRef;

    protected long startTime = 0;

    protected long lastCheckTime = 0;

    protected long lastCount = 0;

    protected static final long CHECK_INTERVAL = 2000;

    protected double lastImediatThroughput = 0;

    protected final ImportStat importStat;

    protected String originatingUsername;

    protected ImporterLogger log = null;

    public AbstractConsumer(ImporterLogger log, DocumentModel root, int batchSize, BlockingQueue<SourceNode> queue) {
        this.log = log;
        repositoryName = root.getRepositoryName();
        this.batch = new Batch(batchSize);
        this.queue = queue;
        rootRef = root.getRef();
        importStat = new ImportStat();
        log.info("Create consumer root:" + root.getPathAsString() + " batchSize: " + batchSize);
    }

    @Override
    public void run() {

        started = true;
        startTime = System.currentTimeMillis();
        lastCheckTime = startTime;

        UnrestrictedSessionRunner runner = new UnrestrictedSessionRunner(repositoryName, originatingUsername) {
            @Override
            public void run() {
                log.info("Consumer running");
                SourceNode src;
                while (true) {
                    try {
                        src = queue.poll(1, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        log.error("Interrupted exception received, stopping consumer");
                        break;
                    }
                    if (src == null) {
                        log.debug("Poll timeout, queue size:" + queue.size());
                        if (canStop) {
                            log.info("End of consumer, processed node: " + getNbProcessed());
                            break;
                        }
                        continue;
                    }
                    incrementProcessed();
                    batch.add(src);
                    try {
                        process(session, src);
                    } catch (Exception e) {
                        log.error("Exception while consuming node: " + src.getName(), e);
                        ExceptionUtils.checkInterrupt(e);
                        TransactionHelper.setTransactionRollbackOnly();
                    }
                    commitIfNeededOrReplayBatch(src);
                }
                commitOrReplayBatch();
            }

            private void commitIfNeededOrReplayBatch(SourceNode lastSrc) {
                if (TransactionHelper.isTransactionMarkedRollback()) {
                    log.error("Transaction marked as rollback while processing node: " + lastSrc.getName());
                    rollbackAndReplayBatch(session);
                } else {
                    commitIfNeeded(session);
                }
            }

            private void commitOrReplayBatch() {
                if (TransactionHelper.isTransactionMarkedRollback()) {
                    rollbackAndReplayBatch(session);
                } else {
                    commit(session);
                }
            }

        };

        if (!TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            // This is needed to acquire a session
            startTransaction();
        }
        try {
            runner.runUnrestricted();
        } catch (Exception e) {
            log.error("Error while running consumer.", e);
            drainQueueOnError(e);
            ExceptionUtils.checkInterrupt(e);
            error = e;
        } finally {
            completed = true;
            started = false;
        }
    }

    protected void drainQueueOnError(Exception e) {
        log.error("Drain " + queue.size() + " nodes after error " + e.getMessage());
        if (queue != null && ! queue.isEmpty()) {
            try {
                do {
                    SourceNode src = queue.poll(10, TimeUnit.MILLISECONDS);
                    onSourceNodeException(src, e);
                } while (!queue.isEmpty());
            } catch (InterruptedException ei) {
                log.error("InterruptedException during drain");
            }

        }
    }

    protected abstract void process(CoreSession session, SourceNode bh) throws Exception;

    protected void commitIfNeeded(CoreSession session) {
        if (batch.isFull()) {
            commit(session);
            long t = System.currentTimeMillis();
            if (t - lastCheckTime > CHECK_INTERVAL) {
                lastImediatThroughput = 1000 * (nbProcessed.get() - lastCount + 0.0) / (t - lastCheckTime);
                lastCount = nbProcessed.get();
                lastCheckTime = t;
            }
        }
    }

    protected void commit(CoreSession session) {
        log.debug("Commit batch of " + batch.size() + " nodes");
        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        batch.clear();
        startTransaction();
    }

    protected void rollbackAndReplayBatch(CoreSession session) {
        log.info("Rollback a batch of " + batch.size() + " docs");
        TransactionHelper.setTransactionRollbackOnly();
        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        replayBatch(session);
        batch.clear();
        startTransaction();
    }

    /**
     * Replays the current batch in an isolated Transaction for each Source Node.
     *
     * @param session
     * @throws InterruptedException
     */
    private void replayBatch(CoreSession session) {
        log.error("Replaying batch in isolated transaction");
        for (SourceNode node : batch.getNodes()) {
            boolean success = false;
            startTransaction();
            try {
                process(session, node);
            } catch (Exception e) { // deals with interrupt below
                ExceptionUtils.checkInterrupt(e);
                onSourceNodeException(node, e);
                TransactionHelper.setTransactionRollbackOnly();
            }
            session.save();
            if (TransactionHelper.isTransactionMarkedRollback()) {
                onSourceNodeRollBack(node);
            } else {
                success = true;
            }
            TransactionHelper.commitOrRollbackTransaction();
            if (success) {
                log.debug("Replaying successfully node: " + node.getName());
            } else {
                log.error("Import failure after replay on node: " + node.getName());
            }
        }
    }

    /**
     * Override if you want to do more that logging the error.
     *
     * @param node
     * @param e
     */
    protected void onSourceNodeException(SourceNode node, Exception e) {
        log.error(String.format("   Unable to import node [%s]", node.getName()), e);
    }

    /**
     * Override if you want to do more that logging the error.
     *
     * @param node
     */
    protected void onSourceNodeRollBack(SourceNode node) {
        log.error(String.format("   Rollback while replaying consumer node [%s]", node.getName()));
    }

    @Override
    public double getImmediateThroughput() {
        return lastImediatThroughput;
    }

    @Override
    public double getThroughput() {
        return 1000 * (nbProcessed.get() + 0.0) / (System.currentTimeMillis() + 1 - startTime);
    }

    @Override
    public ImportStat getImportStat() {
        return importStat;
    }

    public String getOriginatingUsername() {
        return originatingUsername;
    }

    public void setOriginatingUsername(String originatingUsername) {
        this.originatingUsername = originatingUsername;
    }

}
