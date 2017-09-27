/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

package org.nuxeo.ecm.core.api;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.transaction.TransactionHelper;

import java.util.function.Supplier;

/**
 * Utilities to work with locks
 *
 * @since 9.3
 */
public class LockHelper {

    public static final String DOCUMENT_LOCK = "document-lock";

    public static final String LOCK = "lock";

    public static final int NB_TRY = 3;

    public static final int SLEEP_DURATION = 1000;

    private LockHelper() {
    }

    /**
     * Runs a {@link Runnable} atomically, in a cluster-wide critical section.
     *
     * @param key the key used to determine atomicity
     * @param runnable the runnable
     */
    public static void doAtomically(String key, Runnable runnable) throws ConcurrentUpdateException {
        doAtomically(key, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Runs a {@link Supplier} atomically, in a cluster-wide critical section.
     *
     * @param key the key used to determine atomicity
     * @param supplier the supplier
     * @return the result of the function
     */
    public static <R> R doAtomically(String key, Supplier<R> supplier) throws ConcurrentUpdateException {

        KeyValueStore kvStore = Framework.getService(KeyValueService.class).getKeyValueStore(DOCUMENT_LOCK);

        try {
            if (tryLock(key, kvStore)) {
                try {
                    TransactionHelper.commitOrRollbackTransaction();
                    TransactionHelper.startTransaction();

                    return supplier.get();
                } finally {
                    try {
                        TransactionHelper.commitOrRollbackTransaction();
                        TransactionHelper.startTransaction();
                    } finally {
                        unlock(key, kvStore);
                    }
                }
            } else {
                throw new ConcurrentUpdateException("Failed to acquire the lock on key " + key);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NuxeoException(e);
        }
    }

    protected static boolean tryLock(String key, KeyValueStore kvStore) throws InterruptedException {
        // Try to acquire the lock and fail if it takes too long
        long sleepDuration = SLEEP_DURATION;
        for (int i = 0; i < NB_TRY; i++) {
            if (kvStore.compareAndSet(key, null, LOCK)) {
                return true;
            }
            Thread.sleep(sleepDuration);
            sleepDuration *= 2;
        }
        return false;
    }

    protected static void unlock(String key, KeyValueStore kvStore) {
        kvStore.put(key, (String) null);
    }

}
