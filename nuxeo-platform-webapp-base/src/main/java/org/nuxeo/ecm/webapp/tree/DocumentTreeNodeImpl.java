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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.Sorter;
import org.nuxeo.ecm.core.api.quota.QuotaStats;
import org.nuxeo.ecm.core.api.quota.QuotaStatsNonFolderishCount;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.runtime.api.Framework;

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

    protected final Filter filter;

    protected final Filter leafFilter;

    protected final Sorter sorter;

    protected final String pageProviderName;

    protected ContentView orderableContentView;

    protected Map<Object, DocumentTreeNodeImpl> children;

    protected boolean expanded = false;

    public DocumentTreeNodeImpl(DocumentModel document, Filter filter,
            Filter leafFilter, Sorter sorter, String pageProviderName) {
        this.document = document;
        this.filter = filter;
        this.leafFilter = leafFilter;
        this.sorter = sorter;
        this.pageProviderName = pageProviderName;
    }

    /**
     * @deprecated since 5.9.1, sessionId not used.
     */
    @Deprecated
    public DocumentTreeNodeImpl(String sessionId, DocumentModel document,
            Filter filter, Filter leafFilter, Sorter sorter,
            String pageProviderName) {
        this(document, filter, leafFilter, sorter, pageProviderName);
    }

    /**
     * @deprecated since 5.9.1, sessionId not used.
     */
    @Deprecated
    public DocumentTreeNodeImpl(String sessionId, DocumentModel document,
            Filter filter, Sorter sorter) {
        this(document, filter, null, sorter, null);
    }

    public DocumentTreeNodeImpl(DocumentModel document, Filter filter,
            Sorter sorter) {
        this(document, filter, null, sorter, (String) null);
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

    public boolean isSelected(DocumentModel currentDocument) {
        if (currentDocument != null) {
            if (!currentDocument.isFolder()) {
                // check if it's the closest parent
                String currentPath = currentDocument.getPathAsString();
                String nodePath = getPath();
                if (currentPath != null
                        && nodePath != null
                        && currentPath.startsWith(nodePath)
                        && currentPath.length() > nodePath.length()
                        && !currentPath.substring(nodePath.length() + 1).contains(
                                "/")) {
                    // direct parent
                    return true;
                }
            } else {
                // check equality
                return currentDocument.getId().equals(getId());
            }
        }
        return false;
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

    @SuppressWarnings("unchecked")
    public void fetchChildren() {
        try {
            children = new LinkedHashMap<Object, DocumentTreeNodeImpl>();
            if (leafFilter != null && leafFilter.accept(document)) {
                // filter says this is a leaf, don't look at children
                return;
            }
            CoreSession session = getCoreSession();
            if (session == null) {
                log.error("Cannot retrieve CoreSession for " + document);
                return;
            }
            List<DocumentModel> documents;
            boolean isOrderable = document.hasFacet(FacetNames.ORDERABLE);
            if (pageProviderName == null) {
                // get the children using the core
                Sorter sorterToUse = isOrderable ? null : sorter;
                documents = session.getChildren(document.getRef(), null,
                        SecurityConstants.READ, filter, sorterToUse);
            } else {
                // use page providers
                try {
                    PageProviderService ppService = Framework.getService(PageProviderService.class);
                    List<SortInfo> sortInfos = null;
                    Map<String, Serializable> props = new HashMap<String, Serializable>();
                    props.put(
                            CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY,
                            (Serializable) session);
                    if (isOrderable) {
                        // override sort infos to get default sort
                        sortInfos = new ArrayList<SortInfo>();
                        sortInfos.add(new SortInfo("ecm:pos", true));
                    }
                    PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) ppService.getPageProvider(
                            pageProviderName, sortInfos, null, null, props,
                            new Object[] { getId() });
                    documents = pp.getCurrentPage();
                    documents = filterAndSort(documents, !isOrderable);
                } catch (Exception e) {
                    throw new ClientException(e);
                }
            }
            // build the children nodes
            for (DocumentModel child : documents) {
                String identifier = child.getId();
                DocumentTreeNodeImpl childNode;
                childNode = new DocumentTreeNodeImpl(child, filter, leafFilter,
                        sorter, pageProviderName);
                children.put(identifier, childNode);
            }
        } catch (ClientException e) {
            log.error(e);
        }
    }

    protected CoreSession getCoreSession() {
        CoreSession session = document.getCoreSession();
        if (session == null) {
            session = (CoreSession) Component.getInstance("documentManager",
                    ScopeType.CONVERSATION);
        }
        return session;
    }

    protected List<DocumentModel> filterAndSort(List<DocumentModel> docs,
            boolean doSort) {
        // filter and sort if defined
        List<DocumentModel> res = new ArrayList<DocumentModel>();
        if (docs != null) {
            if (filter == null) {
                res.addAll(docs);
            } else {
                for (DocumentModel doc : docs) {
                    if (filter.accept(doc)) {
                        res.add(doc);
                    }
                }
            }
        }
        if (sorter != null && doSort) {
            Collections.sort(res, sorter);
        }
        return res;
    }

    @Override
    public QuotaStats getQuotaStats() {
        return document != null ? document.getAdapter(QuotaStatsNonFolderishCount.class)
                : null;
    }

    @Override
    public boolean isExpanded() {
        // TODO: make sure than calling treeActionBean for each node is
        // costless
        final TreeActionsBean treeActionBean = (TreeActionsBean) Component.getInstance("treeActions");
        if (!treeActionBean.isNodeExpandEvent()) {
            String currentDocPath = treeActionBean.getCurrentDocumentPath();
            if (currentDocPath != null && getPath() != null
                    && currentDocPath.startsWith(getPath())) {
                // additional slower check for strict path prefix
                if ((currentDocPath + '/').startsWith(getPath() + '/')
                        || "/".equals(getPath())) {
                    expanded = true;
                }
            }
        }
        return expanded;
    }

    @Override
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }
}
