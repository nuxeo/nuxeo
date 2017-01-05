/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Tiry
 *     bdelbosc
 */
package org.nuxeo.elasticsearch;

import static org.nuxeo.elasticsearch.ElasticSearchConstants.ES_ENABLED_PROPERTY;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.INDEXING_QUEUE_ID;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.REINDEX_ON_STARTUP_PROPERTY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.transaction.Transaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JsonESDocumentWriter;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.api.EsResult;
import org.nuxeo.elasticsearch.api.EsScrollResult;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.elasticsearch.config.ElasticSearchDocWriterDescriptor;
import org.nuxeo.elasticsearch.config.ElasticSearchIndexConfig;
import org.nuxeo.elasticsearch.config.ElasticSearchLocalConfig;
import org.nuxeo.elasticsearch.config.ElasticSearchRemoteConfig;
import org.nuxeo.elasticsearch.core.ElasticSearchAdminImpl;
import org.nuxeo.elasticsearch.core.ElasticSearchIndexingImpl;
import org.nuxeo.elasticsearch.core.ElasticSearchServiceImpl;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.elasticsearch.work.IndexingWorker;
import org.nuxeo.elasticsearch.work.ScrollingIndexingWorker;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * Component used to configure and manage ElasticSearch integration
 */
public class ElasticSearchComponent extends DefaultComponent implements ElasticSearchAdmin, ElasticSearchIndexing,
        ElasticSearchService {

    private static final Log log = LogFactory.getLog(ElasticSearchComponent.class);

    private static final String EP_REMOTE = "elasticSearchRemote";

    private static final String EP_LOCAL = "elasticSearchLocal";

    private static final String EP_INDEX = "elasticSearchIndex";

    private static final String EP_DOC_WRITER = "elasticSearchDocWriter";

    private static final long REINDEX_TIMEOUT = 20;

    // Indexing commands that where received before the index initialization
    private final List<IndexingCommand> stackedCommands = Collections.synchronizedList(new ArrayList<>());

    private final Map<String, ElasticSearchIndexConfig> indexConfig = new HashMap<>();

    private ElasticSearchLocalConfig localConfig;

    private ElasticSearchRemoteConfig remoteConfig;

    private ElasticSearchAdminImpl esa;

    private ElasticSearchIndexingImpl esi;

    private ElasticSearchServiceImpl ess;

    protected JsonESDocumentWriter jsonESDocumentWriter;

    private ListeningExecutorService waiterExecutorService;

    private final AtomicInteger runIndexingWorkerCount = new AtomicInteger(0);

    // Nuxeo Component impl ======================================é=============
    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        switch (extensionPoint) {
        case EP_LOCAL:
            ElasticSearchLocalConfig localContrib = (ElasticSearchLocalConfig) contribution;
            if (localContrib.isEnabled()) {
                localConfig = localContrib;
                remoteConfig = null;
                log.info("Registering local embedded configuration: " + localConfig + ", loaded from "
                        + contributor.getName());
            } else if (localConfig != null) {
                log.info("Disabling previous local embedded configuration, deactivated by " + contributor.getName());
                localConfig = null;
            }
            break;
        case EP_REMOTE:
            ElasticSearchRemoteConfig remoteContribution = (ElasticSearchRemoteConfig) contribution;
            if (remoteContribution.isEnabled()) {
                remoteConfig = remoteContribution;
                localConfig = null;
                log.info("Registering remote configuration: " + remoteConfig + ", loaded from " + contributor.getName());
            } else if (remoteConfig != null) {
                log.info("Disabling previous remote configuration, deactivated by " + contributor.getName());
                remoteConfig = null;
            }
            break;
        case EP_INDEX:
            ElasticSearchIndexConfig idx = (ElasticSearchIndexConfig) contribution;
            ElasticSearchIndexConfig previous = indexConfig.get(idx.getName());
            if (idx.isEnabled()) {
                idx.merge(previous);
                indexConfig.put(idx.getName(), idx);
                log.info("Registering index configuration: " + idx + ", loaded from " + contributor.getName());
            } else if (previous != null) {
                log.info("Disabling index configuration: " + previous + ", deactivated by " + contributor.getName());
                indexConfig.remove(idx.getName());
            }
            break;
        case EP_DOC_WRITER:
            ElasticSearchDocWriterDescriptor writerDescriptor = (ElasticSearchDocWriterDescriptor) contribution;
            try {
                jsonESDocumentWriter = writerDescriptor.getKlass().newInstance();
            } catch (IllegalAccessException | InstantiationException e) {
                log.error("Can not instantiate jsonESDocumentWriter from " + writerDescriptor.getKlass());
                throw new RuntimeException(e);
            }
            break;
        default:
            throw new IllegalStateException("Invalid EP: " + extensionPoint);
        }
    }

    @Override
    public void applicationStarted(ComponentContext context) {
        if (!isElasticsearchEnabled()) {
            log.info("Elasticsearch service is disabled");
            return;
        }
        esa = new ElasticSearchAdminImpl(localConfig, remoteConfig, indexConfig);
        esi = new ElasticSearchIndexingImpl(esa, jsonESDocumentWriter);
        ess = new ElasticSearchServiceImpl(esa);
        initListenerThreadPool();
        processStackedCommands();
        reindexOnStartup();
    }

    private void reindexOnStartup() {
        boolean reindexOnStartup = Boolean.parseBoolean(Framework.getProperty(REINDEX_ON_STARTUP_PROPERTY, "false"));
        if (!reindexOnStartup) {
            return;
        }
        for (String repositoryName : esa.getInitializedRepositories()) {
            log.warn(String.format("Indexing repository: %s on startup", repositoryName));
            runReindexingWorker(repositoryName, "SELECT ecm:uuid FROM Document");
            try {
                prepareWaitForIndexing().get(REINDEX_TIMEOUT, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                log.error(e.getMessage(), e);
            } catch (TimeoutException e) {
                log.warn(String.format("Indexation of repository %s not finised after %d s, continuing in background",
                        repositoryName, REINDEX_TIMEOUT));
            }
        }
    }

    protected boolean isElasticsearchEnabled() {
        return Boolean.parseBoolean(Framework.getProperty(ES_ENABLED_PROPERTY, "true"));
    }

    @Override
    public void deactivate(ComponentContext context) {
        if (esa != null) {
            esa.disconnect();
        }
    }

    @Override
    public int getApplicationStartedOrder() {
        RepositoryService component = (RepositoryService) Framework.getRuntime().getComponent(
                "org.nuxeo.ecm.core.repository.RepositoryServiceComponent");
        return component.getApplicationStartedOrder() / 2;
    }

    void processStackedCommands() {
        if (!stackedCommands.isEmpty()) {
            log.info(String.format("Processing %d indexing commands stacked during startup", stackedCommands.size()));
            runIndexingWorker(stackedCommands);
            stackedCommands.clear();
            log.debug("Done");
        }
    }

    // Es Admin ================================================================

    @Override
    public Client getClient() {
        return esa.getClient();
    }

    @Override
    public void initIndexes(boolean dropIfExists) {
        esa.initIndexes(dropIfExists);
    }

    @Override
    public void dropAndInitIndex(String indexName) {
        esa.dropAndInitIndex(indexName);
    }

    @Override
    public void dropAndInitRepositoryIndex(String repositoryName) {
        esa.dropAndInitRepositoryIndex(repositoryName);
    }

    @Override
    public List<String> getRepositoryNames() {
        return esa.getRepositoryNames();
    }

    @Override
    public String getIndexNameForRepository(String repositoryName) {
        return esa.getIndexNameForRepository(repositoryName);
    }

    @Override
    public List<String> getIndexNamesForType(String type) {
        return esa.getIndexNamesForType(type);
    }

    @Override
    public String getIndexNameForType(String type) {
        return esa.getIndexNameForType(type);
    }

    @SuppressWarnings("deprecation")
    @Override
    public long getPendingWorkerCount() {
        WorkManager wm = Framework.getLocalService(WorkManager.class);
        // api is deprecated for completed work
        return wm.getQueueSize(INDEXING_QUEUE_ID, Work.State.SCHEDULED);
    }

    @SuppressWarnings("deprecation")
    @Override
    public long getRunningWorkerCount() {
        WorkManager wm = Framework.getLocalService(WorkManager.class);
        // api is deprecated for completed work
        return runIndexingWorkerCount.get() + wm.getQueueSize(INDEXING_QUEUE_ID, Work.State.RUNNING);
    }

    @Override
    public int getTotalCommandProcessed() {
        return esa.getTotalCommandProcessed();
    }

    @Override
    public boolean isEmbedded() {
        return esa.isEmbedded();
    }

    @Override
    public boolean useExternalVersion() {
        return esa.useExternalVersion();
    }

    @Override
    public boolean isIndexingInProgress() {
        return (runIndexingWorkerCount.get() > 0) || (getPendingWorkerCount() > 0) || (getRunningWorkerCount() > 0);
    }

    @Override
    public ListenableFuture<Boolean> prepareWaitForIndexing() {
        return waiterExecutorService.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                WorkManager wm = Framework.getLocalService(WorkManager.class);
                boolean completed = false;
                do {
                    completed = wm.awaitCompletion(INDEXING_QUEUE_ID, 300, TimeUnit.SECONDS);
                } while (!completed);
                return true;
            }
        });
    }

    private static class NamedThreadFactory implements ThreadFactory {
        @SuppressWarnings("NullableProblems")
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "waitForEsIndexing");
        }
    }

    protected void initListenerThreadPool() {
        waiterExecutorService = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool(new NamedThreadFactory()));
    }

    @Override
    public void refresh() {
        esa.refresh();
    }

    @Override
    public void refreshRepositoryIndex(String repositoryName) {
        esa.refreshRepositoryIndex(repositoryName);
    }

    @Override
    public void flush() {
        esa.flush();
    }

    @Override
    public void flushRepositoryIndex(String repositoryName) {
        esa.flushRepositoryIndex(repositoryName);
    }

    @Override
    public void optimize() {
        esa.optimize();
    }

    @Override
    public void optimizeRepositoryIndex(String repositoryName) {
        esa.optimizeRepositoryIndex(repositoryName);
    }

    @Override
    public void optimizeIndex(String indexName) {
        esa.optimizeIndex(indexName);
    }

    // ES Indexing =============================================================

    @Override
    public void indexNonRecursive(IndexingCommand cmd) {
        List<IndexingCommand> cmds = new ArrayList<>(1);
        cmds.add(cmd);
        indexNonRecursive(cmds);
    }

    @Override
    public void indexNonRecursive(List<IndexingCommand> cmds) {
        if (!isReady()) {
            stackCommands(cmds);
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("Process indexing commands: " + Arrays.toString(cmds.toArray()));
        }
        esi.indexNonRecursive(cmds);
    }

    protected void stackCommands(List<IndexingCommand> cmds) {
        if (log.isDebugEnabled()) {
            log.debug("Delaying indexing commands: Waiting for Index to be initialized."
                    + Arrays.toString(cmds.toArray()));
        }
        stackedCommands.addAll(cmds);
    }

    @Override
    public void runIndexingWorker(List<IndexingCommand> cmds) {
        if (!isReady()) {
            stackCommands(cmds);
            return;
        }
        runIndexingWorkerCount.incrementAndGet();
        try {
            dispatchWork(cmds);
        } finally {
            runIndexingWorkerCount.decrementAndGet();
        }
    }

    /**
     * Dispatch jobs between sync and async worker
     */
    protected void dispatchWork(List<IndexingCommand> cmds) {
        Map<String, List<IndexingCommand>> syncCommands = new HashMap<>();
        Map<String, List<IndexingCommand>> asyncCommands = new HashMap<>();
        for (IndexingCommand cmd : cmds) {
            if (cmd.isSync()) {
                List<IndexingCommand> syncCmds = syncCommands.get(cmd.getRepositoryName());
                if (syncCmds == null) {
                    syncCmds = new ArrayList<>();
                }
                syncCmds.add(cmd);
                syncCommands.put(cmd.getRepositoryName(), syncCmds);
            } else {
                List<IndexingCommand> asyncCmds = asyncCommands.get(cmd.getRepositoryName());
                if (asyncCmds == null) {
                    asyncCmds = new ArrayList<>();
                }
                asyncCmds.add(cmd);
                asyncCommands.put(cmd.getRepositoryName(), asyncCmds);
            }
        }
        runIndexingSyncWorker(syncCommands);
        scheduleIndexingAsyncWorker(asyncCommands);
    }

    protected void scheduleIndexingAsyncWorker(Map<String, List<IndexingCommand>> asyncCommands) {
        if (asyncCommands.isEmpty()) {
            return;
        }
        WorkManager wm = Framework.getLocalService(WorkManager.class);
        for (String repositoryName : asyncCommands.keySet()) {
            IndexingWorker idxWork = new IndexingWorker(repositoryName, asyncCommands.get(repositoryName));
            // we are in afterCompletion don't wait for a commit
            wm.schedule(idxWork, false);
        }
    }

    protected void runIndexingSyncWorker(Map<String, List<IndexingCommand>> syncCommands) {
        if (syncCommands.isEmpty()) {
            return;
        }
        Transaction transaction = TransactionHelper.suspendTransaction();
        try {
            for (String repositoryName : syncCommands.keySet()) {
                IndexingWorker idxWork = new IndexingWorker(repositoryName, syncCommands.get(repositoryName));
                idxWork.run();
            }
        } finally {
            if (transaction != null) {
                TransactionHelper.resumeTransaction(transaction);
            }

        }
    }

    @Override
    public void runReindexingWorker(String repositoryName, String nxql) {
        if (nxql == null || nxql.isEmpty()) {
            throw new IllegalArgumentException("Expecting an NXQL query");
        }
        ScrollingIndexingWorker worker = new ScrollingIndexingWorker(repositoryName, nxql);
        WorkManager wm = Framework.getLocalService(WorkManager.class);
        wm.schedule(worker);
    }

    // ES Search ===============================================================
    @Override
    public DocumentModelList query(NxQueryBuilder queryBuilder) {
        return ess.query(queryBuilder);
    }

    @Override
    public EsResult queryAndAggregate(NxQueryBuilder queryBuilder) {
        return ess.queryAndAggregate(queryBuilder);
    }

    @Override
    public EsScrollResult scroll(NxQueryBuilder queryBuilder, long keepAlive) {
        return ess.scroll(queryBuilder, keepAlive);
    }

    @Override
    public EsScrollResult scroll(EsScrollResult scrollResult) {
        return ess.scroll(scrollResult);
    }

    @Override
    public void clearScroll(EsScrollResult scrollResult) {
        ess.clearScroll(scrollResult);
    }

    @Deprecated
    @Override
    public DocumentModelList query(CoreSession session, String nxql, int limit, int offset, SortInfo... sortInfos) {
        NxQueryBuilder query = new NxQueryBuilder(session).nxql(nxql).limit(limit).offset(offset).addSort(sortInfos);
        return query(query);
    }

    @Deprecated
    @Override
    public DocumentModelList query(CoreSession session, QueryBuilder queryBuilder, int limit, int offset,
            SortInfo... sortInfos) {
        NxQueryBuilder query = new NxQueryBuilder(session).esQuery(queryBuilder)
                                                          .limit(limit)
                                                          .offset(offset)
                                                          .addSort(sortInfos);
        return query(query);
    }

    // misc ====================================================================
    private boolean isReady() {
        return (esa != null) && esa.isReady();
    }

}
