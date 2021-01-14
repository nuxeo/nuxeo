/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 *     Thierry Delprat
 */
package org.nuxeo.ecm.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.versioning.DefaultVersionRemovalPolicy;
import org.nuxeo.ecm.core.versioning.OrphanVersionRemovalFilter;
import org.nuxeo.ecm.core.versioning.VersionRemovalPolicy;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Service used to register version removal policies.
 */
public class CoreService extends DefaultComponent {

    private static final String VERSION_REMOVAL_POLICY_XP = "versionRemovalPolicy";

    private static final String ORPHAN_VERSION_REMOVAL_FILTER_XP = "orphanVersionRemovalFilter";

    protected static final DefaultVersionRemovalPolicy DEFAULT_VERSION_REMOVAL_POLICY = new DefaultVersionRemovalPolicy();

    protected VersionRemovalPolicy versionRemovalPolicy;

    protected List<OrphanVersionRemovalFilter> orphanVersionRemovalFilters;

    protected ComponentContext context;

    @Override
    public void activate(ComponentContext context) {
        this.context = context;
    }

    @Override
    public void deactivate(ComponentContext context) {
        this.context = null;
    }

    @Override
    public void start(ComponentContext context) {
        versionRemovalPolicy = this.<CoreServicePolicyDescriptor> getRegistryContribution(VERSION_REMOVAL_POLICY_XP)
                                   .map(this::createVersionRemovalPolicy)
                                   .orElse(DEFAULT_VERSION_REMOVAL_POLICY);
        orphanVersionRemovalFilters = this.<CoreServiceOrphanVersionRemovalFilterDescriptor> getRegistryContributions(
                ORPHAN_VERSION_REMOVAL_FILTER_XP)
                                          .stream()
                                          .map(this::createOrphanVersionRemovalFilter)
                                          .collect(Collectors.toList());
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        versionRemovalPolicy = null;
        orphanVersionRemovalFilters = null;
    }

    protected VersionRemovalPolicy createVersionRemovalPolicy(CoreServicePolicyDescriptor contrib) {
        String klass = contrib.getKlass();
        try {
            return (VersionRemovalPolicy) context.getRuntimeContext()
                                                 .loadClass(klass)
                                                 .getDeclaredConstructor()
                                                 .newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to instantiate " + VERSION_REMOVAL_POLICY_XP + ": " + klass, e);
        }
    }

    protected OrphanVersionRemovalFilter createOrphanVersionRemovalFilter(
            CoreServiceOrphanVersionRemovalFilterDescriptor contrib) {
        String klass = contrib.getKlass();
        try {
            return (OrphanVersionRemovalFilter) context.getRuntimeContext()
                                                       .loadClass(klass)
                                                       .getDeclaredConstructor()
                                                       .newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to instantiate " + ORPHAN_VERSION_REMOVAL_FILTER_XP + ": " + klass, e);
        }
    }

    /** Gets the last version removal policy registered. */
    public VersionRemovalPolicy getVersionRemovalPolicy() {
        return versionRemovalPolicy;
    }

    /** Gets all the orphan version removal filters registered. */
    public Collection<OrphanVersionRemovalFilter> getOrphanVersionRemovalFilters() {
        return Collections.unmodifiableList(orphanVersionRemovalFilters);
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
    public long cleanupOrphanVersions(long commitSize) {
        RepositoryService repositoryService = Framework.getService(RepositoryService.class);
        if (repositoryService == null) {
            // not initialized
            return 0;
        }
        List<String> repositoryNames = repositoryService.getRepositoryNames();
        AtomicLong count = new AtomicLong();
        for (String repositoryName : repositoryNames) {
            TransactionHelper.runInTransaction(() -> {
                CoreInstance.doPrivileged(repositoryName, (CoreSession session) -> {
                    count.addAndGet(doCleanupOrphanVersions(session, commitSize));
                });
            });
        }
        return count.get();
    }

    protected long doCleanupOrphanVersions(CoreSession session, long commitSize) {
        // compute map of version series -> list of version ids in it
        Map<String, List<String>> versionSeriesToVersionIds = new HashMap<>();
        String findVersions = "SELECT " + NXQL.ECM_UUID + ", " + NXQL.ECM_VERSION_VERSIONABLEID
                + " FROM Document WHERE " + NXQL.ECM_ISVERSION + " = 1";
        try (IterableQueryResult res = session.queryAndFetch(findVersions, NXQL.NXQL)) {
            for (Map<String, Serializable> map : res) {
                String versionSeriesId = (String) map.get(NXQL.ECM_VERSION_VERSIONABLEID);
                String versionId = (String) map.get(NXQL.ECM_UUID);
                versionSeriesToVersionIds.computeIfAbsent(versionSeriesId, k -> new ArrayList<>(4)).add(versionId);
            }
        }
        Set<String> seriesIds = new HashSet<>();
        // find the live doc ids
        String findLive = "SELECT " + NXQL.ECM_UUID + " FROM Document WHERE " + NXQL.ECM_ISPROXY + " = 0 AND "
                + NXQL.ECM_ISVERSION + " = 0";
        try (IterableQueryResult res = session.queryAndFetch(findLive, NXQL.NXQL)) {
            for (Map<String, Serializable> map : res) {
                String id = (String) map.get(NXQL.ECM_UUID);
                seriesIds.add(id);
            }
        }
        // find the version series for proxies
        String findProxies = "SELECT " + NXQL.ECM_PROXY_VERSIONABLEID + " FROM Document WHERE " + NXQL.ECM_ISPROXY
                + " = 1";
        try (IterableQueryResult res = session.queryAndFetch(findProxies, NXQL.NXQL)) {
            for (Map<String, Serializable> map : res) {
                String versionSeriesId = (String) map.get(NXQL.ECM_PROXY_VERSIONABLEID);
                seriesIds.add(versionSeriesId);
            }
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
