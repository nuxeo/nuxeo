/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Razvan Caraghin
 *     Bogdan Tatar
 *     Catalin Baican
 *     Anahide Tchertchian
 *     Thomas Roger
 *     Florent Guillaume
 */
package org.nuxeo.ecm.webapp.navigation;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.security.PermitAll;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remove;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.custom.tree2.TreeState;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.search.api.client.querymodel.QueryModel;
import org.nuxeo.ecm.core.search.api.client.querymodel.descriptor.QueryModelDescriptor;
import org.nuxeo.ecm.platform.cache.AbstractCacheListener;
import org.nuxeo.ecm.platform.ejb.EJBExceptionHandler;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.tree.LazyTreeModel;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.helpers.EventNames;

/**
 * Manages the tree. Performs additional work when a node is selected such as
 * saving the selection and redirectig towards the required page.
 *
 * @author Razvan Caraghin
 * @author Florent Guillaume
 */
@Scope(CONVERSATION)
@Name("treeManager")
@Install(precedence = FRAMEWORK)
public class TreeManagerBean extends InputController implements TreeManager,
        Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(TreeManagerBean.class);

    protected boolean initialized;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    protected LazyTreeModel treeModel;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    protected transient AbstractCacheListener cacheListener;

    // flag to know when it is necessary to expand the tree to the current
    // document before rendering the tree
    protected boolean isTreeSyncedWithCurrentDocument;

    public void initialize() throws ClientException {
        try {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            DocumentModel firstAccessibleParent = getFirstAccessibleParent(currentDocument);
            if (firstAccessibleParent != null) {
                QueryModelDescriptor queryModelDescriptor = TreeManagerService.getQueryModelDescriptor();
                QueryModel queryModel = queryModelDescriptor == null ? null
                        : new QueryModel(queryModelDescriptor,
                                (NuxeoPrincipal) documentManager.getPrincipal());
                LazyTreeNode treeNode = new LazyTreeNode(firstAccessibleParent,
                        documentManager,
                        TreeManagerService.getDocumentFilter(),
                        TreeManagerService.getLeafFilter(), queryModel);
                treeModel = new LazyTreeModel(treeNode);
                logDocumentWithTitle("Tree initialized with root: ",
                        firstAccessibleParent);
                initialized = true;
            } else {
                // log.error("Could not initialize the navigation tree");
            }
        } catch (Throwable t) {
            throw EJBExceptionHandler.wrapException(t);
        }
        // also init cache listener
        registerCacheListener();
    }

    @Observer(value = { EventNames.GO_HOME, EventNames.DOMAIN_SELECTION_CHANGED }, create = false)
    public void reset() {
        if (cacheUpdateNotifier != null && cacheListener != null) {
            cacheUpdateNotifier.removeCacheListener(cacheListener);
        }
        cacheListener = null;
        treeModel = null;
        initialized = false;
    }

    public boolean isInitialized() throws ClientException {
        return initialized;
    }

    protected void registerCacheListener() {
        final String logPrefix = "<registerCacheListener> ";

        log.debug(logPrefix);

        if (cacheUpdateNotifier == null) {
            log.warn("<registerCacheListener> cacheUpdateNotifier not initialized");
            return;
        }

        // register once per session
        if (cacheListener == null) {

            cacheListener = new AbstractCacheListener() {

                /**
                 * removes a document from the hashmap which contains all the
                 * references
                 */
                @Override
                public void documentRemove(DocumentModel docModel) {

                    try {
                        DocumentModel parentModel = documentManager.getDocument(docModel.getParentRef());
                        LazyTreeNode currentNode = (LazyTreeNode) getTreeModel().getNodeById(
                                docModel.getId());

                        LazyTreeNode parentNode = (LazyTreeNode) getTreeModel().getNodeById(
                                parentModel.getId());
                        parentNode.getChildren().remove(currentNode);

                        LazyTreeNode lazyCurrentNode = (LazyTreeNode) getTreeModel().getNodeById(
                                "0");
                        treeModel = new LazyTreeModel(lazyCurrentNode);

                    } catch (SecurityException e) {
                        log.error(e);
                    } catch (ClientException e) {
                        log.error(e);
                    }
                }
            };

            log.debug(logPrefix + "register cache listener.");
            cacheUpdateNotifier.addCacheListener(cacheListener);
        }
    }

    public String selectNode() throws ClientException {
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            Map<String, Object> map = facesContext.getExternalContext().getRequestMap();
            DocumentRef documentReference = ((LazyTreeNode) map.get("node")).getDocumentIdentifier();

            DocumentModel document = documentManager.getDocument(documentReference);

            return navigationContext.navigateToDocument(document);
        } catch (Throwable t) {
            throw EJBExceptionHandler.wrapException(t);
        }
    }

    @Destroy
    @Remove
    @PermitAll
    public void destroy() {
        log.debug("Removing SEAM component...");
    }

    public LazyTreeModel getTreeModel() throws ClientException {
        if (!initialized) {
            initialize();
        }
        if (initialized && !isTreeSyncedWithCurrentDocument) {
            expandToCurrentTreeNode();
        }
        return treeModel;
    }

    public LazyTreeNode getRoot() {
        return (LazyTreeNode) treeModel.getNodeById(treeModel.getTreeWalker().getRootNodeId());
    }

    @Observer(value = EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED, create = false, inject = false)
    public void invalidateSyncedState() {
        isTreeSyncedWithCurrentDocument = false;
    }

    /**
     * Expands the tree on the path to the newly selected document.
     * <p>
     * If the selected document is not visible in the tree (because it's a file
     * or because a filter hides it), then expand as much as possible.
     */
    public void expandToCurrentTreeNode() throws ClientException {
        try {
            DocumentModel doc = navigationContext.getCurrentDocument();
            if (doc == null || treeModel == null) {
                reset();
                return;
            }
            if (doc.equals(documentManager.getRootDocument())) {
                treeModel = null;
                return;
            }
            LazyTreeNode node = findClosestNode(doc, true);
            if (node == null) {
                reset();
                initialize();
                return;
            }
            isTreeSyncedWithCurrentDocument = true;
        } catch (Throwable t) {
            throw EJBExceptionHandler.wrapException(t);
        }
    }

    /**
     * Expands the tree up to a given document and returns the closest tree node
     * found.
     *
     * @return the closest tree node that could be expanded, null if the
     *         document is outside the tree.
     */
    @SuppressWarnings("unchecked")
    private LazyTreeNode findClosestNode(DocumentModel doc, boolean doExpand)
            throws ClientException {
        List<DocumentModel> ancestors = documentManager.getParentDocuments(doc.getRef());

        TreeState treeState = treeModel.getTreeState();
        LazyTreeNode node = getRoot();
        List<LazyTreeNode> children = Collections.singletonList(node);

        boolean rootFoundInAncestors = false;
        for (DocumentModel ancestor : ancestors) {
            if (!rootFoundInAncestors) {
                // first find the root of the tree in one of the ancestors
                if (!ancestor.getRef().equals(node.getDocumentIdentifier())) {
                    // this ancestor does not match the root of the navigation
                    // tree, let us retry with its child
                    continue;
                }
                // we have synced the root of the tree with an ancestor of
                // the target document, we shall proceed to the tree
                // expansion
                rootFoundInAncestors = true;
            }

            LazyTreeNode child = findNodeInChildren(ancestor.getRef(), children);
            if (child == null) {
                // child not visible in tree, return latest node found
                return node;
            }
            // expand
            if (doExpand && !treeState.isNodeExpanded(child.nodeId)) {
                treeState.toggleExpanded(child.nodeId);
            }
            // loop into children
            node = child;
            children = node.getChildren();
        }
        if (!rootFoundInAncestors) {
            return null;
        }
        // return last node expanded
        return node;
    }

    private LazyTreeNode findNodeInChildren(DocumentRef docRef,
            List<LazyTreeNode> children) {
        for (LazyTreeNode child : children) {
            if (child.getDocumentIdentifier().equals(docRef)) {
                return child;
            }
        }
        return null;
    }

    /**
     * Finds a doc's tree node, only returns exact matches.
     */
    private LazyTreeNode findNode(DocumentModel doc) throws ClientException {
        LazyTreeNode node = findClosestNode(doc, false);
        if (node != null && node.getDocumentIdentifier().equals(doc.getRef())) {
            return node;
        }
        return null;
    }

    /**
     * @deprecated use {@link #refreshTreeNodeChildren(DocumentModel)} instead
     */
    @Deprecated
    public void refreshTreeNodeChildren() throws ClientException {
        if (documentManager == null || treeModel == null) {
            return;
        }
        LazyTreeNode node = findNode(navigationContext.getCurrentDocument());
        if (node != null) {
            node.noData = true;
        }
    }

    @Observer(value = { EventNames.DOCUMENT_CHILDREN_CHANGED }, create = false)
    public void refreshTreeNodeChildren(DocumentModel targetDoc)
            throws ClientException {
        if (isInitialized()) {
            LazyTreeNode node = findNode(targetDoc);
            if (node != null) {
                node.noData = true;
            }
        }
        refreshTreeNodeDescription();
    }

    @Observer(value = { EventNames.DOCUMENT_CHANGED }, create = false)
    public void refreshCurrentNode(DocumentModel targetDoc)
            throws ClientException {
        if (isInitialized()) {
            LazyTreeNode node = findNode(targetDoc);
            if (node != null) {
                node.refreshDescription();
            }
        }
    }

    /**
     * Goes through all the tree nodes and refreshes the description of the one
     * that corresponds to the newly selected document.
     */
    // @Observer(value = EventNames.DOCUMENT_CHANGED, create = false)
    public void refreshTreeNodeDescription() throws ClientException {
        try {
            if (treeModel != null) {
                LazyTreeNode node = findNode(navigationContext.getCurrentDocument());
                if (node != null) {
                    // current document may not be in the tree
                    node.refreshDescription();
                }
            }
        } catch (Throwable t) {
            throw EJBExceptionHandler.wrapException(t);
        }
    }

    @PrePassivate
    public void saveState() {
        log.debug("PrePassivate");
        if (cacheUpdateNotifier != null && cacheListener != null) {
            cacheUpdateNotifier.removeCacheListener(cacheListener);
        }
        cacheListener = null;
    }

    @PostActivate
    public void readState() {
        log.debug("PostActivate");
        registerCacheListener();
    }

    private DocumentModel getFirstAccessibleParent(DocumentModel doc)
            throws ClientException {
        if (doc == null) {
            return null;
        }
        List<DocumentModel> parents = documentManager.getParentDocuments(doc.getRef());
        if (!parents.isEmpty()) {
            return parents.get(0);
        } else if (!doc.getType().equals("Root") && doc.isFolder()) {
            // default on current doc
            return doc;
        } else {
            return null;
        }
    }

}
