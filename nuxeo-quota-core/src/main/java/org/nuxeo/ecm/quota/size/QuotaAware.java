package org.nuxeo.ecm.quota.size;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

public interface QuotaAware {

    long getInnerSize();

    long getTotalSize();

    long getMaxQuota();

    void addInnerSize(long additionalSize, boolean save) throws ClientException;

    void addTotalSize(long additionalSize, boolean save) throws ClientException;

    void save() throws ClientException;

    DocumentModel getDoc();

    void setMaxQuota(long maxSize, boolean save) throws ClientException;

    QuotaInfo getQuotaInfo();
}
