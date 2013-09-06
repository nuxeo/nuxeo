/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.core.api.ClientException;
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
import org.nuxeo.ecm.core.work.api.WorkManager.Scheduling;
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
public class QuotaStatsServiceImpl extends DefaultComponent implements
        QuotaStatsService {

    private static final Log log = LogFactory.getLog(QuotaStatsServiceImpl.class);

    public static final String STATUS_INITIAL_COMPUTATION_QUEUED = "status.quota.initialComputationQueued";

    public static final String STATUS_INITIAL_COMPUTATION_PENDING = "status.quota.initialComputationInProgress";

    // TODO configurable through an ep?
    public static final int DEFAULT_BATCH_SIZE = 1000;

    public static final String QUOTA_STATS_UPDATERS_EP = "quotaStatsUpdaters";

    protected QuotaStatsUpdaterRegistry quotaStatsUpdaterRegistry;

    @Override
    public void activate(ComponentContext context) throws Exception {
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
    public void updateStatistics(final DocumentEventContext docCtx,
            final Event event) throws ClientException {
        // Open via session rather than repo name so that session.save and sync
        // is done automatically
        new UnrestrictedSessionRunner(docCtx.getCoreSession()) {
            @Override
            public void run() throws ClientException {
                List<QuotaStatsUpdater> quotaStatsUpdaters = quotaStatsUpdaterRegistry.getQuotaStatsUpdaters();
                for (QuotaStatsUpdater updater : quotaStatsUpdaters) {
                    log.debug("Calling updateStatistics on "
                            + updater.getName());
                    updater.updateStatistics(session, docCtx, event);
                }
            }
        }.runUnrestricted();
    }

    @Override
    public void computeInitialStatistics(String updaterName,
            CoreSession session, QuotaStatsInitialWork currentWorker) {
        QuotaStatsUpdater updater = quotaStatsUpdaterRegistry.getQuotaStatsUpdater(updaterName);
        if (updater != null) {
            updater.computeInitialStatistics(session, currentWorker);
        }
    }

    @Override
    public void launchInitialStatisticsComputation(String updaterName,
            String repositoryName) {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        if (workManager == null) {
            throw new RuntimeException("No WorkManager available");
        }
        Work work = new QuotaStatsInitialWork(updaterName, repositoryName);
        workManager.schedule(work, Scheduling.IF_NOT_RUNNING_OR_SCHEDULED);
    }

    @Override
    public String getProgressStatus(String updaterName, String repositoryName) {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        Work work = new QuotaStatsInitialWork(updaterName, repositoryName);
        State state = workManager.getWorkState(work.getId());
        if (state == null) {
            return null;
        } else if (state == State.SCHEDULED) {
            return STATUS_INITIAL_COMPUTATION_QUEUED;
        } else { // RUNNING
            return STATUS_INITIAL_COMPUTATION_PENDING;
        }
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (QUOTA_STATS_UPDATERS_EP.equals(extensionPoint)) {
            quotaStatsUpdaterRegistry.addContribution((QuotaStatsUpdaterDescriptor) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (QUOTA_STATS_UPDATERS_EP.equals(extensionPoint)) {
            quotaStatsUpdaterRegistry.removeContribution((QuotaStatsUpdaterDescriptor) contribution);
        }
    }

    @Override
    public long getQuotaFromParent(DocumentModel doc, CoreSession session)
            throws ClientException {
        List<DocumentModel> parents = getParentsInReverseOrder(doc, session);
        // if a user workspace, only interested in the qouta on its direct
        // parent
        if (parents.size() > 0
                && "UserWorkspacesRoot".equals(parents.get(0).getType())) {
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
    public void activateQuotaOnUserWorkspaces(final long maxQuota,
            CoreSession session) throws ClientException {
        final String userWorkspacesRootId = getUserWorkspaceRootId(
                session.getRootDocument(), session);
        new UnrestrictedSessionRunner(session) {
            @Override
            public void run() throws ClientException {
                DocumentModel uwRoot = session.getDocument(new IdRef(
                        userWorkspacesRootId));
                QuotaAware qa = uwRoot.getAdapter(QuotaAware.class);
                if (qa == null) {
                    qa = QuotaAwareDocumentFactory.make(uwRoot, false);
                }
                qa.setMaxQuota(maxQuota, true, false);

            };
        }.runUnrestricted();
    }

    @Override
    public long getQuotaSetOnUserWorkspaces(CoreSession session)
            throws ClientException {
        final String userWorkspacesRootId = getUserWorkspaceRootId(
                session.getRootDocument(), session);
        return new UnrestrictedSessionRunner(session) {

            long quota = -1;

            public long getsQuotaSetOnUserWorkspaces() throws ClientException {
                runUnrestricted();
                return quota;
            }

            @Override
            public void run() throws ClientException {
                DocumentModel uwRoot = session.getDocument(new IdRef(
                        userWorkspacesRootId));
                QuotaAware qa = uwRoot.getAdapter(QuotaAware.class);
                if (qa == null) {
                    quota = -1;
                } else {
                    quota = qa.getMaxQuota();
                }
            }
        }.getsQuotaSetOnUserWorkspaces();
    }

    protected List<DocumentModel> getParentsInReverseOrder(DocumentModel doc,
            CoreSession session) throws ClientException {
        UnrestrictedParentsFetcher parentsFetcher = new UnrestrictedParentsFetcher(
                doc, session);
        return parentsFetcher.getParents();
    }

    @Override
    public void launchSetMaxQuotaOnUserWorkspaces(final long maxSize,
            DocumentModel context, CoreSession session) throws ClientException {
        final String userWorkspacesId = getUserWorkspaceRootId(context, session);
        new UnrestrictedSessionRunner(session) {

            @Override
            public void run() throws ClientException {
                IterableQueryResult results = session.queryAndFetch(
                        String.format(
                                "Select ecm:uuid from Workspace where ecm:parentId = '%s'  "
                                        + "AND ecm:isCheckedInVersion = 0 AND ecm:currentLifeCycleState != 'deleted' ",
                                userWorkspacesId), "NXQL");
                int size = 0;
                List<String> allIds = new ArrayList<String>();
                for (Map<String, Serializable> map : results) {
                    allIds.add((String) map.get("ecm:uuid"));
                }
                results.close();
                List<String> ids = new ArrayList<String>();
                WorkManager workManager = Framework.getLocalService(WorkManager.class);
                for (String id : allIds) {
                    ids.add(id);
                    size++;
                    if (size % DEFAULT_BATCH_SIZE == 0) {
                        QuotaMaxSizeSetterWork work = new QuotaMaxSizeSetterWork(
                                maxSize, ids, session.getRepositoryName());
                        workManager.schedule(work);
                        ids.clear();
                    }
                }
                if (ids.size() > 0) {
                    QuotaMaxSizeSetterWork work = new QuotaMaxSizeSetterWork(
                            maxSize, ids, session.getRepositoryName());
                    workManager.schedule(work);
                }
            }
        }.runUnrestricted();
    }

    public String getUserWorkspaceRootId(DocumentModel context,
            CoreSession session) throws ClientException {
        // get only the userworkspaces root under the first domain
        // it should be only one
        DocumentModel currentUserWorkspace = Framework.getLocalService(
                UserWorkspaceService.class).getUserPersonalWorkspace(
                session.getPrincipal().getName(), context);


        return ((IdRef) currentUserWorkspace.getParentRef()).value;
    }

    @Override
    public boolean canSetMaxQuota(long maxQuota, DocumentModel doc,
            CoreSession session) throws ClientException {
        QuotaAware qa = null;
        DocumentModel parent = null;
        if ("UserWorkspacesRoot".equals(doc.getType())) {
            return true;
        }
        List<DocumentModel> parents = getParentsInReverseOrder(doc, session);
        if (parents != null && parents.size() > 0) {
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
        Long quotaOnChildren = new UnrestrictedQuotaOnChildrenCalculator(
                parent, maxAllowedOnChildrenToSetQuota, doc.getId(), session).quotaOnChildren();
        if (quotaOnChildren > 0
                && quotaOnChildren > maxAllowedOnChildrenToSetQuota) {
            return false;
        }
        return true;
    }

    class UnrestrictedQuotaOnChildrenCalculator extends
            UnrestrictedSessionRunner {

        DocumentModel parent;

        Long maxAllowedOnChildrenToSetQuota;

        long quotaOnChildren = -1;

        String currentDocIdToIgnore;

        protected UnrestrictedQuotaOnChildrenCalculator(DocumentModel parent,
                Long maxAllowedOnChildrenToSetQuota,
                String currentDocIdToIgnore, CoreSession session) {
            super(session);
            this.parent = parent;
            this.maxAllowedOnChildrenToSetQuota = maxAllowedOnChildrenToSetQuota;
            this.currentDocIdToIgnore = currentDocIdToIgnore;
        }

        @Override
        public void run() throws ClientException {
            quotaOnChildren = canSetMaxQuotaOnChildrenTree(
                    maxAllowedOnChildrenToSetQuota, quotaOnChildren, parent,
                    currentDocIdToIgnore, session);
        }

        public long quotaOnChildren() throws ClientException {
            runUnrestricted();
            return quotaOnChildren;
        }

        protected Long canSetMaxQuotaOnChildrenTree(
                Long maxAllowedOnChildrenToSetQuota, Long quotaOnChildren,
                DocumentModel doc, String currentDocIdToIgnore,
                CoreSession session) throws ClientException {
            if (quotaOnChildren > 0
                    && quotaOnChildren > maxAllowedOnChildrenToSetQuota) {
                // quota can not be set, don't continue
                return quotaOnChildren;
            }
            DocumentModelIterator childrenIterator = null;
            childrenIterator = session.getChildrenIterator(doc.getRef(), null,
                    null, new QuotaFilter());

            while (childrenIterator.hasNext()) {
                DocumentModel child = childrenIterator.next();
                QuotaAware qac = child.getAdapter(QuotaAware.class);
                if (qac == null) {
                    continue;
                }
                if (qac.getMaxQuota() > 0
                        && !currentDocIdToIgnore.equals(child.getId())) {
                    quotaOnChildren = (quotaOnChildren == -1L ? 0L
                            : quotaOnChildren) + qac.getMaxQuota();
                }
                if (quotaOnChildren > 0
                        && quotaOnChildren > maxAllowedOnChildrenToSetQuota) {
                    return quotaOnChildren;
                }
                if (qac.getMaxQuota() == -1L) {
                    // if there is no quota set at this level, go deeper
                    quotaOnChildren = canSetMaxQuotaOnChildrenTree(
                            maxAllowedOnChildrenToSetQuota, quotaOnChildren,
                            child, currentDocIdToIgnore, session);
                }
                if (quotaOnChildren > 0
                        && quotaOnChildren > maxAllowedOnChildrenToSetQuota) {
                    return quotaOnChildren;
                }
            }
            return quotaOnChildren;
        }
    }

    class UnrestrictedParentsFetcher extends UnrestrictedSessionRunner {

        DocumentModel doc;

        List<DocumentModel> parents;

        protected UnrestrictedParentsFetcher(DocumentModel doc,
                CoreSession session) {
            super(session);
            this.doc = doc;
        }

        @Override
        public void run() throws ClientException {
            parents = new ArrayList<DocumentModel>();
            DocumentRef[] parentRefs = session.getParentDocumentRefs(doc.getRef());
            for (DocumentRef documentRef : parentRefs) {
                parents.add(session.getDocument(documentRef));
            }
            for (DocumentModel parent : parents) {
                parent.detach(true);
            }
        }

        public List<DocumentModel> getParents() throws ClientException {
            runUnrestricted();
            return parents;
        }
    }

    class QuotaFilter implements Filter {

        private static final long serialVersionUID = 1L;

        @Override
        public boolean accept(DocumentModel doc) {
            if ("UserWorkspacesRoot".equals(doc.getType())) {
                return false;
            }
            return true;
        }
    }
}