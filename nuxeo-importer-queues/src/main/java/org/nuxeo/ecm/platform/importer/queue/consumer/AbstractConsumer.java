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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.queue.AbstractTaskRunner;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.runtime.transaction.TransactionHelper;

public abstract class AbstractConsumer extends AbstractTaskRunner implements Consumer {

    protected final int batchSize;

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
        this.batchSize = batchSize;
        this.queue = queue;
        rootRef = root.getRef();
        importStat = new ImportStat();
    }

    @Override
    public void run() {

        started = true;
        startTime = System.currentTimeMillis();
        lastCheckTime = startTime;

        UnrestrictedSessionRunner runner = new UnrestrictedSessionRunner(repositoryName, originatingUsername) {
            @Override
            public void run() throws ClientException {
                while (!mustStop) {
                    try {
                        SourceNode src = queue.poll(1, TimeUnit.SECONDS);
                        if (src != null) {
                            incrementProcessed();
                            process(session, src);
                            commitIfNeeded(session);
                        } else {
                            if (canStop) {
                                commit(session);
                                break;
                            }
                        }
                    } catch (Exception e) {
                        error = e;
                        log.error("Error while running consummer. ", e);
                        throw new ClientException(e);
                    }
                }
            }
        };

        TransactionHelper.startTransaction();
        try {
            runner.runUnrestricted();
        } catch (Exception e) {
            log.error("Error while running consumer.", e);
            TransactionHelper.setTransactionRollbackOnly();
            error = e;
            ExceptionUtils.checkInterrupt(e);
        } finally {
            completed = true;
            started = false;
            try {
                TransactionHelper.commitOrRollbackTransaction();
            } catch (Exception e) {
                log.error("Error while running consumer. Could not commit or rollback transaction", e);
                throw e;
            }
        }
    }

    protected abstract void process(CoreSession session, SourceNode bh) throws Exception;

    protected void commitIfNeeded(CoreSession session) {
        if (nbProcessed % batchSize == 0) {
            commit(session);
            long t = System.currentTimeMillis();
            if (t - lastCheckTime > CHECK_INTERVAL) {
                lastImediatThroughput = 1000 * (nbProcessed - lastCount + 0.0) / (t - lastCheckTime);
                lastCount = nbProcessed;
                lastCheckTime = t;
            }
        }
    }

    protected void commit(CoreSession session) {
        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
    }

    @Override
    public double getImmediateThroughput() {
        return lastImediatThroughput;
    }

    @Override
    public double getThroughput() {
        return 1000 * (nbProcessed + 0.0) / (System.currentTimeMillis() + 1 - startTime);
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
