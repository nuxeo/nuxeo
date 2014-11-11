/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.core.convert.cache;

import java.io.File;
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

    public boolean persist(String basePath) throws Exception {
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
            CachableBlobHolder holder = new SimpleCachableBlobHolder();
            holder.load(persistPath);
            return holder;
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
