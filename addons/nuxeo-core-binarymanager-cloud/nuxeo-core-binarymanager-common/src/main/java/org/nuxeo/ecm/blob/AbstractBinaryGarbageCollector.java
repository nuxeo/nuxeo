/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
