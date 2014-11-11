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

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.security.PermitAll;
import javax.ejb.Remove;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.custom.tree2.TreeModel;
import org.apache.myfaces.custom.tree2.TreeModelBase;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.runtime.api.Framework;

/**
 * Manage trees defined by xvocabulary directories. Update the associated
 * QueryModel when a node is selected and return a parameterized faces
 * navigation case.
 *
 * @author <a href="mailto:ogrisel@nuxeo.com">Olivier Grisel</a>
 */
@Scope(CONVERSATION)
@Name("directoryTreeManager")
public class DirectoryTreeManagerBean extends InputController implements
        DirectoryTreeManager, Serializable {

    private static final long serialVersionUID = -5250556791009032616L;

    private static final Log log = LogFactory
            .getLog(DirectoryTreeManagerBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    protected transient Map<String, TreeModel> treeModels;

    protected transient DirectoryTreeService directoryTreeService;

    protected String selectedTree;

    private transient List<TreeModel> directoryTrees;

    /*
     * The directoryTrees need a working core session in order to perform search
     * actions.
     */
    public boolean isInitialized() {
        return documentManager != null;
    }

    @Remove
    @Destroy
    @PermitAll
    public void destroy() {
        // destroy seam component
    }

    public TreeModel get(String treeName) {
        if (treeModels == null) {
            treeModels = new HashMap<String, TreeModel>();
        }
        // lazy loading of tree models
        TreeModel treeModel = treeModels.get(treeName);
        if (treeModel != null) {
            // return cached model
            return treeModel;
        }
        DirectoryTreeDescriptor config = getDirectoryTreeService()
                .getDirectoryTreeDescriptor(treeName);
        if (config == null) {

            log.error("no DirectoryTreeDescriptor registered as " + treeName);
            return null;
        }
        DirectoryTreeNode treeRootNode = new DirectoryTreeNode(0, config,
                config.getName(), config.getLabel(), "", null);
        treeModel = new TreeModelBase(treeRootNode);

        // store the build tree to reuse it the next time in the same state
        treeModels.put(treeName, treeModel);
        return treeModel;
    }

    public List<String> getDirectoryTreeNames() {
        return getDirectoryTreeService().getDirectoryTrees();
    }

    public List<TreeModel> getDirectoryTrees() {
        if (directoryTrees == null) {
            directoryTrees = new LinkedList<TreeModel>();
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

    public TreeModel getSelectedTree() {
        return get(getSelectedTreeName());
    }

    protected DirectoryTreeService getDirectoryTreeService() {
        if (directoryTreeService != null) {
            return directoryTreeService;
        }
        directoryTreeService = (DirectoryTreeService) Framework.getRuntime()
                .getComponent(DirectoryTreeService.NAME);
        return directoryTreeService;
    }

}
