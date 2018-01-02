/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: DirectoryTreeManagerBean.java 28950 2008-01-11 13:35:06Z tdelprat $
 */
package org.nuxeo.ecm.webapp.directory;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.nuxeo.ecm.webapp.directory.DirectoryTreeNode.PARENT_FIELD_ID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.ui.web.directory.DirectoryHelper;
import org.nuxeo.ecm.webapp.seam.NuxeoSeamHotReloader;
import org.nuxeo.runtime.api.Framework;

/**
 * Manage trees defined by xvocabulary directories. Update the associated QueryModel when a node is selected and return
 * a parameterized faces navigation case.
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
    protected NuxeoSeamHotReloader seamReload;

    @In(create = true)
    protected Map<String, String> messages;

    protected transient Map<String, DirectoryTreeNode> treeModels;

    protected Long treeModelsTimestamp;

    protected transient DirectoryTreeService directoryTreeService;

    protected String selectedTree;

    private transient List<DirectoryTreeNode> directoryTrees;

    /*
     * The directoryTrees need a working core session in order to perform search actions.
     */
    public boolean isInitialized() {
        return documentManager != null;
    }

    public DirectoryTreeNode get(String treeName) {
        DirectoryTreeService dirTreeService = getDirectoryTreeService();
        if (seamReload.isDevModeSet() && seamReload.shouldResetCache(dirTreeService, treeModelsTimestamp)) {
            treeModels = null;
        }
        if (treeModels == null) {
            treeModels = new HashMap<String, DirectoryTreeNode>();
            treeModelsTimestamp = dirTreeService.getLastModified();
        }
        // lazy loading of tree models
        DirectoryTreeNode treeModel = treeModels.get(treeName);
        if (treeModel != null) {
            // return cached model
            return treeModel;
        }
        DirectoryTreeDescriptor config = dirTreeService.getDirectoryTreeDescriptor(treeName);
        if (config == null) {
            log.error("no DirectoryTreeDescriptor registered as " + treeName);
            return null;
        }

        // check that each required directory exists and has the xvocabulary
        // schema
        String[] directories = config.getDirectories();
        DirectoryService directoryService = DirectoryHelper.getDirectoryService();
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        try {
            boolean isFirst = true;
            for (String directoryName : directories) {
                Directory directory = directoryService.getDirectory(directoryName);
                if (directory == null) {
                    throw new DirectoryException(directoryName + " is not a registered directory");
                }
                if (!isFirst) {
                    Schema schema = schemaManager.getSchema(directory.getSchema());
                    if (!schema.hasField(PARENT_FIELD_ID)) {
                        throw new DirectoryException(directoryName + "does not have the required field: "
                                + PARENT_FIELD_ID);
                    }
                }
                isFirst = false;
            }
        } catch (DirectoryException e) {
            throw new RuntimeException(e);
        }

        treeModel = new DirectoryTreeNode(0, config, config.getName(), config.getLabel(), "", null);

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

    public List<DirectoryTreeNode> getSelectedTreeAsList() {
        List<DirectoryTreeNode> res = new ArrayList<>();
        DirectoryTreeNode selected = getSelectedTree();
        if (selected != null) {
            res.add(selected);
        }
        return res;
    }

    public DirectoryTreeNode getSelectedTree() {
        return get(getSelectedTreeName());
    }

    protected DirectoryTreeService getDirectoryTreeService() {
        if (directoryTreeService != null) {
            return directoryTreeService;
        }
        directoryTreeService = (DirectoryTreeService) Framework.getRuntime().getComponent(DirectoryTreeService.NAME);
        return directoryTreeService;
    }

    public String getLabelFor(String directoryTreeName, String fullPath) {
        return getLabelFor(directoryTreeName, fullPath, false);
    }

    public String getLabelFor(String directoryTreeName, String fullPath, boolean includeDirectoryTreeLabel) {
        DirectoryTreeNode rootNode = get(directoryTreeName);
        List<String> labels = new ArrayList<String>();
        computeLabels(labels, rootNode, fullPath, includeDirectoryTreeLabel);
        List<String> translatedLabels = translateLabels(labels);
        return StringUtils.join(translatedLabels, "/");
    }

    protected void computeLabels(List<String> labels, DirectoryTreeNode node, String fullPath,
            boolean includeDirectoryTreeLabel) {
        // add label for the root path only if specified
        if (!node.getPath().isEmpty() || (node.getPath().isEmpty() && includeDirectoryTreeLabel)) {
            labels.add(node.getDescription());
        }
        if (fullPath.equals(node.getPath())) {
            return;
        }
        for (DirectoryTreeNode treeNode : node.getChildren()) {
            if (fullPath.startsWith(treeNode.getPath())) {
                computeLabels(labels, treeNode, fullPath, includeDirectoryTreeLabel);
            }
        }
    }

    protected List<String> translateLabels(List<String> labels) {
        List<String> translatedLabels = new ArrayList<String>(labels.size());
        for (String label : labels) {
            translatedLabels.add(messages.get(label));
        }
        return translatedLabels;
    }

    public void resetCurrentTree() {
        if (treeModels != null && selectedTree != null) {
            treeModels.remove(selectedTree);
        }
    }

}
