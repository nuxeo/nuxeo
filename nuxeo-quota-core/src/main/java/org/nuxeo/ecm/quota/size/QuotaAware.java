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

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Interface to manage DocumentModel that supports Quotas
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.6
 */
public interface QuotaAware {

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
}
