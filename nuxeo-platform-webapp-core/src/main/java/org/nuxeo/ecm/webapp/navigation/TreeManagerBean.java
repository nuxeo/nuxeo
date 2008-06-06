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

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;
import java.util.Arrays;
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
import org.nuxeo.ecm.core.api.security.SecurityConstants;
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
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
@Scope(CONVERSATION)
@Name("treeManager")
@Install(precedence = FRAMEWORK)
public class TreeManagerBean extends InputController implements TreeManager,
        Serializable {

    private static final long serialVersionUID = 4773510417160248991L;

    private static final Log log = LogFactory.getLog(TreeManagerBean.class);

    protected boolean initialized = false;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    protected LazyTreeModel treeModel;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    protected transient AbstractCacheListener cacheListener;

    // flag to know when it is necessary to expand the tree to the current
    // document before rendering the tree
    protected boolean isTreeSyncedWithCurrentDocument = false;

    public void initialize() throws ClientException {
        try {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            DocumentModel firstAccessibleParent = getFirstAccessibleParent(currentDocument);
            if (typesTool != null && firstAccessibleParent != null) {
                LazyTreeNode treeNode = new LazyTreeNode(firstAccessibleParent,
                        documentManager, TreeManagerService.getDocumentFilter());

                treeModel = new LazyTreeModel(treeNode);

                if (null != navigationContext.getCurrentDomain()) {
                    logDocumentWithTitle("Tree initialized with root: ",
                            navigationContext.getCurrentDomain());
                }
                initialized = true;
            } else {
                log.error("Could not initialize the navigation tree");
            }
        } catch (Throwable t) {
            throw EJBExceptionHandler.wrapException(t);
        }

        // also init cache listener
        registerCacheListener();
    }

    @Observer(value = { EventNames.GO_HOME,
            EventNames.DOMAIN_SELECTION_CHANGED, EventNames.DOCUMENT_CHANGED,
            EventNames.DOCUMENT_SECURITY_CHANGED }, create = false)
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
        if (!isInitialized()) {
            initialize();
        }
        if (isInitialized() && !isTreeSyncedWithCurrentDocument) {
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
     * Expands and updates the path to the path to the newly selected document.
     */
    public void expandToCurrentTreeNode() throws ClientException {
        try {
            DocumentModel docModel = navigationContext.getCurrentDocument();
            if (null != docModel && null != treeModel) {
                if (docModel.equals(documentManager.getRootDocument())) {
                    treeModel = null;
                } else {
                    DocumentFilterImpl documentFilter = (DocumentFilterImpl) TreeManagerService.getDocumentFilter();
                    LazyTreeNode foundNode = null;

                    // Fix for NXP-1735 and NXP-1846
                    // If showFiles feature is disabled then the tree have to be
                    // kept expanded on parent's node
                    if (documentFilter.showFiles) {
                        foundNode = findNode(docModel, true);
                    } else {
                        if (docModel.isFolder()) {
                            foundNode = findNode(docModel, true);
                        } else {
                            if (!documentManager.hasPermission(
                                    docModel.getParentRef(),
                                    SecurityConstants.READ)) {
                                // current user doesn't have the right to the
                                // parent document.
                                // reset the tree so it won't be shown on the
                                // page and let him access the document.
                                reset();
                            } else {
                                foundNode = findNode(
                                        documentManager.getDocument(docModel.getParentRef()),
                                        true);
                            }
                        }
                    }

                    if (foundNode != null) {
                        isTreeSyncedWithCurrentDocument = true;
                    } else {
                        reset();
                        initialize();
                    }
                }
            } else {
                reset();
            }
        } catch (Throwable t) {
            throw EJBExceptionHandler.wrapException(t);
        }
    }

    @SuppressWarnings("unchecked")
    private LazyTreeNode findNode(DocumentModel model, boolean doExpand)
            throws ClientException {
        DocumentRef docRef = model.getRef();
        List<DocumentModel> ancestors = documentManager.getParentDocuments(docRef);

        TreeState state = treeModel.getTreeState();
        LazyTreeNode node = getRoot();

        List<LazyTreeNode> children = Arrays.asList(node);
        boolean rootFoundInAncestors = false;

        for (DocumentModel ancestor : ancestors) {

            if (!rootFoundInAncestors) {
                // first find the root of the tree in one of the ancestors
                if (ancestor.getRef().equals(node.getDocumentIdentifier())) {
                    // we have synced the root of the tree with an ancestor of
                    // the target document, we shall proceed to the tree
                    // expansion
                    rootFoundInAncestors = true;
                } else {
                    // this ancestor does not match the root of the navigation
                    // tree, let us retry with its child
                    continue;
                }
            }

            LazyTreeNode matchingChild = findNodeInChildren(children,
                    ancestor.getRef(), state, doExpand);

            if (matchingChild == null) {
                if (log.isDebugEnabled()) {
                    log.debug("could not find ancestor " + ancestor.getTitle()
                            + " for " + model.getTitle());
                }
                return null;
            } else {
                node = matchingChild;
                children = node.getChildren();
            }
        }
        if (rootFoundInAncestors) {
            return node;
        } else {
            return null;
        }
    }

    private LazyTreeNode findNodeInChildren(List<LazyTreeNode> children,
            DocumentRef docRef, TreeState state, boolean doExpand) {
        for (LazyTreeNode child : children) {
            if (child.getDocumentIdentifier().equals(docRef)) {
                if (doExpand && !state.isNodeExpanded(child.nodeId)) {
                    state.toggleExpanded(child.nodeId);
                }
                return child;
            }
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
        LazyTreeNode node = findNode(navigationContext.getCurrentDocument(),
                false);
        if (node != null) {
            node.noData = true;
        }
    }

    @Observer(value = { EventNames.DOCUMENT_CHILDREN_CHANGED }, create = false)
    public void refreshTreeNodeChildren(DocumentModel targetDoc)
            throws ClientException {
        if (isInitialized()) {
            LazyTreeNode node = findNode(targetDoc, false);
            if (node != null) {
                node.noData = true;
            }
        }
        refreshTreeNodeDescription();
    }

    /**
     * Goes through all the nodes and refreshes the description of the one that
     * corresponds to the newly selected document.
     */
    // @Observer(value = EventNames.DOCUMENT_CHANGED, create = false)
    public void refreshTreeNodeDescription() throws ClientException {
        try {
            if (treeModel != null) {
                LazyTreeNode node = findNode(
                        navigationContext.getCurrentDocument(), false);
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
