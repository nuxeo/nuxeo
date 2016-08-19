/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.core.transientstore.work;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolderWithProperties;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.runtime.api.Framework;

/**
 * A work allowing to store a result in the {@link TransientStore}.
 *
 * @since 7.4
 */
public abstract class TransientStoreWork extends AbstractWork {

    private static final long serialVersionUID = 1L;

    public static final String STORE_NAME = "transientStoreWorkCache";

    public static final String KEY_SUFFIX = "_result";

    protected String entryKey;

    /**
     * @since 8.4
     */
    public static String computeEntryKey(String id) {
        return id + KEY_SUFFIX;
    }

    /**
     * Stores the given {@link BlobHolder} as an entry with the given {@code key} in the transient store used by the
     * {@code TransientStoreWork}.
     */
    public static void putBlobHolder(String key, BlobHolder bh) {
        getStore().putBlobs(key, bh.getBlobs());
        Map<String, Serializable> properties = bh.getProperties();
        if (properties != null) {
            getStore().putParameters(key, properties);
        }
    }

    /**
     * Returns a {@link BlobHolder} representing the entry with the given {@code key} in the transient store used by the
     * {@code TransientStoreWork} or null if the entry doesn't exist.
     */
    public static BlobHolder getBlobHolder(String key) {
        List<Blob> blobs = getStore().getBlobs(key);
        Map<String, Serializable> params = getStore().getParameters(key);
        if (blobs == null && params == null) {
            return null;
        }
        return new SimpleBlobHolderWithProperties(blobs, params);
    }

    /**
     * Returns true if a {@link BlobHolder} is stored for the given {@code key}.
     * @since 8.3
     */
    public static boolean containsBlobHolder(String key) {
        return getStore().exists(key);
    }

    public static void removeBlobHolder(String key) {
        getStore().remove(key);
    }

    protected static TransientStore getStore() {
        TransientStoreService transientStoreService = Framework.getService(TransientStoreService.class);
        return transientStoreService.getStore(STORE_NAME);
    }

    public TransientStoreWork() {
        super();
        computeEntryKey();
    }

    public TransientStoreWork(String id) {
        super(id);
        computeEntryKey();
    }

    protected void computeEntryKey() {
        entryKey = computeEntryKey(getId());
    }

    protected void putBlobHolder(BlobHolder bh) {
        putBlobHolder(entryKey, bh);
    }

    @Override
    public String getWorkInstanceResult() {
        return entryKey;
    }
}
