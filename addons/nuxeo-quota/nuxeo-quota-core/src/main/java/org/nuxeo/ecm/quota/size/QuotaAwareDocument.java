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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.DeltaLong;
import org.nuxeo.ecm.quota.QuotaStatsService;
import org.nuxeo.ecm.quota.QuotaUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * Adapter to manage a DocumentModel that supports Quotas
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.6
 */
public class QuotaAwareDocument implements QuotaAware {

    public static final String DOCUMENTS_SIZE_STATISTICS_FACET = "DocumentsSizeStatistics";

    public static final String DOCUMENTS_SIZE_INNER_SIZE_PROPERTY = "dss:innerSize";

    public static final String DOCUMENTS_SIZE_TOTAL_SIZE_PROPERTY = "dss:totalSize";

    public static final String DOCUMENTS_SIZE_TRASH_SIZE_PROPERTY = "dss:sizeTrash";

    public static final String DOCUMENTS_SIZE_VERSIONS_SIZE_PROPERTY = "dss:sizeVersions";

    public static final String DOCUMENTS_SIZE_MAX_SIZE_PROPERTY = "dss:maxSize";

    protected DocumentModel doc;

    protected static final Log log = LogFactory.getLog(QuotaAwareDocument.class);

    public QuotaAwareDocument(DocumentModel doc) {
        this.doc = doc;
    }

    @Override
    public DocumentModel getDoc() {
        return doc;
    }

    @Override
    public long getInnerSize() {
        try {
            Number size = (Number) doc.getPropertyValue(DOCUMENTS_SIZE_INNER_SIZE_PROPERTY);
            return size == null ? 0 : size.longValue();
        } catch (PropertyException e) {
            return 0;
        }
    }

    @Override
    public long getTotalSize() {
        try {
            Number size = (Number) doc.getPropertyValue(DOCUMENTS_SIZE_TOTAL_SIZE_PROPERTY);
            return size == null ? 0 : size.longValue();
        } catch (PropertyException e) {
            return 0;
        }
    }

    @Override
    public long getTrashSize() {
        try {
            Number size = (Number) doc.getPropertyValue(DOCUMENTS_SIZE_TRASH_SIZE_PROPERTY);
            return size == null ? 0 : size.longValue();
        } catch (PropertyException e) {
            return 0;
        }
    }

    @Override
    public long getVersionsSize() {
        try {
            Number size = (Number) doc.getPropertyValue(DOCUMENTS_SIZE_VERSIONS_SIZE_PROPERTY);
            return size == null ? 0 : size.longValue();
        } catch (PropertyException e) {
            return 0;
        }
    }

    protected Number addDelta(String property, long delta) {
        Number oldValue = (Number) doc.getPropertyValue(property);
        DeltaLong newValue = DeltaLong.valueOf(oldValue, delta);
        doc.setPropertyValue(property, newValue);
        return newValue;
    }

    @Override
    public void addInnerSize(long delta) {
        Number inner = addDelta(DOCUMENTS_SIZE_INNER_SIZE_PROPERTY, delta);
        if (log.isDebugEnabled()) {
            log.debug("Setting quota (inner size) : " + inner + " on document " + doc.getId());
        }
    }

    @Override
    public void addTotalSize(long delta) {
        Number total = addDelta(DOCUMENTS_SIZE_TOTAL_SIZE_PROPERTY, delta);
        if (log.isDebugEnabled()) {
            log.debug("Setting quota (total size) : " + total + " on document " + doc.getId());
        }
    }

    @Override
    public void addTrashSize(long delta) {
        Number trash = addDelta(DOCUMENTS_SIZE_TRASH_SIZE_PROPERTY, delta);
        if (log.isDebugEnabled()) {
            log.debug("Setting quota (trash size):" + trash + " on document " + doc.getId());
        }
    }

    @Override
    public void addVersionsSize(long delta) {
        Number versions = addDelta(DOCUMENTS_SIZE_VERSIONS_SIZE_PROPERTY, delta);
        if (log.isDebugEnabled()) {
            log.debug("Setting quota (versions size): " + versions + " on document " + doc.getId());
        }
    }

    @Override
    public void setAll(long innerSize, long totalSize, long trashSize, long versionsSize) {
        doc.setPropertyValue(DOCUMENTS_SIZE_INNER_SIZE_PROPERTY, Long.valueOf(innerSize));
        doc.setPropertyValue(DOCUMENTS_SIZE_TOTAL_SIZE_PROPERTY, Long.valueOf(totalSize));
        doc.setPropertyValue(DOCUMENTS_SIZE_TRASH_SIZE_PROPERTY, Long.valueOf(trashSize));
        doc.setPropertyValue(DOCUMENTS_SIZE_VERSIONS_SIZE_PROPERTY, Long.valueOf(versionsSize));
    }

    @Override
    public void save() {
        doc.putContextData(DocumentsSizeUpdater.DISABLE_QUOTA_CHECK_LISTENER, Boolean.TRUE);
        QuotaUtils.disableListeners(doc);
        DocumentModel origDoc = doc;
        doc = doc.getCoreSession().saveDocument(doc);
        QuotaUtils.clearContextData(doc);
        QuotaUtils.clearContextData(origDoc);
    }

    @Override
    public long getMaxQuota() {
        try {
            Long size = (Long) doc.getPropertyValue(DOCUMENTS_SIZE_MAX_SIZE_PROPERTY);
            return size == null ? -1 : size.longValue();
        } catch (PropertyException e) {
            return -1;
        }
    }

    @Override
    public void setMaxQuota(long maxSize) {
        setMaxQuota(maxSize, false);
    }

    @Override
    public void setMaxQuota(long maxSize, boolean skipValidation) {
        if (!skipValidation) {
            QuotaStatsService quotaStatsService = Framework.getService(QuotaStatsService.class);
            if (!(quotaStatsService.canSetMaxQuota(maxSize, doc, doc.getCoreSession()))) {
                throw new QuotaExceededException(doc, "Can not set " + maxSize
                        + ". Quota exceeded because the quota set on one of the children.");
            }
        }
        doc.setPropertyValue(DOCUMENTS_SIZE_MAX_SIZE_PROPERTY, maxSize);
    }

    @Override
    public QuotaInfo getQuotaInfo() {
        return new QuotaInfo(getInnerSize(), getTotalSize(), getTrashSize(), getVersionsSize(), getMaxQuota());
    }

    /**
     * @since 5.7
     */
    @Override
    public void resetInfos() {
        clearInfos();
        clearMaxSize();
    }

    @Override
    public void clearInfos() {
        // we reset by setting actual values and not null, because we expect to apply deltas to those
        // and col = col + delta wouldn't work if the database had col = null
        doc.setPropertyValue(DOCUMENTS_SIZE_INNER_SIZE_PROPERTY, 0L);
        doc.setPropertyValue(DOCUMENTS_SIZE_TOTAL_SIZE_PROPERTY, 0L);
        doc.setPropertyValue(DOCUMENTS_SIZE_TRASH_SIZE_PROPERTY, 0L);
        doc.setPropertyValue(DOCUMENTS_SIZE_VERSIONS_SIZE_PROPERTY, 0L);
    }

    protected void clearMaxSize() {
        doc.setPropertyValue(DOCUMENTS_SIZE_MAX_SIZE_PROPERTY, null);
    }

}
