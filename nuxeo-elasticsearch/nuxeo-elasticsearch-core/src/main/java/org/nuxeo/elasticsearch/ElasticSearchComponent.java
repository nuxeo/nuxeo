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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventProducer;
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
import org.nuxeo.elasticsearch.core.ElasticsearchServiceImpl;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.elasticsearch.work.ChildrenIndexingWorker;
import org.nuxeo.elasticsearch.work.IndexingWorker;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

import static org.nuxeo.elasticsearch.ElasticSearchConstants.ES_ENABLED_PROPERTY;

/**
 * Component used to configure and manage ElasticSearch integration
 *
 */
public class ElasticSearchComponent extends DefaultComponent implements
        ElasticSearchAdmin, ElasticSearchIndexing, ElasticSearchService {

    private static final String EP_REMOTE = "elasticSearchRemote";
    private static final String EP_LOCAL = "elasticSearchLocal";
    private static final String EP_INDEX = "elasticSearchIndex";
    private static final Log log = LogFactory
            .getLog(ElasticSearchComponent.class);
    // temporary hack until we are able to list pending indexing jobs cluster
    // wide
    private final Set<String> pendingWork = Collections
            .synchronizedSet(new HashSet<String>());
    private final Set<String> pendingCommands = Collections
            .synchronizedSet(new HashSet<String>());
    private final Map<String, ElasticSearchIndexConfig> indexConfig = new HashMap<String, ElasticSearchIndexConfig>();
    // indexing command that where received before the index initialization
    private final List<IndexingCommand> stackedCommands = new ArrayList<>();
    private ElasticSearchLocalConfig localConfig;
    private ElasticSearchRemoteConfig remoteConfig;
    private ElasticSearchAdminImpl esa;
    private ElasticSearchIndexingImpl esi;
    private ElasticsearchServiceImpl ess;

    // Nuxeo Component impl ======================================é=============
    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        switch (extensionPoint) {
        case EP_LOCAL:
            ElasticSearchLocalConfig localContrib = (ElasticSearchLocalConfig) contribution;
            if (localContrib.isEnabled()) {
                localConfig = localContrib;
                remoteConfig = null;
                log.info("Registering local embedded configuration: "
                        + localConfig + ", loaded from "
                        + contributor.getName());
            } else if (localConfig != null) {
                log.info("Disabling previous local embedded configuration, deactivated by "
                        + contributor.getName());
                localConfig = null;
            }
            break;
        case EP_REMOTE:
            ElasticSearchRemoteConfig remoteContribution = (ElasticSearchRemoteConfig) contribution;
            if (remoteContribution.isEnabled()) {
                remoteConfig = remoteContribution;
                localConfig = null;
                log.info("Registering remote configuration: " + remoteConfig
                        + ", loaded from " + contributor.getName());
            } else if (remoteConfig != null) {
                log.info("Disabling previous remote configuration, deactivated by "
                        + contributor.getName());
                remoteConfig = null;
            }
            break;
        case EP_INDEX:
            ElasticSearchIndexConfig idx = (ElasticSearchIndexConfig) contribution;
            ElasticSearchIndexConfig previous = indexConfig.get(idx.getName());
            if (idx.isEnabled()) {
                idx.merge(previous);
                indexConfig.put(idx.getName(), idx);
                log.info("Registering index configuration: " + idx
                        + ", loaded from " + contributor.getName());
            } else if (previous != null) {
                log.info("Disabling index configuration: " + previous
                        + ", deactivated by " + contributor.getName());
                indexConfig.remove(idx.getName());
            }
            break;
         default:
             throw new IllegalStateException("Invalid EP: " + extensionPoint);
        }

    }

    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        if (! isElasticsearchEnabled()) {
            log.info("Elasticsearch service is disabled");
            return;
        }
        esa = new ElasticSearchAdminImpl(localConfig, remoteConfig, indexConfig);
        esi = new ElasticSearchIndexingImpl(esa);
        ess = new ElasticsearchServiceImpl(esa);
        processStackedCommands();
    }

    protected boolean isElasticsearchEnabled() {
        return Boolean.parseBoolean(Framework.getProperty(ES_ENABLED_PROPERTY, "true"));
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        if (esa != null) {
            esa.disconnect();
        }
    }

    @Override
    public int getApplicationStartedOrder() {
        RepositoryService component = (RepositoryService) Framework
                .getRuntime()
                .getComponent(
                        "org.nuxeo.ecm.core.repository.RepositoryServiceComponent");
        return component.getApplicationStartedOrder() / 2;
    }

    void processStackedCommands() {
        if (!stackedCommands.isEmpty()) {
            log.info(String.format(
                    "Processing %d indexing commands stacked during startup",
                    stackedCommands.size()));
            boolean txCreated = false;
            if (!TransactionHelper.isTransactionActive()) {
                txCreated = TransactionHelper.startTransaction();
            }
            try {
                for (final IndexingCommand cmd : stackedCommands) {
                    new UnrestrictedSessionRunner(cmd.getRepository()) {
                        @Override
                        public void run() throws ClientException {
                            cmd.refresh(session);
                            esi.indexNow(cmd);
                        }
                    }.runUnrestricted();
                }
            } catch (Exception e) {
                log.error(
                        "Unable to flush pending indexing commands: "
                                + e.getMessage(), e);
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
    public int getPendingDocs() {
        return pendingWork.size();
    }

    @Override
    public int getPendingCommands() {
        return pendingCommands.size()
                + ChildrenIndexingWorker.getRunningWorkers();
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
    public void scheduleIndexing(IndexingCommand cmd) throws ClientException {
        String id = cmd.getDocId();
        if (IndexingCommand.UNKOWN_DOCUMENT_ID.equals(id)) {
            return;
        }
        if (isAlreadyScheduled(cmd)) {
            if (log.isDebugEnabled()) {
                log.debug("Skip indexing for " + cmd.toString()
                        + " since it is already scheduled");
            }
            return;
        }
        pendingCommands.add(cmd.getId());
        pendingWork.add(getWorkKey(cmd));
        if (cmd.isSync()) {
            if (log.isDebugEnabled()) {
                log.debug("Schedule Sync PostCommit indexing request "
                        + cmd.toString());
            }
            schedulePostCommitIndexing(cmd);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Schedule Async indexing request  " + cmd.toString());
            }
            WorkManager wm = Framework.getLocalService(WorkManager.class);
            IndexingWorker idxWork = new IndexingWorker(cmd);
            // will be scheduled after the commit and only if the tx is not
            // rollbacked
            wm.schedule(idxWork, true);
        }
    }

    void schedulePostCommitIndexing(IndexingCommand cmd) throws ClientException {
        try {
            EventProducer evtProducer = Framework
                    .getLocalService(EventProducer.class);
            Event indexingEvent = cmd.asIndexingEvent();
            if (indexingEvent != null) {
                evtProducer.fireEvent(indexingEvent);
            }
        } catch (Exception e) {
            throw ClientException.wrap(e);
        }
    }

    @Override
    public boolean isAlreadyScheduled(IndexingCommand cmd) {
        return pendingCommands.contains(cmd.getId())
                || pendingWork.contains(getWorkKey(cmd));
    }

    @Override
    public void indexNow(IndexingCommand cmd) throws ClientException {
        if (!isReady()) {
            stackedCommands.add(cmd);
            log.debug("Delaying indexing command: Waiting for Index to be initialized.");
            return;
        }
        markCommandInProgress(cmd);
        esi.indexNow(cmd);
    }

    @Override
    public void indexNow(List<IndexingCommand> cmds) throws ClientException {
        if (!isReady()) {
            log.debug("Delaying indexing commands: Waiting for Index to be initialized.");
            stackedCommands.addAll(cmds);
            return;
        }
        markCommandInProgress(cmds);
        esi.indexNow(cmds);
    }

    // ES Search ===============================================================
    @Override
    public DocumentModelList query(NxQueryBuilder queryBuilder)
            throws ClientException {
        return ess.query(queryBuilder);
    }

    @Override
    public EsResult queryAndAggregate(NxQueryBuilder queryBuilder)
            throws ClientException {
        return ess.queryAndAggregate(queryBuilder);
    }

    @Deprecated
    @Override
    public DocumentModelList query(CoreSession session, String nxql, int limit,
            int offset, SortInfo... sortInfos) throws ClientException {
        return ess.query(session, nxql, limit, offset, sortInfos);
    }

    @Deprecated
    @Override
    public DocumentModelList query(CoreSession session,
            QueryBuilder queryBuilder, int limit, int offset,
            SortInfo... sortInfos) throws ClientException {
        return ess.query(session, queryBuilder, limit, offset, sortInfos);
    }

    // misc ====================================================================
    private boolean isReady() {
        return (esa != null) && esa.isReady();
    }

    int markCommandInProgress(List<IndexingCommand> cmds) {
        int ret = 0;
        for (IndexingCommand cmd : cmds) {
            ret += markCommandInProgress(cmd);
        }
        return ret;
    }

    String getWorkKey(IndexingCommand cmd) {
        return cmd.getRepository() + ":" + cmd.getDocId() + ":"
                + cmd.isRecurse();
    }

    int markCommandInProgress(IndexingCommand cmd) {
        pendingWork.remove(getWorkKey(cmd));
        boolean isRemoved = pendingCommands.remove(cmd.getId());
        return isRemoved ? 1 : 0;
    }

}
