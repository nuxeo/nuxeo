/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.kv;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Helper to run a {@link Runnable} atomically in a cluster-wide critical section, outside a transaction.
 *
 * @since 11.1
 */
public class ClusterLockHelper {

    private static final Logger log = LogManager.getLogger(ClusterLockHelper.class);

    public static final String CLUSTERING_ENABLED_PROP = "repository.clustering.enabled";

    public static final String NODE_ID_PROP = "repository.clustering.id";

    public static final String KV_STORE_NAME = "cluster";

    protected static final Random RANDOM = new Random(); // NOSONAR (doesn't need cryptographic strength)

    // TTL set on the lock, to make it expire if the process crashes or gets stuck
    // this is a multiplier of the duration during which we attempt to acquire the lock
    private static final int TTL_MULTIPLIER = 10;

    protected final String nodeId;

    protected final Duration duration;

    protected final Duration pollDelay;

    protected final KeyValueStore kvStore;

    /**
     * Runs a {@link Runnable} atomically in a cluster-wide critical section, outside a transaction.
     *
     * @param key the key used to determine atomicity
     * @param duration the duration during which we attempt to acquire the lock
     * @param pollDelay the delay between two subsequent polls of the lock
     * @param runnable the runnable
     * @throws RuntimeServiceException if locking failed
     */
    public static void runAtomically(String key, Duration duration, Duration pollDelay, Runnable runnable) {
        if (!Framework.isBooleanPropertyTrue(CLUSTERING_ENABLED_PROP)) {
            runnable.run();
            return;
        }
        new ClusterLockHelper(getNodeId(), duration, pollDelay).runAtomically(key, runnable);
    }

    protected static String getNodeId() {
        String nodeId = Framework.getProperty(NODE_ID_PROP);
        if (isBlank(nodeId)) {
            return String.valueOf(RANDOM.nextLong());
        }
        return nodeId.trim();
    }

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
     * @implSpec this is different from {@link TransactionHelper#runWithoutTransaction(Runnable)} because that one, in
     *           some implementations, may keep the current transaction and start the runnable in a new thread. Here we
     *           don't want a new thread or a risk of deadlock, so we just commit the original transaction.
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
            throw new RuntimeServiceException("Failed to acquire lock '" + key + "' after " + duration.getSeconds()
                    + "s, owner: " + getLock(key));
        }
    }

    // try to acquire the lock and fail if it takes too long
    protected String tryLock(String key) {
        log.debug("Trying to lock '{}'", key);
        long deadline = System.nanoTime() + duration.toNanos();
        long ttl = duration.multipliedBy(TTL_MULTIPLIER).getSeconds();
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
        log.debug("Failed to acquire lock '{}' after {}s", () -> key, duration::getSeconds);
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
