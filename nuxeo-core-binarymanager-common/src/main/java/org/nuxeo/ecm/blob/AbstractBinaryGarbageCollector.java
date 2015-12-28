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

    protected Set<String> marked;

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
        marked = new HashSet<>();

        // XXX : we should be able to do better
        // and only remove the cache entry that will be removed from S3
        binaryManager.fileCache.clear();
    }

    @Override
    public void stop(boolean delete) {
        if (startTime == 0) {
            throw new RuntimeException("Not started");
        }
        try {
            Set<String> unmarked = getUnmarkedBlobs();
            marked = null;

            if (delete) {
                binaryManager.removeBinaries(unmarked);
            }
        } finally {
            status.gcDuration = System.currentTimeMillis() - startTime;
            startTime = 0;
        }
    }

    public abstract Set<String> getUnmarkedBlobs();

    @Override
    public void mark(String digest) {
        marked.add(digest);
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
