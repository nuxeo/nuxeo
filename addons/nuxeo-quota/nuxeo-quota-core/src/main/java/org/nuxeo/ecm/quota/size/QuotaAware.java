/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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

    void addInnerSize(long additionalSize);

    void addTotalSize(long additionalSize);

    void addTrashSize(long additionalSize);

    void addVersionsSize(long additionalSize);

    void save();

    DocumentModel getDoc();

    void setMaxQuota(long maxSize);

    void setMaxQuota(long maxSize, boolean skipValidation);

    QuotaInfo getQuotaInfo();

    void resetInfos();

    /**
     * Resets quota info but keeps the user-specified max quota size, if present.
     *
     * @since 10.1
     */
    void clearInfos();

    /**
     * Set all quota info.
     *
     * @since 10.1
     */
    void setAll(long innerSize, long totalSize, long trashSize, long versionsSize);

}
