/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.shibboleth.web;

import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.SizeLimitExceededException;
import org.nuxeo.ecm.platform.shibboleth.ShibbolethGroupHelper;
import org.nuxeo.ecm.platform.usermanager.UserManagerImpl;
import org.nuxeo.ecm.webapp.security.UserSuggestionActionsBean;
import org.nuxeo.ecm.platform.shibboleth.web.tree.UserTreeNode;
import org.richfaces.component.UITree;

import java.io.Serializable;
import java.util.*;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

@Name("shibbUserSuggestionWithGroupTree")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class UserSuggestionWithGroupTreeActionsBean extends
        UserSuggestionActionsBean {

    private static final long serialVersionUID = -1L;

    private List<UserTreeNode> treeRoot;

    /**
     * Build the tree with all groups (not virtual) / shibbGroup and merge them
     * into a List of UserTreeNode to be displayed
     * @throws org.nuxeo.ecm.core.api.ClientException thrown from getGroups() or getShibbGroups()
     */
    protected void buildTree() throws ClientException {
        List<DocumentModel> groups = getGroups();
        List<DocumentModel> shibbGroups = getShibbGroups();

        treeRoot = new ArrayList<UserTreeNode>();
        treeRoot.addAll(UserTreeNode.getHierarcicalNodes(groups));
        treeRoot.addAll(UserTreeNode.buildBranch("external:shibb", shibbGroups));
    }

    /**
     * Get all groups (without virtual) and shibbGroups with the userManager
     * bean
     *
     * @return list of group which match the pattern
     * @throws ClientException if error occurred while getting all groups.
     * @see UserManagerImpl
     */
    protected List<DocumentModel> getGroups() throws ClientException {
        try {
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            filter.put(userManager.getGroupIdField(), "%");
            filter.put("__virtualGroup", false);

            // parameters must be serializable so copy keySet to HashSet
            return userManager.searchGroups(filter, new HashSet<String>(
                    filter.keySet()));
        } catch (SizeLimitExceededException e) {
            return Collections.emptyList();
        } catch (Exception e) {
            throw new ClientException("error searching for groups", e);
        }
    }

    /**
     * Get all shibboleth group with the Shibboleth Helper
     *
     * @return All Shibboleth groups, or an empty list if SizeLimitExceededException is reached.
     * @throws ClientException encapsulated another one
     * @see ShibbolethGroupHelper
     */
    protected List<DocumentModel> getShibbGroups() throws ClientException {
        try {
            return ShibbolethGroupHelper.getGroups();
        } catch (SizeLimitExceededException e) {
            return Collections.emptyList();
        } catch (Exception e) {
            throw new ClientException("error searching for Shibboleth Groups",
                    e);
        }
    }

    /**
     * Check if the node is open or not
     */
    public Boolean adviseNodeOpened(UITree treeComponent) {
        return null;
    }

    public void refreshRoot() {
        treeRoot = null;
    }

    public List<UserTreeNode> getTreeRoot() throws ClientException {
        if (treeRoot == null) {
            buildTree();
        }
        return treeRoot;
    }
}
