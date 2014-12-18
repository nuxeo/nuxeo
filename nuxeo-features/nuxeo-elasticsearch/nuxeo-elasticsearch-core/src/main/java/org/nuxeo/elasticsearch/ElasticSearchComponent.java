/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Tiry
 *     bdelbosc
 */
package org.nuxeo.elasticsearch;

import static org.nuxeo.elasticsearch.ElasticSearchConstants.ES_ENABLED_PROPERTY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.transaction.Transaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.api.EsResult;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.elasticsearch.config.ElasticSearchIndexConfig;
import org.nuxeo.elasticsearch.config.ElasticSearchLocalConfig;
import org.nuxeo.elasticsearch.config.ElasticSearchRemoteConfig;
import org.nuxeo.elasticsearch.core.ElasticSearchAdminImpl;
import org.nuxeo.elasticsearch.core.ElasticSearchIndexingImpl;
import org.nuxeo.elasticsearch.core.ElasticSearchServiceImpl;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.elasticsearch.work.BaseIndexingWorker;
import org.nuxeo.elasticsearch.work.IndexingWorker;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Component used to configure and manage ElasticSearch integration
 */
public class ElasticSearchComponent extends DefaultComponent implements ElasticSearchAdmin, ElasticSearchIndexing,
        ElasticSearchService {

    private static final String EP_REMOTE = "elasticSearchRemote";

    private static final String EP_LOCAL = "elasticSearchLocal";

    private static final String EP_INDEX = "elasticSearchIndex";

    private static final Log log = LogFactory.getLog(ElasticSearchComponent.class);

    // temporary hack until we are able to list pending indexing jobs cluster
    // wide
    private final Set<String> pendingWork = Collections.synchronizedSet(new HashSet<String>());

    private final Set<String> pendingCommands = Collections.synchronizedSet(new HashSet<String>());

    private final Map<String, ElasticSearchIndexConfig> indexConfig = new HashMap<>();

    // indexing command that where received before the index initialization
    private final List<IndexingCommand> stackedCommands = new ArrayList<>();

    private ElasticSearchLocalConfig localConfig;

    private ElasticSearchRemoteConfig remoteConfig;

    private ElasticSearchAdminImpl esa;

    private ElasticSearchIndexingImpl esi;

    private ElasticSearchServiceImpl ess;

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
        esi = new ElasticSearchIndexingImpl(esa);
        ess = new ElasticSearchServiceImpl(esa);
        processStackedCommands();
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
            boolean txCreated = false;
            if (!TransactionHelper.isTransactionActive()) {
                txCreated = TransactionHelper.startTransaction();
            }
            try {
                new UnrestrictedSessionRunner(stackedCommands.get(0).getRepositoryName()) {
                    @Override
                    public void run() throws ClientException {
                        esi.indexNonRecursive(stackedCommands);
                    }
                }.runUnrestricted();
            } catch (ClientException e) {
                log.error("Unable to flush pending indexing commands: " + e.getMessage(), e);
            } finally {
                if (txCreated) {
                    TransactionHelper.commitOrRollbackTransaction();
                }
                stackedCommands.clear();
                log.debug("Done");
            }
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
    public void dropAndInitRepositoryIndex(String repositoryName) {
        esa.dropAndInitRepositoryIndex(repositoryName);
    }

    @Override
    public List<String> getRepositoryNames() {
        return esa.getRepositoryNames();
    }

    @Override
    public int getPendingDocs() {
        return pendingWork.size();
    }

    @Override
    public int getPendingCommands() {
        return pendingCommands.size() + BaseIndexingWorker.getRunningWorkers();
    }

    @Override
    public int getRunningCommands() {
        return esa.getRunningCommands();
    }

    @Override
    public int getTotalCommandProcessed() {
        return esa.getTotalCommandProcessed();
    }

    @Override
    public boolean isIndexingInProgress() {
        return (getRunningCommands() > 0 || getPendingCommands() > 0);
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

    // ES Indexing =============================================================

    @Override
    public boolean isAlreadyScheduled(IndexingCommand cmd) {
        return pendingCommands.contains(cmd.getId()) || pendingWork.contains(cmd.getWorkKey());
    }

    @Override
    public void indexNonRecursive(IndexingCommand cmd) throws ClientException {
        List<IndexingCommand> cmds = new ArrayList<>(1);
        cmds.add(cmd);
        indexNonRecursive(cmds);
    }

    @Override
    public void indexNonRecursive(List<IndexingCommand> cmds) throws ClientException {
        if (!isReady()) {
            if (log.isDebugEnabled()) {
                log.debug("Delaying indexing commands: Waiting for Index to be initialized."
                        + Arrays.toString(cmds.toArray()));
            }
            stackedCommands.addAll(cmds);
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("Process indexing commands: " + Arrays.toString(cmds.toArray()));
        }
        markCommandInProgress(cmds);
        esi.indexNonRecursive(cmds);
    }

    @Override
    public void runIndexingWorker(List<IndexingCommand> cmds) {
        List<IndexingCommand> syncCommands = new ArrayList<>();
        List<IndexingCommand> asyncCommands = new ArrayList<>();
        for (IndexingCommand cmd : cmds) {
            if (isAlreadyScheduled(cmd)) {
                if (log.isDebugEnabled()) {
                    log.debug("Cancel indexing command, because it is already scheduled: " + cmd);
                }
                continue;
            }
            pendingCommands.add(cmd.getId());
            pendingWork.add(cmd.getWorkKey());
            if (cmd.isSync()) {
                syncCommands.add(cmd);
            } else {
                asyncCommands.add(cmd);
            }
        }
        // TODO implement multi repositories
        if (!syncCommands.isEmpty()) {
            String repositoryName = syncCommands.get(0).getRepositoryName();
            Transaction transaction = TransactionHelper.suspendTransaction();
            IndexingWorker idxWork = new IndexingWorker(repositoryName, syncCommands);
            try {
                idxWork.run();
            } finally {
                if (transaction != null) {
                    TransactionHelper.resumeTransaction(transaction);
                }
            }
        }
        if (!asyncCommands.isEmpty()) {
            String repositoryName = asyncCommands.get(0).getRepositoryName();
            IndexingWorker idxWork = new IndexingWorker(repositoryName, asyncCommands);
            WorkManager wm = Framework.getLocalService(WorkManager.class);
            wm.schedule(idxWork, false);
        }
    }

    @Override
    public void runIndexingWorker(IndexingCommand cmd) {
        List<IndexingCommand> cmds = new ArrayList<>(1);
        cmds.add(cmd);
        runIndexingWorker(cmds);
    }

    @Override
    public void runReindexingWorker(String repositoryName, String nxql) {
        esi.runReindexingWorker(repositoryName, nxql);
    }

    // ES Search ===============================================================
    @Override
    public DocumentModelList query(NxQueryBuilder queryBuilder) throws ClientException {
        return ess.query(queryBuilder);
    }

    @Override
    public EsResult queryAndAggregate(NxQueryBuilder queryBuilder) throws ClientException {
        return ess.queryAndAggregate(queryBuilder);
    }

    @Deprecated
    @Override
    public DocumentModelList query(CoreSession session, String nxql, int limit, int offset, SortInfo... sortInfos)
            throws ClientException {
        NxQueryBuilder query = new NxQueryBuilder(session).nxql(nxql).limit(limit).offset(offset).addSort(sortInfos);
        return query(query);
    }

    @Deprecated
    @Override
    public DocumentModelList query(CoreSession session, QueryBuilder queryBuilder, int limit, int offset,
            SortInfo... sortInfos) throws ClientException {
        NxQueryBuilder query = new NxQueryBuilder(session).esQuery(queryBuilder).limit(limit).offset(offset).addSort(
                sortInfos);
        return query(query);
    }

    // misc ====================================================================
    private boolean isReady() {
        return (esa != null) && esa.isReady();
    }

    int markCommandInProgress(List<IndexingCommand> cmds) {
        int ret = 0;
        for (IndexingCommand cmd : cmds) {
            pendingWork.remove(cmd.getWorkKey());
            if (pendingCommands.remove(cmd.getId())) {
                ret += 1;
            }
        }
        return ret;
    }

}
