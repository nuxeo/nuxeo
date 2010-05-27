/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.ecm.webapp.tree;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.Sorter;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.search.api.client.querymodel.QueryModel;

/**
 * Tree node of documents.
 * <p>
 * Children are lazy-loaded from backend only when needed.
 *
 * @author Anahide Tchertchian
 */
public class DocumentTreeNodeImpl implements DocumentTreeNode {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DocumentTreeNodeImpl.class);

    protected final DocumentModel document;

    protected final String sessionId;

    protected final Filter filter;

    protected final Filter leafFilter;

    protected final Sorter sorter;

    protected final QueryModel queryModel;

    protected final QueryModel orderableQueryModel;

    protected Map<Object, DocumentTreeNodeImpl> children;

    public DocumentTreeNodeImpl(DocumentModel document, Filter filter,
            Filter leafFilter, Sorter sorter, QueryModel queryModel) {
        this(document.getSessionId(), document, filter, leafFilter, sorter,
                queryModel);
    }

    public DocumentTreeNodeImpl(DocumentModel document, Filter filter,
            Filter leafFilter, Sorter sorter, QueryModel queryModel,
            QueryModel orderableQueryModel) {
        this(document.getSessionId(), document, filter, leafFilter, sorter,
                queryModel, orderableQueryModel);
    }

    public DocumentTreeNodeImpl(String sessionId, DocumentModel document,
            Filter filter, Filter leafFilter, Sorter sorter,
            QueryModel queryModel) {
        this(sessionId, document, filter, leafFilter, sorter, queryModel, null);
    }

    public DocumentTreeNodeImpl(String sessionId, DocumentModel document,
            Filter filter, Filter leafFilter, Sorter sorter,
            QueryModel queryModel, QueryModel orderableQueryModel) {
        this.document = document;
        this.sessionId = sessionId;
        this.filter = filter;
        this.leafFilter = leafFilter;
        this.sorter = sorter;
        this.queryModel = queryModel;
        this.orderableQueryModel = orderableQueryModel;
    }

    /** @deprecated use the other constructor */
    @Deprecated
    public DocumentTreeNodeImpl(DocumentModel document, Filter filter,
            Sorter sorter) {
        this(document, filter, null, sorter, null);
    }

    public List<DocumentTreeNode> getChildren() {
        if (children == null) {
            fetchChildren();
        }
        List<DocumentTreeNode> childrenNodes = new ArrayList<DocumentTreeNode>();
        childrenNodes.addAll(children.values());
        return childrenNodes;
    }

    public DocumentModel getDocument() {
        return document;
    }

    public String getId() {
        if (document != null) {
            return document.getId();
        }
        return null;
    }

    public String getPath() {
        if (document != null) {
            return document.getPathAsString();
        }
        return null;
    }

    /**
     * Resets children map
     */
    public void resetChildren() {
        children = null;
    }

    public void fetchChildren() {
        try {
            children = new LinkedHashMap<Object, DocumentTreeNodeImpl>();
            if (leafFilter != null && leafFilter.accept(document)) {
                // filter says this is a leaf, don't look at children
                return;
            }
            CoreSession session = CoreInstance.getInstance().getSession(
                    sessionId);
            List<DocumentModel> documents;
            if (queryModel == null) {
                // get the children using the core
                Sorter sorterToUse = document.hasFacet(FacetNames.ORDERABLE) ? null
                        : sorter;
                documents = session.getChildren(document.getRef(), null,
                        SecurityConstants.READ, filter, sorterToUse);
            } else {
                // get the children using a query model
                if (document.hasFacet(FacetNames.ORDERABLE)
                        && orderableQueryModel != null) {
                    documents = orderableQueryModel.getDocuments(session,
                            new Object[] { getId() });
                } else {
                    documents = queryModel.getDocuments(session,
                            new Object[] { getId() });
                }
            }
            // build the children nodes
            for (DocumentModel child : documents) {
                String identifier = child.getId();
                DocumentTreeNodeImpl childNode = new DocumentTreeNodeImpl(
                        session.getSessionId(), child, filter, leafFilter,
                        sorter, queryModel, orderableQueryModel);
                children.put(identifier, childNode);
            }
        } catch (ClientException e) {
            log.error(e);
        }
    }

}
