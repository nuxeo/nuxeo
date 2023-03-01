/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.blob;

import java.util.HashSet;
import java.util.Set;

import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.blob.binary.BinaryManagerStatus;
import org.nuxeo.ecm.core.blob.binary.CachingBinaryManager;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 7.10
 */
public abstract class AbstractBinaryGarbageCollector<T extends CachingBinaryManager> implements BinaryGarbageCollector {

    protected T binaryManager;

    protected BinaryManagerStatus status;

    protected volatile long startTime;

    // kept for backward compat with old subclasses implementations
    protected Set<String> marked;

    // new implementations must use this instead
    protected Set<String> toDelete;

    protected AbstractBinaryGarbageCollector(T binaryManager) {
        this.binaryManager = binaryManager;
    }

    @Override
    public void start() {
        if (startTime != 0) {
            throw new RuntimeException("Already started");
        }
        startTime = System.currentTimeMillis();
        status = new BinaryManagerStatus();
        marked = null;
        computeToDelete();
        if (marked == null && toDelete == null) {
            throw new IllegalStateException("New class must define 'toDelete'");
        }
        if (marked != null && toDelete != null) {
            throw new IllegalStateException("New class must not define 'marked'");
        }

        // XXX : we should be able to do better
        // and only remove the cache entry that will be removed from S3
        binaryManager.fileCache.clear();
    }

    /**
     * Computes keys candidate for deletion.
     * <p>
     * Overrides should not call super (this allows detecting old implementations).
     *
     * @since 11.5
     */
    public void computeToDelete() {
        // new implementations shouldn't do this, just keep marked null and define toDelete
        marked = new HashSet<>();
        toDelete = null;
    }

    @Override
    public void stop(boolean delete) {
        if (startTime == 0) {
            throw new RuntimeException("Not started");
        }
        try {
            removeUnmarkedBlobsAndUpdateStatus(delete);
        } finally {
            marked = null;
            toDelete = null;
            status.gcDuration = System.currentTimeMillis() - startTime;
            startTime = 0;
        }
    }

    @Override
    public void reset() {
        startTime = 0;
    }

    protected void removeUnmarkedBlobsAndUpdateStatus(boolean delete) {
        // kept for backward compat, new implementations should override everything
        Set<String> unmarked = getUnmarkedBlobs();
        if (delete) {
            binaryManager.removeBinaries(unmarked);
        }
    }

    public Set<String> getUnmarkedBlobs() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void mark(String digest) {
        if (marked != null) {
            marked.add(digest);
        } else {
            toDelete.remove(digest);
        }
    }

    @Override
    public BinaryManagerStatus getStatus() {
        return status;
    }

    @Override
    public boolean isInProgress() {
        // volatile as this is designed to be called from another thread
        return startTime != 0;
    }
}
