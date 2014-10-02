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

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.Sorter;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;
import org.richfaces.event.CollapsibleSubTableToggleEvent;

/**
 * Manages the navigation tree.
 *
 * @author Razvan Caraghin
 * @author Anahide Tchertchian
 */
@Scope(CONVERSATION)
@Name("treeActions")
@Install(precedence = FRAMEWORK)
public class TreeActionsBean implements TreeActions, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(TreeActionsBean.class);

    public static final String NODE_SELECTED_MARKER = TreeActionsBean.class.getName()
            + "_NODE_SELECTED_MARKER";

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    protected Map<String, List<DocumentTreeNode>> trees = new HashMap<String, List<DocumentTreeNode>>();

    protected String currentDocumentPath;

    @In(create = true, required = false)
    protected Boolean isUserWorkspace;

    @In(create = true, required = false)
    protected String currentPersonalWorkspacePath;

    protected String userWorkspacePath;

    // cache the path of the tree root to check if invalidation are needed when
    // bypassing interceptors
    protected String firstAccessibleParentPath;

    @In(create = true)
    protected TreeInvalidatorBean treeInvalidator;

    public List<DocumentTreeNode> getTreeRoots() throws ClientException {
        return getTreeRoots(false);
    }

    public List<DocumentTreeNode> getTreeRoots(String treeName)
            throws ClientException {
        return getTreeRoots(false, treeName);
    }

    protected List<DocumentTreeNode> getTreeRoots(boolean showRoot,
            String treeName) throws ClientException {
        return getTreeRoots(showRoot, navigationContext.getCurrentDocument(),
                treeName);
    }

    protected List<DocumentTreeNode> getTreeRoots(boolean showRoot)
            throws ClientException {
        return getTreeRoots(showRoot, navigationContext.getCurrentDocument(),
                DEFAULT_TREE_PLUGIN_NAME);
    }

    protected List<DocumentTreeNode> getTreeRoots(boolean showRoot,
            DocumentModel currentDocument) throws ClientException {
        return getTreeRoots(showRoot, currentDocument, DEFAULT_TREE_PLUGIN_NAME);
    }

    /**
     * @since 5.4
     */
    protected List<DocumentTreeNode> getTreeRoots(boolean showRoot,
            DocumentModel currentDocument, String treeName)
            throws ClientException {

        if (treeInvalidator.needsInvalidation()) {
            reset();
            treeInvalidator.invalidationDone();
        }
        if (Boolean.TRUE.equals(isUserWorkspace)) {
            userWorkspacePath = getUserWorkspacePath();
        }
        List<DocumentTreeNode> currentTree = trees.get(treeName);
        if (currentTree == null) {
            currentTree = new ArrayList<DocumentTreeNode>();
            DocumentModel firstAccessibleParent = null;
            if (currentDocument != null) {

                if (Boolean.TRUE.equals(isUserWorkspace)) {
                    firstAccessibleParent = documentManager.getDocument(new PathRef(
                            userWorkspacePath));
                } else {

                    List<DocumentModel> parents = documentManager.getParentDocuments(currentDocument.getRef());
                    if (!parents.isEmpty()) {
                        firstAccessibleParent = parents.get(0);
                    } else if (!"Root".equals(currentDocument.getType())
                            && currentDocument.isFolder()) {
                        // default on current doc
                        firstAccessibleParent = currentDocument;
                    } else {
                        if (showRoot) {
                            firstAccessibleParent = currentDocument;
                        }
                    }

                }
            }
            firstAccessibleParentPath = firstAccessibleParent == null ? null
                    : firstAccessibleParent.getPathAsString();
            if (firstAccessibleParent != null) {
                Filter filter = null;
                Filter leafFilter = null;
                Sorter sorter = null;
                String pageProvider = null;
                try {
                    TreeManager treeManager = Framework.getService(TreeManager.class);
                    filter = treeManager.getFilter(treeName);
                    leafFilter = treeManager.getLeafFilter(treeName);
                    sorter = treeManager.getSorter(treeName);
                    pageProvider = treeManager.getPageProviderName(treeName);
                } catch (Exception e) {
                    log.error("Could not fetch filter or sorter for tree ", e);
                }

                DocumentTreeNode treeRoot = null;
                treeRoot = new DocumentTreeNodeImpl(firstAccessibleParent,
                        filter, leafFilter, sorter, pageProvider);
                currentTree.add(treeRoot);
                log.debug("Tree initialized with document: "
                        + firstAccessibleParent.getId());
            } else {
                log.debug("Could not initialize the navigation tree: no parent"
                        + " found for current document");
            }
            trees.put(treeName, currentTree);
        }
        return trees.get(treeName);
    }

    @Deprecated
    public void changeExpandListener(CollapsibleSubTableToggleEvent event) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Map<String, Object> requestMap = facesContext.getExternalContext().getRequestMap();
        requestMap.put(NODE_SELECTED_MARKER, Boolean.TRUE);
    }

    public String getCurrentDocumentPath() {
        if (currentDocumentPath == null) {
            DocumentModel currentDoc = navigationContext.getCurrentDocument();
            if (currentDoc != null) {
                currentDocumentPath = currentDoc.getPathAsString();
            }
        }
        return currentDocumentPath;
    }

    protected String getUserWorkspacePath() {
        String currentDocumentPath = getCurrentDocumentPath();
        if (StringUtils.isBlank(currentPersonalWorkspacePath)) {
            reset();
            return currentDocumentPath;
        }
        if (userWorkspacePath == null
                || !userWorkspacePath.contains(currentPersonalWorkspacePath)) {
            // navigate to another personal workspace
            reset();
            try {
                return documentManager.exists(new PathRef(
                        currentPersonalWorkspacePath)) ? currentPersonalWorkspacePath
                        : findFarthestContainerPath(currentDocumentPath);
            } catch (ClientException e) {
                return currentDocumentPath;
            }
        }
        return userWorkspacePath;
    }

    protected String findFarthestContainerPath(String documentPath)
            throws ClientException {
        Path containerPath = new Path(documentPath);
        String result;
        do {
            result = containerPath.toString();
            containerPath = containerPath.removeLastSegments(1);
        } while (!containerPath.isRoot()
                && documentManager.exists(new PathRef(containerPath.toString())));
        return result;
    }

    @Observer(value = { EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED }, create = false)
    @BypassInterceptors
    public void resetCurrentDocumentData() {
        currentDocumentPath = null;
        if (checkIfTreeInvalidationNeeded()) {
            trees.clear();
            return;
        }
        // reset tree in case an accessible parent is finally found this time
        // for the new current document
        for (List<DocumentTreeNode> tree : trees.values()) {
            if (tree != null && tree.isEmpty()) {
                tree = null;
            }
        }
    }

    protected boolean checkIfTreeInvalidationNeeded() {
        // NXP-9813: this check may consume more resource, because called each
        // time a document selection is changed but it guarantees a better
        // detection if moving from one tree to another without using
        // UserWorkspace actions from user menu, which raise appropriate events
        DocumentModel currentDocument = (DocumentModel) Component.getInstance("currentDocument");
        if (currentDocument != null
                && firstAccessibleParentPath != null
                && currentDocument.getPathAsString() != null
                && (!currentDocument.getPathAsString().contains(
                        firstAccessibleParentPath) || (userWorkspacePath != null
                        && currentDocument.getPathAsString().contains(
                                userWorkspacePath) && !firstAccessibleParentPath.contains(userWorkspacePath)))) {
            return true;
        }
        return false;
    }

    @Observer(value = { EventNames.GO_HOME,
            EventNames.DOMAIN_SELECTION_CHANGED, EventNames.DOCUMENT_CHANGED,
            EventNames.DOCUMENT_SECURITY_CHANGED,
            EventNames.DOCUMENT_CHILDREN_CHANGED }, create = false)
    @BypassInterceptors
    public void reset() {
        trees.clear();
        resetCurrentDocumentData();
    }

    @Observer(value = { EventNames.GO_PERSONAL_WORKSPACE }, create = true)
    public void switchToUserWorkspace() {
        userWorkspacePath = getCurrentDocumentPath();
        reset();
    }

    @Observer(value = { EventNames.GO_HOME }, create = false)
    @BypassInterceptors
    public void switchToDocumentBase() {
    }

    public String forceTreeRefresh() throws IOException {

        resetCurrentDocumentData();

        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        response.setContentType("application/xml; charset=UTF-8");
        response.getWriter().write("<response>OK</response>");
        context.responseComplete();

        return null;
    }

    /**
     * @since 5.9.6
     */
    public void toggleListener() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Map<String, Object> requestMap = facesContext.getExternalContext().getRequestMap();
        requestMap.put(NODE_SELECTED_MARKER, Boolean.TRUE);
    }

    /**
     * @since 5.9.6
     */
    public boolean isNodeExpandEvent() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            ExternalContext externalContext = facesContext.getExternalContext();
            if (externalContext != null) {
                return Boolean.TRUE.equals(externalContext.getRequestMap().get(
                        NODE_SELECTED_MARKER));
            }
        }
        return false;
    }

}
