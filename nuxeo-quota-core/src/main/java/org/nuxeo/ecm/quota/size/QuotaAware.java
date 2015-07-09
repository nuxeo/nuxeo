/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */

package org.nuxeo.ecm.quota.size;

import java.io.IOException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Interface to manage DocumentModel that supports Quotas
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.6
 */
public interface QuotaAware {

    public static final String QUOTA_TOTALSIZE_CACHE_NAME = "quota-totalsize-cache";

    long getInnerSize();

    long getTotalSize();

    long getTrashSize();

    long getVersionsSize();

    long getMaxQuota();

    void setInnerSize(long size, boolean save);

    void addInnerSize(long additionalSize, boolean save);

    void addTotalSize(long additionalSize, boolean save);

    void addTrashSize(long additionalSize, boolean save);

    void addVersionsSize(long additionalSize, boolean save);

    void save();

    /**
     * @since 5.7 allows to save the document without notifying DublincoreListener and the notification service
     */
    void save(boolean disableNotifications);

    DocumentModel getDoc();

    void setMaxQuota(long maxSize, boolean save);

    void setMaxQuota(long maxSize, boolean save, boolean skipValidation);

    QuotaInfo getQuotaInfo();

    void resetInfos(boolean save);

    /**
     * Invalidates "total size" key-value in cache if exists.
     * 
     * @throws IOException when unable to invalidate key-value
     * @since 6.0-HF16, 7.4
     */
    void invalidateTotalSizeCache() throws IOException;
    
    /**
     * Returns value of "total size" cache of document OR <code>null</code> if cache does not exist or key does not exist in cache.
     * 
     * @return <code>Long</code> object or <code>null</code>
     * @since 6.0-HF16, 7.4
     */
    Long getTotalSizeCache() throws IOException;
    
    /**
     * Stores "total size" value in cache if it exists.
     * 
     * @param size
     * @throws IOException if unable to store value
     * @since 6.0-HF16, 7.4
     */
    void putTotalSizeCache(long size) throws IOException;
    
    /**
     * @return <code>true</code> if "total size" cache exists otherwise <code>false</code>.
     * 
     * @since 6.0-HF16, 7.4
     */
    boolean totalSizeCacheExists();
}
