/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

import org.apache.commons.lang3.StringUtils;
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

    public static final String NODE_SELECTED_MARKER = TreeActionsBean.class.getName() + "_NODE_SELECTED_MARKER";

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    protected Map<String, List<DocumentTreeNode>> trees = new HashMap<>();

    protected String currentDocumentPath;

    @In(create = true, required = false)
    protected Boolean isUserWorkspace;

    @In(create = true, required = false)
    protected String currentPersonalWorkspacePath;

    protected String userWorkspacePath;

    // cache the path of the tree root to check if invalidation are needed when
    // bypassing interceptors
    protected String firstAccessibleParentPath;

    protected boolean showingGlobalRoot;

    @In(create = true)
    protected TreeInvalidatorBean treeInvalidator;

    @Override
    public List<DocumentTreeNode> getTreeRoots() {
        return getTreeRoots(false);
    }

    public List<DocumentTreeNode> getTreeRoots(String treeName) {
        return getTreeRoots(false, treeName);
    }

    protected List<DocumentTreeNode> getTreeRoots(boolean showRoot, String treeName) {
        return getTreeRoots(showRoot, navigationContext.getCurrentDocument(), treeName);
    }

    protected List<DocumentTreeNode> getTreeRoots(boolean showRoot) {
        return getTreeRoots(showRoot, navigationContext.getCurrentDocument(), DEFAULT_TREE_PLUGIN_NAME);
    }

    protected List<DocumentTreeNode> getTreeRoots(boolean showRoot, DocumentModel currentDocument)
            {
        return getTreeRoots(showRoot, currentDocument, DEFAULT_TREE_PLUGIN_NAME);
    }

    /**
     * @since 5.4
     */
    protected List<DocumentTreeNode> getTreeRoots(boolean showRoot, DocumentModel currentDocument, String treeName)
            {

        if (treeInvalidator.needsInvalidation()) {
            reset();
            treeInvalidator.invalidationDone();
        }
        if (Boolean.TRUE.equals(isUserWorkspace)) {
            userWorkspacePath = getUserWorkspacePath();
        }
        List<DocumentTreeNode> currentTree = trees.get(treeName);
        if (currentTree == null) {
            currentTree = new ArrayList<>();
            DocumentModel globalRoot = null;
            DocumentModel firstAccessibleParent = null;
            if (currentDocument != null) {

                if (Boolean.TRUE.equals(isUserWorkspace)) {
                    firstAccessibleParent = documentManager.getDocument(new PathRef(userWorkspacePath));
                } else {

                    List<DocumentModel> parents = documentManager.getParentDocuments(currentDocument.getRef());
                    if (!parents.isEmpty()) {
                        firstAccessibleParent = parents.get(0);
                    } else if (!"Root".equals(currentDocument.getType()) && currentDocument.isFolder()) {
                        // default on current doc
                        firstAccessibleParent = currentDocument;
                    } else {
                        if (showRoot) {
                            firstAccessibleParent = currentDocument;
                        }
                    }

                }
                if (showRoot
                        && (firstAccessibleParent == null || !"/".equals(firstAccessibleParent.getPathAsString()))) {
                    // also add the global root if we don't already show it and it's accessible
                    if (documentManager.exists(new PathRef("/"))) {
                        globalRoot = documentManager.getRootDocument();
                    }
                }
            }
            showingGlobalRoot = globalRoot != null;
            if (showingGlobalRoot) {
                DocumentTreeNode treeRoot = newDocumentTreeNode(globalRoot, treeName);
                currentTree.add(treeRoot);
                log.debug("Tree initialized with additional global root");
            }
            firstAccessibleParentPath = firstAccessibleParent == null ? null : firstAccessibleParent.getPathAsString();
            if (firstAccessibleParent != null) {
                DocumentTreeNode treeRoot = newDocumentTreeNode(firstAccessibleParent, treeName);
                currentTree.add(treeRoot);
                log.debug("Tree initialized with document: " + firstAccessibleParent.getId());
            } else {
                log.debug("Could not initialize the navigation tree: no parent" + " found for current document");
            }
            trees.put(treeName, currentTree);
        }
        return trees.get(treeName);
    }

    protected DocumentTreeNode newDocumentTreeNode(DocumentModel doc, String treeName) {
        TreeManager treeManager = Framework.getService(TreeManager.class);
        Filter filter = treeManager.getFilter(treeName);
        Filter leafFilter = treeManager.getLeafFilter(treeName);
        Sorter sorter = treeManager.getSorter(treeName);
        String pageProvider = treeManager.getPageProviderName(treeName);
        return new DocumentTreeNodeImpl(doc, filter, leafFilter, sorter, pageProvider);
    }

    @Deprecated
    public void changeExpandListener(CollapsibleSubTableToggleEvent event) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Map<String, Object> requestMap = facesContext.getExternalContext().getRequestMap();
        requestMap.put(NODE_SELECTED_MARKER, Boolean.TRUE);
    }

    @Override
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
        if (userWorkspacePath == null || !userWorkspacePath.contains(currentPersonalWorkspacePath)) {
            // navigate to another personal workspace
            reset();
            return documentManager.exists(new PathRef(currentPersonalWorkspacePath)) ? currentPersonalWorkspacePath
                    : findFarthestContainerPath(currentDocumentPath);
        }
        return userWorkspacePath;
    }

    protected String findFarthestContainerPath(String documentPath) {
        Path containerPath = new Path(documentPath);
        String result;
        do {
            result = containerPath.toString();
            containerPath = containerPath.removeLastSegments(1);
        } while (!containerPath.isRoot() && documentManager.exists(new PathRef(containerPath.toString())));
        return result;
    }

    @Override
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
        if (currentDocument != null && showingGlobalRoot) {
            return true;
        }
        if (currentDocument != null
                && firstAccessibleParentPath != null
                && currentDocument.getPathAsString() != null
                && (!currentDocument.getPathAsString().contains(firstAccessibleParentPath) || (userWorkspacePath != null
                        && currentDocument.getPathAsString().contains(userWorkspacePath) && !firstAccessibleParentPath.contains(userWorkspacePath)))) {
            return true;
        }
        return false;
    }

    @Override
    @Observer(value = { EventNames.GO_HOME, EventNames.DOMAIN_SELECTION_CHANGED, EventNames.DOCUMENT_CHANGED,
            EventNames.DOCUMENT_SECURITY_CHANGED, EventNames.DOCUMENT_CHILDREN_CHANGED }, create = false)
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
     * @since 6.0
     */
    @Override
    public void toggleListener() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Map<String, Object> requestMap = facesContext.getExternalContext().getRequestMap();
        requestMap.put(NODE_SELECTED_MARKER, Boolean.TRUE);
    }

    /**
     * @since 6.0
     */
    @Override
    public boolean isNodeExpandEvent() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            ExternalContext externalContext = facesContext.getExternalContext();
            if (externalContext != null) {
                return Boolean.TRUE.equals(externalContext.getRequestMap().get(NODE_SELECTED_MARKER));
            }
        }
        return false;
    }

}
