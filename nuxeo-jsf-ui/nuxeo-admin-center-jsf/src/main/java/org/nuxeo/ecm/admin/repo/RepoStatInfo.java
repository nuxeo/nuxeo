/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.admin.repo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.common.utils.Path;

/**
 * Statistics collector class
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
public class RepoStatInfo {

    private final Map<String, Long> docsPerTypes;

    private long totalBlobSize = 0;

    private long totalBlobNb = 0;

    private long lastTotalNbDocs = 0;

    private long lastTotalBlobSize = 0;

    private long lastTotalBlobNb = 0;

    private long maxDepth = 0;

    private long maxChildren;

    private long maxBlobSize;

    private long versions = 0;

    private final long t1;

    protected float speed;

    public RepoStatInfo() {
        docsPerTypes = new ConcurrentHashMap<>();
        t1 = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("\nNumber of documents    :");
        sb.append(getTotalNbDocs());

        for (String dtype : docsPerTypes.keySet()) {
            sb.append("\n   ");
            sb.append(dtype);
            sb.append(" :");
            sb.append(docsPerTypes.get(dtype));
        }
        sb.append("\nNumber of Blobs        :");
        sb.append(totalBlobNb);
        sb.append("\nSize of Blobs          :");
        sb.append(totalBlobSize);

        sb.append("\n");
        sb.append("\nMax tree depth         :");
        sb.append(maxDepth);
        sb.append("\nBiggest Folder         :");
        sb.append(maxChildren);
        sb.append(" children");
        sb.append("\nBiggest Blob           :");
        sb.append(maxBlobSize);

        return sb.toString();
    }

    public Long getDocTypeCount(String dType) {
        return docsPerTypes.get(dType);
    }

    public List<String> getDocTypes() {
        List<String> types = new ArrayList<>();
        types.addAll(docsPerTypes.keySet());
        Collections.sort(types);
        return types;
    }

    public long getVersions() {
        return versions;
    }

    public long getMaxDepth() {
        return maxDepth;
    }

    public long getMaxChildren() {
        return maxChildren;
    }

    public long getMaxBlobSize() {
        return maxBlobSize;
    }

    public Map<String, Long> getDocsPerType() {
        return docsPerTypes;
    }

    public float getSpeed() {
        return speed;
    }

    public synchronized void addDoc(String type, Path path) {
        addDoc(type, path, false);
    }

    public synchronized void addDoc(String type, Path path, boolean isVersion) {
        Long counter = docsPerTypes.get(type);
        if (path.segmentCount() > maxDepth) {
            maxDepth = path.segmentCount();
        }
        if (counter == null) {
            counter = 1L;
        } else {
            counter += 1;
        }
        docsPerTypes.put(type, counter);

        if (isVersion) {
            versions += 1;
        }
        long t2 = System.currentTimeMillis();
        long delta = t2 - t1;
        if (delta == 0)
            delta = 1;
        speed = 1000 * getTotalNbDocs() / (float) delta;
    }

    public synchronized void addBlob(long size, Path path) {
        totalBlobSize += size;
        totalBlobNb += 1;
        if (size > maxBlobSize) {
            maxBlobSize = size;
        }
    }

    public synchronized void childrenCount(long children, Path path) {
        if (children > maxChildren) {
            maxChildren = children;
        }
    }

    public long getTotalNbDocs() {
        long total = 0;
        for (String k : docsPerTypes.keySet()) {
            total += docsPerTypes.get(k);
        }
        lastTotalNbDocs = total;
        return total;
    }

    public long getTotalBlobSize() {
        lastTotalBlobSize = totalBlobSize;
        return totalBlobSize;
    }

    public long getTotalBlobNumber() {
        lastTotalBlobNb = totalBlobNb;
        return totalBlobNb;
    }

    public long getLastTotalNbDocs() {
        return lastTotalNbDocs;
    }

    public long getLastTotalBlobSize() {
        return lastTotalBlobSize;
    }

    public long getLastTotalBlobNumber() {
        return lastTotalBlobNb;
    }

}
