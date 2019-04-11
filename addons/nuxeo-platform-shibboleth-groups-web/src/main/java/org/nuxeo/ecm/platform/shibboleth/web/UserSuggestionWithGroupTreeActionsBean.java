/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
     */
    protected void buildTree() {
        List<DocumentModel> groups = getGroups();
        List<DocumentModel> shibbGroups = getShibbGroups();

        treeRoot = new ArrayList<>();
        treeRoot.addAll(UserTreeNodeHelper.getHierarcicalNodes(groups));
        treeRoot.addAll(UserTreeNodeHelper.buildBranch(UserTreeNodeHelper.getShibbGroupBasePath(), shibbGroups));
    }

    /**
     * Get all groups (without virtual) and shibbGroups with the userManager bean
     *
     * @return list of group which match the pattern
     * @see UserManagerImpl
     */
    protected List<DocumentModel> getGroups() {
        try {
            Map<String, Serializable> filter = new HashMap<>();
            filter.put("__virtualGroup", false);

            // parameters must be serializable so copy keySet to HashSet
            return userManager.searchGroups(filter, new HashSet<>(filter.keySet()));
        } catch (SizeLimitExceededException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Get all shibboleth group with the Shibboleth Helper
     *
     * @return All Shibboleth groups, or an empty list if SizeLimitExceededException is reached.
     * @see ShibbolethGroupHelper
     */
    protected List<DocumentModel> getShibbGroups() {
        try {
            return ShibbolethGroupHelper.getGroups();
        } catch (SizeLimitExceededException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, Object> getUserInfo(String id) {
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

    public List<UserTreeNode> getTreeRoot() {
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
