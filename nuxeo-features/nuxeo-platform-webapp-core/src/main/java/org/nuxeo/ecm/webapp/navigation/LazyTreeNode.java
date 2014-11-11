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
package org.nuxeo.ecm.webapp.navigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.custom.tree2.TreeNodeBase;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.ejb.EJBExceptionHandler;
import org.nuxeo.ecm.platform.ui.web.tree.LazyTreeModel;
import org.nuxeo.ecm.webapp.action.TypesTool;
import org.nuxeo.ecm.webapp.treesorter.TreeSorter;
import org.nuxeo.runtime.api.Framework;

/**
 * This represents a tree node. Knows how to load the children from backend when
 * they are needed.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
public class LazyTreeNode extends TreeNodeBase {

    private static final long serialVersionUID = 196667825998714078L;

    private static final Log log = LogFactory.getLog(LazyTreeNode.class);

    public static final String TREE_FACET_NAME = "Document";

    public static final String DEFAULT_TITLE_PROPERTY_NAME = "dc:title";

    // protected CoreSession handle;

    protected String sid;

    protected DocumentModel doc;

    protected DocumentRef documentIdentifier;

    protected String nodeId = "0";

    protected String nodeCellId = "defaultId";

    /**
     * @deprecated types tool is not useful anymore for the tree rendering
     */
    @Deprecated
    protected TypesTool typesTool;

    protected boolean noData = true;

    protected final FacetFilter facetFilter = new FacetFilter(
            "HiddenInNavigation", false);

    protected final DocumentFilter docFilter;

    public LazyTreeNode(DocumentModel document, CoreSession session,
            DocumentFilter docFilter) {
        // standard attributes
        setType(TREE_FACET_NAME);
        setDescription(getDefaultDescription(document));
        if (document != null) {
            documentIdentifier = document.getRef();
            setLeaf(document.isFolder());
        }
        if (documentIdentifier != null) {
            setIdentifier(documentIdentifier.toString());
        }

        // specific attributes

        this.doc = document;
        setHandle(session);
        setNodeCellId("nodeRef:" + getIdentifier());
        this.docFilter = docFilter;

        noData = true;
    }

    public LazyTreeNode(DocumentModel document, CoreSession session,
            DocumentFilter docFilter, boolean leaf) {
        this(document, session, docFilter);
        setLeaf(leaf);
    }

    @Deprecated
    public LazyTreeNode(String type, String description,
            DocumentRef identifier, TypesTool typesTool, CoreSession handle,
            boolean leaf, DocumentModel doc, DocumentFilter docFilter) {
        super(type, description, identifier.toString(), leaf);

        documentIdentifier = identifier;
        this.typesTool = typesTool;
        this.doc = doc;
        setHandle(handle);
        nodeCellId = "nodeRef:" + doc.getRef();
        setLeaf(leaf);
        this.docFilter = docFilter;

        noData = true;
    }

    @Deprecated
    public LazyTreeNode(TypesTool typesTool, CoreSession handle,
            DocumentModel doc, DocumentFilter docFilter) {
        this(doc.getType(), typesTool, handle, doc, docFilter);
        setNodeCellId("nodeRef:" + doc.getRef());
    }

    @Deprecated
    public LazyTreeNode(String type, TypesTool typesTool, CoreSession handle,
            DocumentModel doc, DocumentFilter docFilter) {
        super(type, (String) doc.getProperty("dublincore", "title"),
                doc.getRef().toString(), !doc.isFolder());

        documentIdentifier = doc.getRef();
        this.typesTool = typesTool;
        this.doc = doc;
        setHandle(handle);
        nodeCellId = "nodeRef:" + doc.getRef();
        noData = true;
        setLeaf(!doc.isFolder());
        this.docFilter = docFilter;
    }

    public boolean hasChildren() {
        return getChildCount() != 0;
    }

    @Deprecated
    public TypesTool getTypesTool() {
        return typesTool;
    }

    @Deprecated
    public void setTypesTool(TypesTool typesTool) {
        this.typesTool = typesTool;
    }

    public CoreSession getHandle() {
        return CoreInstance.getInstance().getSession(sid);
    }

    public void setHandle(CoreSession handle) {
        // this.handle = handle;
        sid = handle.getSessionId();
    }

    public DocumentRef getDocumentIdentifier() {
        return documentIdentifier;
    }

    public void setDocumentIdentifier(DocumentRef documentIdentifier) {
        this.documentIdentifier = documentIdentifier;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List getChildren() {
        try {
            if (noData) {
                refreshChildrenWithBackend();
                noData = false;
            }
            return super.getChildren();
        } catch (Throwable t) {
            EJBExceptionHandler.wrapException(t);
            return new ArrayList();
        }
    }

    @SuppressWarnings("unchecked")
    public List getVisibleChildren() {
        if (noData) {
            return null;
        }
        return super.getChildren();
    }

    /**
     * Retrieves again from backend the children of this node if and only if the
     * passed document is the same as the document represented by the internal
     * document identifier.
     *
     * @param currentItem
     * @throws ClientException
     * @throws SecurityException
     */
    public boolean resetNodeChildren(DocumentModel currentItem)
            throws ClientException {
        boolean nodeReset = false;
        // if the selected document reference is the same as the current node
        // reference we refresh the children of the node
        if (null != currentItem
                && currentItem.getRef().equals(documentIdentifier)) {
            refreshChildrenWithBackend();

            nodeReset = true;
            log.debug("Refreshed children for node with doc id: "
                    + documentIdentifier);
        }

        return nodeReset;
    }

    public void updateChildren() throws ClientException {
        super.getChildren().clear();
        refreshChildrenWithBackend();
        noData = false;
    }

    /**
     * Resets the description of this node if and only if the passed document is
     * the same as the document represented by the internal document identifier.
     *
     * @param currentItem
     * @throws ClientException
     */
    public boolean resetNodeDescription(DocumentModel currentItem)
            throws ClientException {
        boolean nodeReset = false;
        // if the selected document reference is the same as the current node
        // reference we refresh the children of the node
        if (null != currentItem
                && currentItem.getRef().equals(documentIdentifier)) {
            refreshDescription();
            nodeReset = true;
        }
        return nodeReset;
    }

    public static String getDefaultDescription(DocumentModel doc) {
        String desc = null;
        if (doc != null) {
            try {
                desc = (String) doc.getPropertyValue(DEFAULT_TITLE_PROPERTY_NAME);
            } catch (PropertyException e) {
                log.error(e);
            }
        }
        return desc;
    }

    public void refreshDescription() throws ClientException {
        doc = getHandle().getDocument(documentIdentifier);
        setDescription(getDefaultDescription(doc));
    }

    /**
     * Retrieves again from backend the children of this node.
     *
     * @throws ClientException
     * @throws SecurityException
     */
    @SuppressWarnings("unchecked")
    public void refreshChildrenWithBackend() throws ClientException {
        List<LazyTreeNode> children = new ArrayList<LazyTreeNode>();

        super.getChildren().clear();

        List<DocumentModel> coreChildren = getHandle().getChildren(
                documentIdentifier, null, SecurityConstants.READ, facetFilter,
                null);
        for (DocumentModel document : coreChildren) {
            String title = getDefaultDescription(document);
            if (title != null) {
                if (docFilter != null && docFilter.accept(document)) {
                    LazyTreeNode treeNode = new LazyTreeNode(document,
                            getHandle(), docFilter);
                    children.add(treeNode);
                }
            }
        }

        // sort by description e.g title
        Collections.sort(children);

        int i = 0;
        for (LazyTreeNode child : children) {
            child.nodeId = nodeId + LazyTreeModel.SEPARATOR + i;
            i++;
        }
        super.getChildren().addAll(children);
    }

    /**
     * Overrides comparison so that children are always sorted by by the
     * implementation defined in Tree Sorter Service
     */
    @Override
    public int compareTo(Object obj) {
        TreeSorter treeSorterService = null;
        try {
            treeSorterService = Framework.getService(TreeSorter.class);
        } catch (Exception e) {
            log.error("Couldn't get Tree Sorter Service", e);
            return 0;
        }

        try {
            return treeSorterService.getCompareToResult(this, obj);
        } catch (ClientException e) {
            log.error(e);
            return 0;
        }
    }

    @Override
    public int getChildCount() {
        if (noData) {
            try {
                refreshChildrenWithBackend();
                noData = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // ...
        int nbChildren = super.getChildren().size();
        if (nbChildren == 0) {
            setLeaf(true);
        }
        return nbChildren;
    }

    /**
     * Given a document type, returns either the document type or "Document".
     * <p>
     * This is a helper method that should be used to compute the node type
     * dynamically for the tree.
     *
     * @param documentType
     * @deprecated use {@link #getType()}
     * @return
     */
    @Deprecated
    protected String nodeType(String documentType) {
        String nodeType = null;

        if (typesTool.hasType(documentType)) {
            nodeType = "Document";
        }

        if (null == nodeType) {
            nodeType = documentType;
        }

        return nodeType;
    }

    public DocumentModel getDoc() {
        return doc;
    }

    public void setDoc(DocumentModel doc) {
        this.doc = doc;
    }

    public String getNodeCellId() {
        return nodeCellId;
    }

    public void setNodeCellId(String nodeCellId) {
        this.nodeCellId = nodeCellId;
    }

    public boolean isNoData() {
        return noData;
    }

    public void setNoData(boolean noData) {
        this.noData = noData;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

}
