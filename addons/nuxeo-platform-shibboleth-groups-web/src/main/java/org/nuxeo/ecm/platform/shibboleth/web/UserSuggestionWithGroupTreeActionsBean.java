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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.SizeLimitExceededException;
import org.nuxeo.ecm.platform.shibboleth.ShibbolethGroupHelper;
import org.nuxeo.ecm.platform.shibboleth.web.tree.UserTreeNode;
import org.nuxeo.ecm.platform.shibboleth.web.tree.UserTreeNodeHelper;
import org.nuxeo.ecm.platform.ui.web.util.SuggestionActionsBean;
import org.nuxeo.ecm.platform.usermanager.UserManagerImpl;
import org.nuxeo.ecm.webapp.security.UserSuggestionActionsBean;
import org.richfaces.component.UITree;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

/**
 * Action bean handling tree's nodes generation and filling them with groups
 *
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 * @see org.nuxeo.ecm.webapp.security.UserSuggestionActionsBean
 */
@Name("shibbUserSuggestionWithGroupTree")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class UserSuggestionWithGroupTreeActionsBean extends UserSuggestionActionsBean {

    @In(create = true)
    SuggestionActionsBean suggestionActions;

    private static final long serialVersionUID = -1L;

    private List<UserTreeNode> treeRoot;

    protected String shibbUserName = "";

    /**
     * Build the tree with all groups (not virtual) / shibbGroup and merge them into a List of UserTreeNode to be
     * displayed
     * 
     * @throws org.nuxeo.ecm.core.api.ClientException thrown from getGroups() or getShibbGroups()
     */
    protected void buildTree() throws ClientException {
        List<DocumentModel> groups = getGroups();
        List<DocumentModel> shibbGroups = getShibbGroups();

        treeRoot = new ArrayList<UserTreeNode>();
        treeRoot.addAll(UserTreeNodeHelper.getHierarcicalNodes(groups));
        treeRoot.addAll(UserTreeNodeHelper.buildBranch(UserTreeNodeHelper.getShibbGroupBasePath(), shibbGroups));
    }

    /**
     * Get all groups (without virtual) and shibbGroups with the userManager bean
     *
     * @return list of group which match the pattern
     * @throws ClientException if error occurred while getting all groups.
     * @see UserManagerImpl
     */
    protected List<DocumentModel> getGroups() throws ClientException {
        try {
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            filter.put("__virtualGroup", false);

            // parameters must be serializable so copy keySet to HashSet
            return userManager.searchGroups(filter, new HashSet<String>(filter.keySet()));
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
            throw new ClientException("error searching for Shibboleth Groups", e);
        }
    }

    @Override
    public Map<String, Object> getUserInfo(String id) throws ClientException {
        Map<String, Object> userInfo = super.getUserInfo(id);
        if (userInfo.get(ENTRY_KEY_NAME) == null) {
            DocumentModel doc = userManager.getBareUserModel();
            doc.setProperty(userManager.getUserSchemaName(), userManager.getUserIdField(), id);
            userInfo.put(ENTRY_KEY_NAME, doc);
        }
        return userInfo;
    }

    public void forceShibUserValue(String newValue) {
        setShibbUserName(newValue);
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

    public String getShibbUserName() {
        return shibbUserName;
    }

    public void setShibbUserName(String shibbUserName) {
        this.shibbUserName = shibbUserName;
    }
}
