/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Olivier Grisel
 *     Georges Racinet
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.search.api.client.querymodel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.search.api.client.querymodel.descriptor.FieldDescriptor;
import org.nuxeo.ecm.core.search.api.client.querymodel.descriptor.QueryModelDescriptor;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultItem;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;
import org.nuxeo.ecm.core.search.api.client.search.results.document.SearchPageProvider;
import org.nuxeo.ecm.core.search.api.client.search.results.impl.DocumentModelResultItem;
import org.nuxeo.ecm.core.search.api.client.search.results.impl.ResultSetImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Query model maintaining the context about a query descriptor, and for
 * stateful models a document containing parameters that can be used by the
 * model.
 *
 * @author Olivier Grisel
 * @author Georges Racinet
 * @author Florent Guillaume
 * @deprecated use ContentView instances in conjunction with
 *             PageProvider instead.
 */
@Deprecated
public class QueryModel implements Serializable {

    private static final long serialVersionUID = 762348097532723566L;

    private static final Log log = LogFactory.getLog(QueryModel.class);

    protected transient QueryModelDescriptor descriptor;

    protected String descriptorName;

    protected int max;

    protected final DocumentModel documentModel;

    protected final DocumentModel originalDocumentModel;

    protected boolean isPersisted = false;

    protected transient QueryModelService queryModelService;

    protected Boolean detachResultsFlag;

    public QueryModel(QueryModelDescriptor descriptor,
            DocumentModel documentModel) {
        this.descriptor = descriptor;
        if (descriptor != null) {
            descriptorName = descriptor.getName();
            max = descriptor.getMax() == null ? 0
                    : descriptor.getMax().intValue();
        }

        this.documentModel = documentModel;
        if (documentModel == null) {
            originalDocumentModel = null;
        } else {
            // detach and keep a copy of the original to be able to reset
            try {
                ((DocumentModelImpl) documentModel).detach(true);
            } catch (ClientException e) {
                throw new ClientRuntimeException(e);
            }
            originalDocumentModel = new DocumentModelImpl(
                    documentModel.getType());
            try {
                originalDocumentModel.copyContent(documentModel);
            } catch (ClientException e) {
                throw new ClientRuntimeException(e);
            }
        }
    }

    public QueryModel(QueryModelDescriptor descriptor) {
        this(descriptor, null);
    }

    public boolean detachResults() {
        if (detachResultsFlag == null) {
            detachResultsFlag = Boolean.valueOf(Framework.getProperty(
                    ResultSet.ALWAYS_DETACH_SEARCH_RESULTS_KEY, "false"));
        }
        return detachResultsFlag.booleanValue();
    }

    public boolean isPersisted() {
        return isPersisted;
    }

    public void setPersisted(boolean isPersisted) {
        this.isPersisted = isPersisted;
    }

    public DocumentModel getDocumentModel() {
        return documentModel;
    }

    public DocumentModelList getDocuments(CoreSession session)
            throws ClientException {
        return getDocuments(session, null);
    }

    public DocumentModelList getDocuments(CoreSession session, Object[] params)
            throws ClientException {
        return getResultsProvider(session, params).getCurrentPage();
    }

    /**
     * Used to reconstruct the descriptor after a ser/de-serialization cycle
     */
    private void checkDescriptor() {
        if (descriptor == null) {
            if (queryModelService == null) {
                queryModelService = (QueryModelService) Framework.getRuntime().getComponent(
                        QueryModelService.NAME);
            }
            descriptor = queryModelService.getQueryModelDescriptor(descriptorName);
        }
    }

    public PagedDocumentsProvider getResultsProvider(CoreSession session,
            Object[] params) throws ClientException {
        return getResultsProvider(session, params, null);
    }

    public PagedDocumentsProvider getResultsProvider(CoreSession session,
            Object[] params, SortInfo sortInfo) throws ClientException {
        checkDescriptor();

        if (sortInfo == null) {
            sortInfo = descriptor.getDefaultSortInfo(documentModel);
        }

        String query;
        if (descriptor.isStateful()) {
            query = descriptor.getQuery(documentModel, sortInfo);
        } else {
            query = descriptor.getQuery(params, sortInfo);
        }

        if (log.isDebugEnabled()) {
            log.debug("execute query: " + query.replace('\n', ' '));
        }

        DocumentModelList documentModelList = session.query(query, null, max,
                0, true);
        int size = documentModelList.size();
        List<ResultItem> resultItems = new ArrayList<ResultItem>(size);
        for (DocumentModel doc : documentModelList) {
            if (doc == null) {
                log.error("Got null document from query: " + query);
                continue;
            }
            if (detachResults()) {
                // detach the document so that we can use it beyond the session
                try {
                    ((DocumentModelImpl) doc).detach(true);
                } catch (DocumentSecurityException e) {
                    // no access to the document (why?)
                    continue;
                }
            }
            resultItems.add(new DocumentModelResultItem(doc));
        }
        ResultSet resultSet = new ResultSetImpl(query, session, 0, max,
                resultItems, (int) documentModelList.totalSize(), size);
        return new SearchPageProvider(resultSet, isSortable(), sortInfo, query);
    }

    public QueryModelDescriptor getDescriptor() {
        checkDescriptor();
        return descriptor;
    }

    /*
     * Convenience API
     */

    public Object getProperty(String schemaName, String name) {
        try {
            return documentModel.getProperty(schemaName, name);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    public void setProperty(String schemaName, String name, Object value) {
        try {
            documentModel.setProperty(schemaName, name, value);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    public Object getPropertyValue(String xpath) {
        try {
            return documentModel.getPropertyValue(xpath);
        } catch (PropertyException e) {
            throw new ClientRuntimeException(e);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    public void setPropertyValue(String xpath, Serializable value) {
        try {
            documentModel.setPropertyValue(xpath, value);
        } catch (PropertyException e) {
            throw new ClientRuntimeException(e);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    public void setSortColumn(String value) {
        FieldDescriptor fd = getDescriptor().getSortColumnField();
        if (fd.getXpath() != null) {
            setPropertyValue(fd.getXpath(), value);
        } else {
            setProperty(fd.getSchema(), fd.getName(), value);
        }
    }

    public String getSortColumn() {
        FieldDescriptor fd = getDescriptor().getSortColumnField();
        if (fd.getXpath() != null) {
            return (String) getPropertyValue(fd.getXpath());
        } else {
            return (String) getProperty(fd.getSchema(), fd.getName());
        }
    }

    public boolean getSortAscending() {
        FieldDescriptor fd = getDescriptor().getSortAscendingField();
        Boolean result;
        if (fd.getXpath() != null) {
            result = (Boolean) getPropertyValue(fd.getXpath());
        } else {
            result = (Boolean) getProperty(fd.getSchema(), fd.getName());
        }
        return Boolean.TRUE.equals(result);
    }

    public void setSortAscending(boolean sortAscending) {
        FieldDescriptor fd = getDescriptor().getSortAscendingField();
        if (fd.getXpath() != null) {
            setPropertyValue(fd.getXpath(), Boolean.valueOf(sortAscending));
        } else {
            setProperty(fd.getSchema(), fd.getName(),
                    Boolean.valueOf(sortAscending));
        }
    }

    public boolean isSortable() {
        return getDescriptor().isSortable();
    }

    public void reset() {
        try {
            documentModel.copyContent(originalDocumentModel);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

}
