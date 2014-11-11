/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: DirectoryTreeManagerBean.java 28950 2008-01-11 13:35:06Z tdelprat $
 */
package org.nuxeo.ecm.webapp.directory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ejb.Remove;
import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.runtime.api.Framework;
import org.richfaces.component.UITree;
import org.richfaces.event.NodeExpandedEvent;

import static org.jboss.seam.ScopeType.CONVERSATION;

/**
 * Manage trees defined by xvocabulary directories. Update the associated
 * QueryModel when a node is selected and return a parameterized faces
 * navigation case.
 *
 * @author <a href="mailto:ogrisel@nuxeo.com">Olivier Grisel</a>
 */
@Scope(CONVERSATION)
@Name("directoryTreeManager")
public class DirectoryTreeManagerBean implements DirectoryTreeManager {

    private static final long serialVersionUID = -5250556791009032616L;

    private static final Log log = LogFactory.getLog(DirectoryTreeManagerBean.class);

    public static final String NODE_SELECTED_MARKER = DirectoryTreeManagerBean.class.getName()
            + "_NODE_SELECTED_MARKER";

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected ResourcesAccessor resourcesAccessor;

    protected transient Map<String, DirectoryTreeNode> treeModels;

    protected transient DirectoryTreeService directoryTreeService;

    protected String selectedTree;

    private transient List<DirectoryTreeNode> directoryTrees;

    /*
     * The directoryTrees need a working core session in order to perform search
     * actions.
     */
    public boolean isInitialized() {
        return documentManager != null;
    }

    public DirectoryTreeNode get(String treeName) {
        if (treeModels == null) {
            treeModels = new HashMap<String, DirectoryTreeNode>();
        }
        // lazy loading of tree models
        DirectoryTreeNode treeModel = treeModels.get(treeName);
        if (treeModel != null) {
            // return cached model
            return treeModel;
        }
        DirectoryTreeDescriptor config = getDirectoryTreeService().getDirectoryTreeDescriptor(
                treeName);
        if (config == null) {

            log.error("no DirectoryTreeDescriptor registered as " + treeName);
            return null;
        }
        treeModel = new DirectoryTreeNode(0, config, config.getName(),
                config.getLabel(), "", null);

        // store the build tree to reuse it the next time in the same state
        treeModels.put(treeName, treeModel);
        return treeModel;
    }

    public List<String> getDirectoryTreeNames() {
        return getDirectoryTreeService().getDirectoryTrees();
    }

    public List<DirectoryTreeNode> getDirectoryTrees() {
        if (directoryTrees == null) {
            directoryTrees = new LinkedList<DirectoryTreeNode>();
            for (String treeName : getDirectoryTreeNames()) {
                directoryTrees.add(get(treeName));
            }
        }
        return directoryTrees;
    }

    public String getSelectedTreeName() {
        if (selectedTree == null) {
            List<String> names = getDirectoryTreeNames();
            if (!names.isEmpty()) {
                selectedTree = names.get(0);
            }
        }
        return selectedTree;
    }

    public void setSelectedTreeName(String treeName) {
        selectedTree = treeName;
    }

    public DirectoryTreeNode getSelectedTree() {
        return get(getSelectedTreeName());
    }

    protected DirectoryTreeService getDirectoryTreeService() {
        if (directoryTreeService != null) {
            return directoryTreeService;
        }
        directoryTreeService = (DirectoryTreeService) Framework.getRuntime().getComponent(
                DirectoryTreeService.NAME);
        return directoryTreeService;
    }

    public void changeExpandListener(NodeExpandedEvent event) {

        // Toggle the expanded/collapse state for this node:
        // its only used for multi-select (see DirectoryTreeNode.isOpened())
        // Note: we can't use the internal nodeState.isExpanded() method because
        // this is broken as of writing
        // https://jira.jboss.org/jira/browse/RF-7273
        // TreeState nodeState = (TreeState) requestMap.get("nodeState");
        // TreeRowKey treeRowKey = nodeState.getSelectedNode();
        // boolean isExpanded = nodeState.isExpanded(treeRowKey);

        UIComponent component = event.getComponent();
        if (component instanceof UITree) {
            UITree treeComponent = (UITree) component;
            Object value = treeComponent.getRowData();
            if (value instanceof DirectoryTreeNode) {
                DirectoryTreeNode treeNode = (DirectoryTreeNode) value;
                if (treeNode.isOpen()) {
                    treeNode.setOpen(false);
                } else {
                    treeNode.setOpen(true);
                }
            }
        }
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Map<String, Object> requestMap = facesContext.getExternalContext().getRequestMap();
        requestMap.put(NODE_SELECTED_MARKER, Boolean.TRUE);
    }

    protected Boolean isNodeExpandEvent() {
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

    public Boolean adviseNodeOpened(UITree treeComponent) {
        if (!isNodeExpandEvent()) {
            try {
                Object value = treeComponent.getRowData();
                if (value instanceof DirectoryTreeNode) {
                    DirectoryTreeNode treeNode = (DirectoryTreeNode) value;
                    if (treeNode.isOpened()) {
                        return Boolean.TRUE;
                    }
                }
            } catch (ClientException e) {
                log.error(e);
            }
        }
        return null;
    }

    public String getLabelFor(String directoryTreeName, String fullPath) {
        return getLabelFor(directoryTreeName, fullPath, false);
    }

    public String getLabelFor(String directoryTreeName, String fullPath,
            boolean includeDirectoryTreeLabel) {
        DirectoryTreeNode rootNode = get(directoryTreeName);
        List<String> labels = new ArrayList<String>();
        computeLabels(labels, rootNode, fullPath, includeDirectoryTreeLabel);
        List<String> translatedLabels = translateLabels(labels);
        return StringUtils.join(translatedLabels, "/");
    }

    protected void computeLabels(List<String> labels, DirectoryTreeNode node,
            String fullPath, boolean includeDirectoryTreeLabel) {
        // add label for the root path only if specified
        if (!node.getPath().isEmpty()
                || (node.getPath().isEmpty() && includeDirectoryTreeLabel)) {
            labels.add(node.getDescription());
        }
        if (fullPath.equals(node.getPath())) {
            return;
        }
        for (DirectoryTreeNode treeNode : node.getChildren()) {
            if (fullPath.startsWith(treeNode.getPath())) {
                computeLabels(labels, treeNode, fullPath,
                        includeDirectoryTreeLabel);
            }
        }
    }

    protected List<String> translateLabels(List<String> labels) {
        List<String> translatedLabels = new ArrayList<String>(labels.size());
        for (String label : labels) {
            translatedLabels.add(resourcesAccessor.getMessages().get(label));
        }
        return translatedLabels;
    }

    @Remove
    @Destroy
    public void destroy() {
        // destroy seam component
    }

}
