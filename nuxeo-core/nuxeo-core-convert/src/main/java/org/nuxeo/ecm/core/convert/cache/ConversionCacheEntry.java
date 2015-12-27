/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.convert.cache;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;

/**
 * Represents an Entry in the {@link ConversionService} cache system.
 * <p>
 * Manages timestamp and persistence.
 *
 * @author tiry
 */
public class ConversionCacheEntry {

    protected Date lastAccessTime;

    protected BlobHolder bh;

    protected boolean persisted = false;

    protected String persistPath;

    protected long sizeInKB = 0;

    public ConversionCacheEntry(BlobHolder bh) {
        this.bh = bh;
        updateAccessTime();
    }

    protected void updateAccessTime() {
        lastAccessTime = new Date();
    }

    public boolean persist(String basePath) throws IOException {
        if (bh instanceof CachableBlobHolder) {
            CachableBlobHolder cbh = (CachableBlobHolder) bh;
            persistPath = cbh.persist(basePath);
            if (persistPath != null) {
                sizeInKB = new File(persistPath).length() / 1024;
                persisted = true;
            }
        }
        bh = null;
        return persisted;
    }

    public void remove() {
        if (persisted && persistPath != null) {
            new File(persistPath).delete();
        }
    }

    public BlobHolder restore() {
        updateAccessTime();
        if (persisted && persistPath != null) {
            try {
                CachableBlobHolder holder = new SimpleCachableBlobHolder();
                holder.load(persistPath);
                return holder;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return null;
        }
    }

    public long getDiskSpaceUsageInKB() {
        return sizeInKB;
    }

    public Date getLastAccessedTime() {
        return lastAccessTime;
    }

}
