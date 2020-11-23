/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.runtime.cluster;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Implementation for the Cluster Service.
 *
 * @since 11.1
 */
public class ClusterServiceImpl extends DefaultComponent implements ClusterService {

    private static final Logger log = LogManager.getLogger(ClusterServiceImpl.class);

    /** Very early as other services depend on us. */
    public static final int APPLICATION_STARTED_ORDER = -1000;

    public static final String XP_CONFIG = "configuration";

    public static final String CLUSTERING_ENABLED_OLD_PROP = "repository.clustering.enabled";

    public static final String NODE_ID_OLD_PROP = "repository.clustering.id";

    protected static final Random RANDOM = new Random(); // NOSONAR (doesn't need cryptographic strength)

    protected boolean enabled;

    protected String nodeId;

    @Override
    public int getApplicationStartedOrder() {
        return APPLICATION_STARTED_ORDER;
    }

    @Override
    public void start(ComponentContext context) {
        ClusterNodeDescriptor descr = getSingleContribution(XP_CONFIG, ClusterNodeDescriptor.class);

        // enabled
        Boolean enabledProp = descr == null ? null : descr.getEnabled();
        if (enabledProp != null) {
            enabled = enabledProp.booleanValue();
        } else {
            // compat with old framework property
            enabled = Framework.isBooleanPropertyTrue(CLUSTERING_ENABLED_OLD_PROP);
        }

        // node id
        String id = descr == null ? null : defaultIfBlank(descr.getName(), null);
        if (id != null) {
            nodeId = id.trim();
        } else {
            // compat with old framework property
            id = Framework.getProperty(NODE_ID_OLD_PROP);
            if (isNotBlank(id)) {
                nodeId = id.trim();
            } else {
                // use a random node id
                long l;
                do {
                    l = RANDOM.nextLong();
                } while (l < 0); // keep a positive value to avoid weird node ids
                nodeId = String.valueOf(l);
                if (enabled) {
                    log.warn("Missing cluster node id configuration, please define it explicitly. "
                            + "Using random cluster node id instead: {}", nodeId);
                } else {
                    log.info("Using random cluster node id: {}", nodeId);
                }
            }
        }
        super.start(context);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }

    /** Allows tests to set the node id without a reload. */
    public void setNodeId(String nodeId) {
        if (!Framework.isTestModeSet()) {
            throw new UnsupportedOperationException("test mode only");
        }
        this.nodeId = nodeId;
    }

    @Override
    public void runAtomically(String key, Duration duration, Duration pollDelay, Runnable runnable) {
        if (!isEnabled()) {
            runnable.run();
            return;
        }
        new ClusterLockHelper(getNodeId(), duration, pollDelay).runAtomically(key, runnable);
    }

    public static class ClusterLockHelper {

        private static final Logger log = LogManager.getLogger(ClusterLockHelper.class);

        public static final String KV_STORE_NAME = "cluster";

        // TTL set on the lock, to make it expire if the process crashes or gets stuck
        // this is a multiplier of the duration during which we attempt to acquire the lock
        private static final int TTL_MULTIPLIER = 10;

        protected final String nodeId;

        protected final Duration duration;

        protected final Duration pollDelay;

        protected final KeyValueStore kvStore;

        public ClusterLockHelper(String nodeId, Duration duration, Duration pollDelay) {
            this.nodeId = nodeId;
            this.duration = duration;
            this.pollDelay = pollDelay;
            kvStore = Framework.getService(KeyValueService.class).getKeyValueStore(KV_STORE_NAME);
        }

        /**
         * Runs a {@link Runnable} atomically in a cluster-wide critical section, outside a transaction.
         */
        public void runAtomically(String key, Runnable runnable) {
            runInSeparateTransaction(() -> runAtomicallyInternal(key, runnable));
        }

        /**
         * Runs a {@link Runnable} outside the current transaction (committing and finally restarting it if needed).
         *
         * @implSpec this is different from {@link TransactionHelper#runWithoutTransaction(Runnable)} because that one,
         *           in some implementations, may keep the current transaction and start the runnable in a new thread.
         *           Here we don't want a new thread or a risk of deadlock, so we just commit the original transaction.
         */
        protected void runInSeparateTransaction(Runnable runnable) {
            // check if there is a current transaction, before committing it
            boolean transaction = TransactionHelper.isTransactionActiveOrMarkedRollback();
            if (transaction) {
                TransactionHelper.commitOrRollbackTransaction();
            }
            boolean completedAbruptly = true;
            try {
                if (transaction) {
                    TransactionHelper.runInTransaction(runnable);
                } else {
                    runnable.run();
                }
                completedAbruptly = false;
            } finally {
                if (transaction) {
                    // restart a transaction if there was one originally
                    try {
                        TransactionHelper.startTransaction();
                    } finally {
                        if (completedAbruptly) {
                            // mark rollback-only if there was an exception
                            TransactionHelper.setTransactionRollbackOnly();
                        }
                    }
                }
            }
        }

        /**
         * Runs a {@link Runnable} atomically, in a cluster-wide critical section.
         */
        protected void runAtomicallyInternal(String key, Runnable runnable) {
            String lockInfo = tryLock(key);
            if (lockInfo != null) {
                try {
                    runnable.run();
                } finally {
                    unLock(key, lockInfo);
                }
            } else {
                throw new RuntimeServiceException("Failed to acquire lock '" + key + "' after " + duration.toSeconds()
                        + "s, owner: " + getLock(key));
            }
        }

        // try to acquire the lock and fail if it takes too long
        protected String tryLock(String key) {
            log.debug("Trying to lock '{}'", key);
            long deadline = System.nanoTime() + duration.toNanos();
            long ttl = duration.multipliedBy(TTL_MULTIPLIER).toSeconds();
            do {
                // try to acquire the lock
                String lockInfo = "node=" + nodeId + " time=" + Instant.now();
                if (kvStore.compareAndSet(key, null, lockInfo, ttl)) {
                    // lock acquired
                    log.debug("Lock '{}' acquired after {}ms", () -> key,
                            () -> (System.nanoTime() - (deadline - duration.toNanos())) / 1_000_000);
                    return lockInfo;
                }
                // wait a bit before retrying
                log.debug("  Sleeping on busy lock '{}' for {}ms", () -> key, pollDelay::toMillis);
                try {
                    Thread.sleep(pollDelay.toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeServiceException(e);
                }
            } while (System.nanoTime() < deadline);
            log.debug("Failed to acquire lock '{}' after {}s", () -> key, duration::toSeconds);
            return null;
        }

        protected void unLock(String key, String lockInfo) {
            log.debug("Unlocking '{}'", key);
            if (kvStore.compareAndSet(key, lockInfo, null)) {
                return;
            }
            // couldn't remove the lock, it expired an may have been reacquired
            String current = kvStore.getString(key);
            if (current == null) {
                // lock expired but was not reacquired
                log.warn("Unlocking '{}' but the lock had already expired; "
                        + "consider increasing the try duration for this lock", key);
            } else {
                // lock expired and was reacquired
                log.error("Failed to unlock '{}', the lock expired and has a new owner: {}; "
                        + "consider increasing the try duration for this lock", key, getLock(key));
            }
        }

        protected String getLock(String key) {
            return kvStore.getString(key);
        }
    }

}
