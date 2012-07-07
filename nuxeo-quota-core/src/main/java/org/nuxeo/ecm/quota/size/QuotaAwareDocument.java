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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

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
                    "canNotSetQuotaToALowerValueThanCurrentSize");
        }
        doc.setPropertyValue(DOCUMENTS_SIZE_MAX_SIZE_PROPERTY, maxSize);
        if (save) {
            save();
        }
    }

    public QuotaInfo getQuotaInfo() {
        return new QuotaInfo(getInnerSize(), getTotalSize(), getMaxQuota());
    }
}
