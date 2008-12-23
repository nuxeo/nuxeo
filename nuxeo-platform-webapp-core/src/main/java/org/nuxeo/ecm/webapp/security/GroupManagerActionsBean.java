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

package org.nuxeo.ecm.webapp.security;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.annotations.datamodel.DataModelSelection;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.NuxeoGroupImpl;
import org.nuxeo.ecm.directory.SizeLimitExceededException;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.exceptions.GroupAlreadyExistsException;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * @author Razvan Caraghin
 */
@Name("groupManagerActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class GroupManagerActionsBean extends InputController implements
        GroupManagerActions, Serializable {

    private static final long serialVersionUID = 5592973087289772720L;

    private static final Log log = LogFactory.getLog(GroupManagerActionsBean.class);

    private static final String ALL = "all";

    public static final String VALID_CHARS = "0123456789_-"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    @In
    protected transient Context conversationContext;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    @In(create = true)
    transient UserManager userManager;

    @In(create = true, required = false)
    transient CoreSession documentManager;

    @DataModel("groupList")
    List<NuxeoGroup> groups;

    @DataModelSelection("groupList")
    NuxeoGroup selectedGroup;

    @In(required = false)
    NuxeoGroupImpl newGroup;

    @In(create = true)
    PrincipalListManager principalListManager;

    // private boolean principalIsAdmin;
    private NuxeoPrincipal principal;

    protected String groupListingMode;

    protected String searchString = "";

    private boolean searchOverflow;

    private List<NuxeoGroup> allGroups;

    private boolean allGroupsOverflow;

    @Create
    public void initialize() throws ClientException {
        log.debug("Initializing...");
        principal = (NuxeoPrincipal) FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal();
        groupListingMode = userManager.getGroupListingMode();
    }

    public void destroy() {
        log.debug("destroy");
    }

    @Factory("groupList")
    public void recomputeGroupList() throws ClientException {
        if (!StringUtils.isEmpty(searchString) && !"*".equals(searchString)) {
            try {
                searchOverflow = false;
                groups = userManager.searchGroups(searchString);
            } catch (SizeLimitExceededException e) {
                searchOverflow = true;
                groups = Collections.emptyList();
            }
        } else if (ALL.equals(groupListingMode) || "*".equals(searchString)) {
            if (allGroups == null) {
                try {
                    allGroupsOverflow = false;
                    allGroups = userManager.getAvailableGroups();
                } catch (SizeLimitExceededException e) {
                    allGroupsOverflow = true;
                    allGroups = Collections.emptyList();
                }
            }
            searchOverflow = allGroupsOverflow;
            groups = new ArrayList<NuxeoGroup>(allGroups);
        } else {
            searchOverflow = false;
            groups = Collections.emptyList();
        }
    }

    public String viewGroup() throws ClientException {
        try {
            refreshGroup(selectedGroup);
            conversationContext.set("selectedGroup", selectedGroup);
            return "view_group";
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public String viewGroup(String groupName) throws ClientException {
        try {
            selectedGroup = userManager.getGroup(groupName);
            return viewGroup();
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public void refreshGroup(NuxeoGroup group) throws ClientException {
        NuxeoGroup freshGroup = userManager.getGroup(group.getName());
        group.setMemberGroups(freshGroup.getMemberGroups());
        group.setMemberUsers(freshGroup.getMemberUsers());
        principalListManager.setSelectedUsers(freshGroup.getMemberUsers());
    }

    public String editGroup() throws ClientException {
        try {
            refreshGroup(selectedGroup);
            conversationContext.set("selectedGroup", selectedGroup);
            return "edit_group";
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public String deleteGroup() throws ClientException {
        try {
            userManager.deleteGroup(selectedGroup);
            if (groups != null && !groups.isEmpty()) {
                groups.remove(selectedGroup);
            }
            if (allGroups != null && !allGroups.isEmpty()) {
                allGroups.remove(selectedGroup);
            }
            return "view_groups";
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public List<SelectItem> getAvailableGroups() throws ClientException {
        List<SelectItem> selectItemList = new ArrayList<SelectItem>();
        for (NuxeoGroup group : userManager.getAvailableGroups()) {
            String groupName = group.getName();
            selectItemList.add(new SelectItem(groupName, groupName));
        }
        return selectItemList;
    }

    public String updateGroup() throws ClientException {
        try {
            selectedGroup.setMemberUsers(principalListManager.getSelectedUsers());
            userManager.updateGroup(selectedGroup);

            principalListManager.setSelectedUsers(new ArrayList<String>());

            return viewGroup(selectedGroup.getName());
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public String saveGroup() throws ClientException {
        try {
            if (StringUtils.isEmpty(newGroup.getName())) {
                String message = resourcesAccessor.getMessages().get(
                        "label.groupManager.emptyGroupName");
                facesMessages.addToControl("groupName",
                        FacesMessage.SEVERITY_INFO, message);
                return null;
            }
            if (!StringUtils.containsOnly(newGroup.getName(), VALID_CHARS)) {
                String message = resourcesAccessor.getMessages().get(
                        "label.groupManager.wrongGroupName");
                facesMessages.addToControl("groupName",
                        FacesMessage.SEVERITY_ERROR, message);
                return null;
            }

            newGroup.setMemberUsers(principalListManager.getSelectedUsers());
            userManager.createGroup(newGroup);
            principalListManager.setSelectedUsers(new ArrayList<String>());

            // do not add default permission since user is already in member
            // group which have default permissions
            // documentManager.applyDefaultPermissions(newGroup.getName());

            allGroups = null; // recompute it
            recomputeGroupList();
            return viewGroup(newGroup.getName());
        } catch (GroupAlreadyExistsException e) {
            String message = resourcesAccessor.getMessages().get(
                    "error.groupManager.groupAlreadyExists");
            facesMessages.addToControl("groupName", FacesMessage.SEVERITY_INFO,
                    message);
            return null;
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public String createGroup() throws ClientException {
        try {
            newGroup = new NuxeoGroupImpl("");
            conversationContext.set("newGroup", newGroup);

            principalListManager.setSelectedUsers(new ArrayList<String>());

            return "create_group";
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public boolean getAllowCreateGroup() throws ClientException {
        try {
            return principal.isAdministrator()
                    && !userManager.areGroupsReadOnly();
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public boolean getAllowDeleteGroup() throws ClientException {
        try {
            return principal.isAdministrator()
                    && !userManager.areGroupsReadOnly();
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public boolean getAllowEditGroup() throws ClientException {
        try {
            return principal.isAdministrator()
                    && !userManager.areGroupsReadOnly();
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
        groups = null; // recomputed through factory
    }

    public String searchGroups() throws ClientException {
        return "view_groups";
    }

    public String clearSearch() throws ClientException {
        setSearchString("");
        return searchGroups();
    }

    public boolean isSearchOverflow() {
        return searchOverflow;
    }

}
