/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 *     Thierry Delprat
 */
package org.nuxeo.ecm.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.versioning.DefaultVersionRemovalPolicy;
import org.nuxeo.ecm.core.versioning.OrphanVersionRemovalFilter;
import org.nuxeo.ecm.core.versioning.VersionRemovalPolicy;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Service used to register version removal policies.
 */
public class CoreService extends DefaultComponent {

    private static final Log log = LogFactory.getLog(CoreService.class);

    private ComponentContext context;

    private VersionRemovalPolicy versionRemovalPolicy;

    private List<OrphanVersionRemovalFilter> orphanVersionRemovalFilters = new ArrayList<OrphanVersionRemovalFilter>();

    public List<OrphanVersionRemovalFilter> getOrphanVersionRemovalFilters() {
        return orphanVersionRemovalFilters;
    }

    public VersionRemovalPolicy getVersionRemovalPolicy() {
        if (versionRemovalPolicy == null) {
            versionRemovalPolicy = new DefaultVersionRemovalPolicy();
        }
        return versionRemovalPolicy;
    }

    @Override
    public void activate(ComponentContext context) {
        this.context = context;
    }

    @Override
    public void deactivate(ComponentContext context) {
        this.context = null;
    }

    @Override
    public void registerExtension(Extension extension) {
        String point = extension.getExtensionPoint();
        if ("versionRemovalPolicy".equals(point)) {
            for (Object contrib : extension.getContributions()) {
                if (contrib instanceof CoreServicePolicyDescriptor) {
                    registerVersionRemovalPolicy((CoreServicePolicyDescriptor) contrib);
                } else {
                    log.error("Invalid contribution to extension point 'versionRemovalPolicy': "
                            + contrib.getClass().getName());
                }
            }
        } else if ("orphanVersionRemovalFilter".equals(point)) {
            for (Object contrib : extension.getContributions()) {
                if (contrib instanceof CoreServiceOrphanVersionRemovalFilterDescriptor) {
                    registerOrphanVersionRemovalFilter((CoreServiceOrphanVersionRemovalFilterDescriptor) contrib);
                } else {
                    log.error("Invalid contribution to extension point 'orphanVersionRemovalFilter': "
                            + contrib.getClass().getName());
                }
            }
        } else {
            log.error("Unknown extension point: " + point);
        }
    }

    @Override
    public void unregisterExtension(Extension extension) {
    }

    private void registerVersionRemovalPolicy(CoreServicePolicyDescriptor desc) {
        String klass = desc.getKlass();
        try {
            versionRemovalPolicy = (VersionRemovalPolicy) context.getRuntimeContext().loadClass(
                    klass).newInstance();
        } catch (Exception e) {
            log.error("Failed to instantiate versionRemovalPolicy: " + klass, e);
        }
    }

    private void registerOrphanVersionRemovalFilter(
            CoreServiceOrphanVersionRemovalFilterDescriptor desc) {
        String klass = desc.getKlass();
        try {
            orphanVersionRemovalFilters.add((OrphanVersionRemovalFilter) context.getRuntimeContext().loadClass(
                    klass).newInstance());
        } catch (Exception e) {
            log.error("Failed to instantiate versionRemovalPolicy: " + klass, e);
        }
    }

    /**
     * Removes the orphan versions.
     * <p>
     * A version stays referenced, and therefore is not removed, if any proxy points to a version in the version history
     * of any live document, or in the case of tree snapshot if there is a snapshot containing a version in the version
     * history of any live document.
     *
     * @param commitSize the maximum number of orphan versions to delete in one transaction
     * @return the number of orphan versions deleted
     * @since 9.1
     */
    public long cleanupOrphanVersions(final long commitSize) {
        RepositoryService repositoryService = Framework.getService(RepositoryService.class);
        if (repositoryService == null) {
            // not initialized
            return 0;
        }
        List<String> repositoryNames = repositoryService.getRepositoryNames();
        final AtomicLong count = new AtomicLong();
        for (String repositoryName : repositoryNames) {
            boolean startTransaction = !TransactionHelper.isTransactionActiveOrMarkedRollback();
            if (startTransaction) {
                if (!TransactionHelper.startTransaction()) {
                    throw new RuntimeException("Cannot start transaction");
                }
            }
            boolean completedAbruptly = true;
            try {
                new UnrestrictedSessionRunner(repositoryName) {
                    @Override
                    public void run() {
                        count.addAndGet(doCleanupOrphanVersions(session, commitSize));
                    }
                }.runUnrestricted();
                completedAbruptly = false;
            } finally {
                try {
                    if (completedAbruptly) {
                        TransactionHelper.setTransactionRollbackOnly();
                    }
                } finally {
                    if (startTransaction) {
                        TransactionHelper.commitOrRollbackTransaction();
                    }
                }
            }
        }
        return count.get();
    }

    protected long doCleanupOrphanVersions(CoreSession session, long commitSize) {
        // compute map of version series -> list of version ids in it
        Map<String, List<String>> versionSeriesToVersionIds = new HashMap<>();
        String findVersions = "SELECT " + NXQL.ECM_UUID + ", " + NXQL.ECM_VERSION_VERSIONABLEID
                + " FROM Document WHERE " + NXQL.ECM_ISVERSION + " = 1";
        IterableQueryResult res = session.queryAndFetch(findVersions, NXQL.NXQL);
        try {
            for (Map<String, Serializable> map : res) {
                String versionSeriesId = (String) map.get(NXQL.ECM_VERSION_VERSIONABLEID);
                String versionId = (String) map.get(NXQL.ECM_UUID);
                List<String> versionIds = versionSeriesToVersionIds.get(versionSeriesId);
                if (versionIds == null) {
                    versionSeriesToVersionIds.put(versionSeriesId, versionIds = new ArrayList<>(4));
                }
                versionIds.add(versionId);
            }
        } finally {
            res.close();
        }
        Set<String> seriesIds = new HashSet<>();
        // find the live doc ids
        String findLive = "SELECT " + NXQL.ECM_UUID + " FROM Document WHERE " + NXQL.ECM_ISPROXY + " = 0 AND "
                + NXQL.ECM_ISVERSION + " = 0";
        res = session.queryAndFetch(findLive, NXQL.NXQL);
        try {
            for (Map<String, Serializable> map : res) {
                String id = (String) map.get(NXQL.ECM_UUID);
                seriesIds.add(id);
            }
        } finally {
            res.close();
        }
        // find the version series for proxies
        String findProxies = "SELECT " + NXQL.ECM_PROXY_VERSIONABLEID + " FROM Document WHERE " + NXQL.ECM_ISPROXY
                + " = 1";
        res = session.queryAndFetch(findProxies, NXQL.NXQL);
        try {
            for (Map<String, Serializable> map : res) {
                String versionSeriesId = (String) map.get(NXQL.ECM_PROXY_VERSIONABLEID);
                seriesIds.add(versionSeriesId);
            }
        } finally {
            res.close();
        }
        // all version for series ids not found from live docs or proxies can be removed
        Set<String> ids = new HashSet<>();
        for (Entry<String, List<String>> en : versionSeriesToVersionIds.entrySet()) {
            if (seriesIds.contains(en.getKey())) {
                continue;
            }
            // not referenced -> remove
            List<String> versionIds = en.getValue();
            ids.addAll(versionIds);
        }
        // new transaction as we may have spent some time in the previous queries
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        // remove these ids
        if (!ids.isEmpty()) {
            long n = 0;
            for (String id : ids) {
                session.removeDocument(new IdRef(id));
                n++;
                if (n >= commitSize) {
                    session.save();
                    TransactionHelper.commitOrRollbackTransaction();
                    TransactionHelper.startTransaction();
                    n = 0;
                }
            }
            session.save();
        }
        return ids.size();
    }

}
