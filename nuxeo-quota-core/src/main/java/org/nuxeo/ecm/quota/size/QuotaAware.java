package org.nuxeo.ecm.quota.size;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

public interface QuotaAware {

    long getInnerSize();

    long getTotalSize();

    void addInnerSize(long additionalSize, boolean save) throws ClientException;

    void addTotalSize(long additionalSize, boolean save) throws ClientException;

    long getMaxQuota();

    void save() throws ClientException;

    DocumentModel getDoc();
}
