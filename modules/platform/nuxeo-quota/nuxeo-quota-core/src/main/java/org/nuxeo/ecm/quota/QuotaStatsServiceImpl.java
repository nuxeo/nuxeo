/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 *     Florent Guillaume
 */
package org.nuxeo.ecm.quota;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelIterator;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.Work.State;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.ecm.quota.size.QuotaAware;
import org.nuxeo.ecm.quota.size.QuotaAwareDocumentFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Default implementation of {@link org.nuxeo.ecm.quota.QuotaStatsService}.
 *
 * @since 5.5
 */
public class QuotaStatsServiceImpl extends DefaultComponent implements QuotaStatsService {

    private static final Log log = LogFactory.getLog(QuotaStatsServiceImpl.class);

    public static final String STATUS_INITIAL_COMPUTATION_QUEUED = "status.quota.initialComputationQueued";

    public static final String STATUS_INITIAL_COMPUTATION_PENDING = "status.quota.initialComputationInProgress";

    public static final String STATUS_INITIAL_COMPUTATION_COMPLETED = "status.quota.initialComputationCompleted";

    // TODO configurable through an ep?
    public static final int DEFAULT_BATCH_SIZE = 1000;

    public static final String QUOTA_STATS_UPDATERS_EP = "quotaStatsUpdaters";

    protected QuotaStatsUpdaterRegistry quotaStatsUpdaterRegistry;

    @Override
    public void activate(ComponentContext context) {
        quotaStatsUpdaterRegistry = new QuotaStatsUpdaterRegistry();
    }

    @Override
    public List<QuotaStatsUpdater> getQuotaStatsUpdaters() {
        return quotaStatsUpdaterRegistry.getQuotaStatsUpdaters();
    }

    public QuotaStatsUpdater getQuotaStatsUpdaters(String updaterName) {
        return quotaStatsUpdaterRegistry.getQuotaStatsUpdater(updaterName);
    }

    @Override
    public void updateStatistics(final DocumentEventContext docCtx, final Event event) {
        // Open via session rather than repo name so that session.save and sync
        // is done automatically
        new UnrestrictedSessionRunner(docCtx.getCoreSession()) {
            @Override
            public void run() {
                List<QuotaStatsUpdater> quotaStatsUpdaters = quotaStatsUpdaterRegistry.getQuotaStatsUpdaters();
                for (QuotaStatsUpdater updater : quotaStatsUpdaters) {
                    if (log.isTraceEnabled()) {
                        DocumentModel doc = docCtx.getSourceDocument();
                        log.trace("Calling updateStatistics of " + updater.getName() + " for " + event.getName()
                                + " on " + doc.getId() + " (" + doc.getPathAsString() + ")");
                    }
                    updater.updateStatistics(session, docCtx, event);
                }
            }
        }.runUnrestricted();
    }

    @Override
    public void computeInitialStatistics(String updaterName, CoreSession session, QuotaStatsInitialWork currentWorker,
            String path) {
        QuotaStatsUpdater updater = quotaStatsUpdaterRegistry.getQuotaStatsUpdater(updaterName);
        if (updater != null) {
            updater.computeInitialStatistics(session, currentWorker, path);
        }
    }

    @Override
    public void launchInitialStatisticsComputation(String updaterName, String repositoryName, String path) {
        WorkManager workManager = Framework.getService(WorkManager.class);
        if (workManager == null) {
            throw new RuntimeException("No WorkManager available");
        }
        Work work = new QuotaStatsInitialWork(updaterName, repositoryName, path);
        workManager.schedule(work, true);
    }

    @Override
    public String getProgressStatus(String updaterName, String repositoryName) {
        WorkManager workManager = Framework.getService(WorkManager.class);
        Work work = new QuotaStatsInitialWork(updaterName, repositoryName, null);
        State state = workManager.getWorkState(work.getId());
        if (state == null) {
            return null;
        } else if (state == State.SCHEDULED) {
            return STATUS_INITIAL_COMPUTATION_QUEUED;
        } else if (state == State.RUNNING) {
            return STATUS_INITIAL_COMPUTATION_PENDING;
        } else { // RUNNING
            return STATUS_INITIAL_COMPUTATION_COMPLETED;
        }
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (QUOTA_STATS_UPDATERS_EP.equals(extensionPoint)) {
            quotaStatsUpdaterRegistry.addContribution((QuotaStatsUpdaterDescriptor) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (QUOTA_STATS_UPDATERS_EP.equals(extensionPoint)) {
            quotaStatsUpdaterRegistry.removeContribution((QuotaStatsUpdaterDescriptor) contribution);
        }
    }

    @Override
    public long getQuotaFromParent(DocumentModel doc, CoreSession session) {
        List<DocumentModel> parents = getParentsInReverseOrder(doc, session);
        // if a user workspace, only interested in the qouta on its direct
        // parent
        if (parents.size() > 0 && "UserWorkspacesRoot".equals(parents.get(0).getType())) {
            QuotaAware qa = parents.get(0).getAdapter(QuotaAware.class);
            return qa != null ? qa.getMaxQuota() : -1L;
        }
        for (DocumentModel documentModel : parents) {
            QuotaAware qa = documentModel.getAdapter(QuotaAware.class);
            if (qa == null) {
                continue;
            }
            if (qa.getMaxQuota() > 0) {
                return qa.getMaxQuota();
            }
        }
        return -1;
    }

    @Override
    public void activateQuotaOnUserWorkspaces(final long maxQuota, CoreSession session) {
        final String userWorkspacesRootId = getUserWorkspaceRootId(session.getRootDocument(), session);
        new UnrestrictedSessionRunner(session) {
            @Override
            public void run() {
                DocumentModel uwRoot = session.getDocument(new IdRef(userWorkspacesRootId));
                QuotaAware qa = QuotaAwareDocumentFactory.make(uwRoot);
                qa.setMaxQuota(maxQuota);
                qa.save();

            }
        }.runUnrestricted();
    }

    @Override
    public long getQuotaSetOnUserWorkspaces(CoreSession session) {
        final String userWorkspacesRootId = getUserWorkspaceRootId(session.getRootDocument(), session);
        return new UnrestrictedSessionRunner(session) {

            long quota = -1;

            public long getsQuotaSetOnUserWorkspaces() {
                runUnrestricted();
                return quota;
            }

            @Override
            public void run() {
                DocumentModel uwRoot = session.getDocument(new IdRef(userWorkspacesRootId));
                QuotaAware qa = uwRoot.getAdapter(QuotaAware.class);
                if (qa == null) {
                    quota = -1;
                } else {
                    quota = qa.getMaxQuota();
                }
            }
        }.getsQuotaSetOnUserWorkspaces();
    }

    protected List<DocumentModel> getParentsInReverseOrder(DocumentModel doc, CoreSession session)
            {
        UnrestrictedParentsFetcher parentsFetcher = new UnrestrictedParentsFetcher(doc, session);
        return parentsFetcher.getParents();
    }

    @Override
    public void launchSetMaxQuotaOnUserWorkspaces(final long maxSize, DocumentModel context, CoreSession session)
            {
        final String userWorkspacesId = getUserWorkspaceRootId(context, session);
        new UnrestrictedSessionRunner(session) {

            @Override
            public void run() {
                try (IterableQueryResult results = session.queryAndFetch(String.format(
                        "Select ecm:uuid from Workspace where ecm:parentId = '%s'  "
                                + "AND ecm:isVersion = 0 AND ecm:isTrashed = 0",
                        userWorkspacesId), "NXQL")) {
                    int size = 0;
                    List<String> allIds = new ArrayList<>();
                    for (Map<String, Serializable> map : results) {
                        allIds.add((String) map.get("ecm:uuid"));
                    }
                    List<String> ids = new ArrayList<>();
                    WorkManager workManager = Framework.getService(WorkManager.class);
                    for (String id : allIds) {
                        ids.add(id);
                        size++;
                        if (size % DEFAULT_BATCH_SIZE == 0) {
                            QuotaMaxSizeSetterWork work = new QuotaMaxSizeSetterWork(maxSize, ids,
                                    session.getRepositoryName());
                            workManager.schedule(work, true);
                            ids = new ArrayList<>(); // don't reuse list
                        }
                    }
                    if (ids.size() > 0) {
                        QuotaMaxSizeSetterWork work = new QuotaMaxSizeSetterWork(maxSize, ids,
                                session.getRepositoryName());
                        workManager.schedule(work, true);
                    }
                }
            }
        }.runUnrestricted();
    }

    public String getUserWorkspaceRootId(DocumentModel context, CoreSession session) {
        // get only the userworkspaces root under the first domain
        // it should be only one
        DocumentModel currentUserWorkspace = Framework.getService(UserWorkspaceService.class).getUserPersonalWorkspace(
                session.getPrincipal().getName(), context);

        return ((IdRef) currentUserWorkspace.getParentRef()).value;
    }

    @Override
    public boolean canSetMaxQuota(long maxQuota, DocumentModel doc, CoreSession session) {
        QuotaAware qa = null;
        DocumentModel parent = null;
        if ("UserWorkspacesRoot".equals(doc.getType())) {
            return true;
        }
        List<DocumentModel> parents = getParentsInReverseOrder(doc, session);
        if (!parents.isEmpty()) {
            if ("UserWorkspacesRoot".equals(parents.get(0).getType())) {
                // checks don't apply to personal user workspaces
                return true;
            }
        }
        for (DocumentModel p : parents) {
            qa = p.getAdapter(QuotaAware.class);
            if (qa == null) {
                // if no quota set on the parent, any value is valid
                continue;
            }
            if (qa.getMaxQuota() > 0) {
                parent = p;
                break;
            }
        }
        if (qa == null || qa.getMaxQuota() < 0) {
            return true;
        }

        long maxAllowedOnChildrenToSetQuota = qa.getMaxQuota() - maxQuota;
        if (maxAllowedOnChildrenToSetQuota < 0) {
            return false;
        }
        Long quotaOnChildren = new UnrestrictedQuotaOnChildrenCalculator(parent, maxAllowedOnChildrenToSetQuota,
                doc.getId(), session).quotaOnChildren();
        if (quotaOnChildren > 0 && quotaOnChildren > maxAllowedOnChildrenToSetQuota) {
            return false;
        }
        return true;
    }

    class UnrestrictedQuotaOnChildrenCalculator extends UnrestrictedSessionRunner {

        DocumentModel parent;

        Long maxAllowedOnChildrenToSetQuota;

        long quotaOnChildren = -1;

        String currentDocIdToIgnore;

        protected UnrestrictedQuotaOnChildrenCalculator(DocumentModel parent, Long maxAllowedOnChildrenToSetQuota,
                String currentDocIdToIgnore, CoreSession session) {
            super(session);
            this.parent = parent;
            this.maxAllowedOnChildrenToSetQuota = maxAllowedOnChildrenToSetQuota;
            this.currentDocIdToIgnore = currentDocIdToIgnore;
        }

        @Override
        public void run() {
            quotaOnChildren = canSetMaxQuotaOnChildrenTree(maxAllowedOnChildrenToSetQuota, quotaOnChildren, parent,
                    currentDocIdToIgnore, session);
        }

        public long quotaOnChildren() {
            runUnrestricted();
            return quotaOnChildren;
        }

        protected Long canSetMaxQuotaOnChildrenTree(Long maxAllowedOnChildrenToSetQuota, Long quotaOnChildren,
                DocumentModel doc, String currentDocIdToIgnore, CoreSession session) {
            if (quotaOnChildren > 0 && quotaOnChildren > maxAllowedOnChildrenToSetQuota) {
                // quota can not be set, don't continue
                return quotaOnChildren;
            }
            DocumentModelIterator childrenIterator = null;
            childrenIterator = session.getChildrenIterator(doc.getRef(), null, null, new QuotaFilter());

            while (childrenIterator.hasNext()) {
                DocumentModel child = childrenIterator.next();
                QuotaAware qac = child.getAdapter(QuotaAware.class);
                if (qac == null) {
                    continue;
                }
                if (qac.getMaxQuota() > 0 && !currentDocIdToIgnore.equals(child.getId())) {
                    quotaOnChildren = (quotaOnChildren == -1L ? 0L : quotaOnChildren) + qac.getMaxQuota();
                }
                if (quotaOnChildren > 0 && quotaOnChildren > maxAllowedOnChildrenToSetQuota) {
                    return quotaOnChildren;
                }
                if (qac.getMaxQuota() == -1L) {
                    // if there is no quota set at this level, go deeper
                    quotaOnChildren = canSetMaxQuotaOnChildrenTree(maxAllowedOnChildrenToSetQuota, quotaOnChildren,
                            child, currentDocIdToIgnore, session);
                }
                if (quotaOnChildren > 0 && quotaOnChildren > maxAllowedOnChildrenToSetQuota) {
                    return quotaOnChildren;
                }
            }
            return quotaOnChildren;
        }
    }

    class UnrestrictedParentsFetcher extends UnrestrictedSessionRunner {

        DocumentModel doc;

        List<DocumentModel> parents;

        protected UnrestrictedParentsFetcher(DocumentModel doc, CoreSession session) {
            super(session);
            this.doc = doc;
        }

        @Override
        public void run() {
            parents = new ArrayList<>();
            DocumentRef[] parentRefs = session.getParentDocumentRefs(doc.getRef());
            for (DocumentRef documentRef : parentRefs) {
                parents.add(session.getDocument(documentRef));
            }
            for (DocumentModel parent : parents) {
                parent.detach(true);
            }
        }

        public List<DocumentModel> getParents() {
            runUnrestricted();
            return parents;
        }
    }

    class QuotaFilter implements Filter {
        @Override
        public boolean accept(DocumentModel doc) {
            if ("UserWorkspacesRoot".equals(doc.getType())) {
                return false;
            }
            return true;
        }
    }
}
