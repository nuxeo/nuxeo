package org.nuxeo.ecm.quota.size;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

public class QuotaAwareDocument implements QuotaAware {

    public static final String DOCUMENTS_SIZE_STATISTICS_FACET = "DocumentsSizeStatistics";

    public static final String DOCUMENTS_SIZE_INNER_SIZE_PROPERTY = "dss:innerSize";

    public static final String DOCUMENTS_SIZE_TOTAL_SIZE_PROPERTY = "dss:totalSize";

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
            Long inner = (Long) doc.getPropertyValue(DOCUMENTS_SIZE_INNER_SIZE_PROPERTY);
            return inner != null ? inner : 0;
        } catch (ClientException e) {
            return 0;
        }
    }

    @Override
    public long getTotalSize() {
        try {
            Long total = (Long) doc.getPropertyValue(DOCUMENTS_SIZE_TOTAL_SIZE_PROPERTY);
            return total != null ? total : 0;
        } catch (ClientException e) {
            return 0;
        }
    }

    @Override
    public void addInnerSize(long additionalSize, boolean save)
            throws ClientException {
        Long inner = getInnerSize() + additionalSize;
        Long total = getTotalSize() + additionalSize;
        doc.setPropertyValue(DOCUMENTS_SIZE_INNER_SIZE_PROPERTY, inner);
        doc.setPropertyValue(DOCUMENTS_SIZE_TOTAL_SIZE_PROPERTY, total);
        if (save) {
            save();
        }
    }

    @Override
    public void addTotalSize(long additionalSize, boolean save)
            throws ClientException {
        Long total = getTotalSize() + additionalSize;
        doc.setPropertyValue(DOCUMENTS_SIZE_TOTAL_SIZE_PROPERTY, total);
        if (save) {
            save();
        }
    }

    public void save() throws ClientException {
        doc.getContextData().putScopedValue(ScopeType.REQUEST,
                QuotaSyncListenerChecker.DISABLE_QUOTA_CHECK_LISTENER, true);
        doc = doc.getCoreSession().saveDocument(doc);
        log.debug("Saving quota doc on session "
                + doc.getCoreSession().getSessionId());
        doc.getCoreSession().save();
    }

    @Override
    public long getMaxQuota() {
        try {
            Long count = (Long) doc.getPropertyValue(DOCUMENTS_SIZE_MAX_SIZE_PROPERTY);
            return count != null ? count : -1;
        } catch (ClientException e) {
            return -1;
        }
    }

    public void setMaxQuota(long maxSize, boolean save) throws ClientException {
        long existingTotal = getTotalSize();
        if (existingTotal > maxSize && maxSize > 0) {
            throw new QuotaExceededException(doc,
                    "Can not set the quota to a lower value that the current size");
        }
        doc.setPropertyValue(DOCUMENTS_SIZE_MAX_SIZE_PROPERTY, maxSize);
        if (save) {
            save();
        }
    }

}
