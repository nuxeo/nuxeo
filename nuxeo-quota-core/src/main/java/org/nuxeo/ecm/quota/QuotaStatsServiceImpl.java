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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.Sorter;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.Work.State;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkManager.Scheduling;
import org.nuxeo.ecm.quota.size.QuotaAware;
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
    public String getProgressStatus(String updaterName) {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        Work work = new QuotaStatsInitialWork(updaterName, null);
        work = workManager.find(work, null, true, null);
        if (work == null) {
            return null;
        } else if (work.getState() == State.SCHEDULED) {
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
        List<DocumentModel> parents = new ArrayList<DocumentModel>();
        parents.addAll(session.getParentDocuments(doc.getRef()));
        Collections.reverse(parents);
        parents.remove(0);
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
    public boolean canSetMaxQuota(long maxQuota, DocumentModel doc,
            CoreSession session) throws ClientException {
        QuotaAware qa = null;
        DocumentModel parent = null;
        List<DocumentModel> parents = getParentsInReverseOrder(doc, session);
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
        if (qa == null) {
            return true;
        }

        long maxAllowedOnChildrenToSetQuota = qa.getMaxQuota() - maxQuota;
        if (maxAllowedOnChildrenToSetQuota < 0) {
            return false;
        }
        // Long quotaOnChildren = canSetMaxQuotaOnChildrenTree(
        // maxAllowedOnChildrenToSetQuota, -1L, parent, session);
        // if (quotaOnChildren > 0
        // && quotaOnChildren > maxAllowedOnChildrenToSetQuota) {
        // return false;
        // }
        return canSetMaxQuotaOnChildrenTree(parent,
                maxAllowedOnChildrenToSetQuota, session);
    }

    protected List<DocumentModel> getParentsInReverseOrder(DocumentModel doc,
            CoreSession session) throws ClientException {
        List<DocumentModel> parents = new ArrayList<DocumentModel>();
        parents.addAll(session.getParentDocuments(doc.getRef()));
        Collections.reverse(parents);
        parents.remove(0);
        return parents;
    }

    protected Long canSetMaxQuotaOnChildrenTree(
            Long maxAllowedOnChildrenToSetQuota, Long quotaOnChildren,
            DocumentModel doc, CoreSession session) throws ClientException {
        if (quotaOnChildren > 0
                && quotaOnChildren > maxAllowedOnChildrenToSetQuota) {
            // quota can not be set, don't continue
            return quotaOnChildren;
        }
        List<DocumentModel> children = new LinkedList<DocumentModel>(
                session.getChildren(doc.getRef(), null, null, new QuotaSorter(
                        false)));

        for (DocumentModel child : children) {
            QuotaAware qac = child.getAdapter(QuotaAware.class);
            if (qac == null) {
                continue;
            }
            if (qac.getMaxQuota() > 0) {
                quotaOnChildren = (quotaOnChildren == -1L ? 0L
                        : quotaOnChildren) + qac.getMaxQuota();
            }
            if (quotaOnChildren > 0
                    && quotaOnChildren > maxAllowedOnChildrenToSetQuota) {
                return quotaOnChildren;
            }
            quotaOnChildren = canSetMaxQuotaOnChildrenTree(
                    maxAllowedOnChildrenToSetQuota, quotaOnChildren, child,
                    session);
            if (quotaOnChildren > 0
                    && quotaOnChildren > maxAllowedOnChildrenToSetQuota) {
                return quotaOnChildren;
            }

        }
        return quotaOnChildren;
    }

    protected boolean canSetMaxQuotaOnChildrenTree(DocumentModel parent,
            Long maxAllowedOnChildrenToSetQuota, CoreSession session)
            throws ClientException {
        long quotaOnChildren = 0;
        IterableQueryResult results = session.queryAndFetch(
                String.format(
                        "Select dss:maxSize from Document where ecm:path STARTSWITH '%s' AND ecm:mixinType = 'DocumentsSizeStatistics' "
                                + "AND ecm:isCheckedInVersion = 0 AND ecm:currentLifeCycleState != 'deleted' ORDER BY dss:maxSize DESC",
                        parent.getPath()), "NXQL");

        for (Map<String, Serializable> result : results) {
            Long maxSize = (Long) result.get("dss:maxSize");
            quotaOnChildren = quotaOnChildren
                    + (maxSize == null || maxSize == -1 ? 0 : maxSize);
            if (quotaOnChildren > maxAllowedOnChildrenToSetQuota) {
                return false;
            }
        }
        return true;
    }

    class QuotaSorter implements Sorter {

        private static final long serialVersionUID = 1L;

        private boolean asc = true;

        public QuotaSorter(boolean asc) {
            this.asc = asc;
        }

        @Override
        public int compare(DocumentModel doc1, DocumentModel doc2) {
            if (doc1 == null && doc2 == null) {
                return 0;
            } else if (doc1 == null) {
                return asc ? -1 : 1;
            } else if (doc2 == null) {
                return asc ? 1 : -1;
            }

            int cmp = 0;
            try {

                Long maxQuota1 = (Long) doc1.getPropertyValue("dss:maxSize");
                Long maxQuota2 = (Long) doc2.getPropertyValue("dss:maxSize");

                if (maxQuota1 == null && maxQuota2 == null) {
                    return 0;
                } else if (maxQuota1 == null) {
                    return asc ? -1 : 1;
                } else if (maxQuota2 == null) {
                    return asc ? 1 : -1;
                }
                cmp = maxQuota1.compareTo(maxQuota2);
            } catch (Exception e) {
                // if property not found exception
            }
            return asc ? cmp : -cmp;
        }
    }

}
