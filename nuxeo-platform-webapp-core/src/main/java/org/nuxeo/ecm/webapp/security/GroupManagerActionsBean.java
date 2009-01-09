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

import java.security.Principal;
import java.util.Collections;

import javax.faces.application.FacesMessage;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.annotations.datamodel.DataModelSelection;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.directory.SizeLimitExceededException;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.exceptions.GroupAlreadyExistsException;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * @author Razvan Caraghin
 */
@Name("groupManagerActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class GroupManagerActionsBean implements GroupManagerActions {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(GroupManagerActionsBean.class);

    @In(create = true)
    protected transient UserManager userManager;

    @In(create = true)
    protected Principal currentUser;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    private DocumentModelList allGroups;

    @DataModel("groupList")
    private DocumentModelList groups;

    @DataModelSelection("groupList")
    private DocumentModel selectedGroup;

    private DocumentModel newGroup;

    private String groupListingMode;

    private Boolean canEditGroups;

    private String searchString = "";

    private boolean searchOverflow;

    private boolean allGroupsOverflow;

    private String getGroupListingMode() throws ClientException {
        if (groupListingMode == null) {
            groupListingMode = userManager.getGroupListingMode();
        }
        return groupListingMode;
    }

    @Factory("groupList")
    public void recomputeGroupList() throws ClientException {
        groups = new DocumentModelListImpl();
        allGroups = new DocumentModelListImpl();
        if (!StringUtils.isEmpty(searchString) && !"*".equals(searchString)) {
            try {
                searchOverflow = false;
                groups = userManager.searchGroups(
                        Collections.<String, Object> emptyMap(), null);
            } catch (SizeLimitExceededException e) {
                searchOverflow = true;
            }
        } else if (ALL.equals(getGroupListingMode())
                || "*".equals(searchString)) {
            if (allGroups == null) {
                try {
                    allGroupsOverflow = false;
                    allGroups = userManager.searchGroups(
                            Collections.<String, Object> emptyMap(), null);
                } catch (SizeLimitExceededException e) {
                    allGroupsOverflow = true;
                }
            }
            searchOverflow = allGroupsOverflow;
        } else {
            searchOverflow = false;
        }
    }

    public String viewGroup() throws ClientException {
        refreshGroup(selectedGroup);
        return "view_group";
    }

    public String viewGroup(String groupName) throws ClientException {
        try {
            selectedGroup = userManager.getGroupModel(groupName);
            return viewGroup();
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public DocumentModel refreshGroup(DocumentModel groupModel)
            throws ClientException {
        return userManager.getGroupModel(groupModel.getId());
    }

    public String editGroup() throws ClientException {
        try {
            selectedGroup = refreshGroup(selectedGroup);
            return "edit_group";
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public DocumentModel getSelectedGroup() throws ClientException {
        return selectedGroup;
    }

    public DocumentModel getNewGroup() throws ClientException {
        if (newGroup == null) {
            newGroup = userManager.getBareGroupModel();
        }
        return newGroup;
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

    public String updateGroup() throws ClientException {
        try {
            userManager.updateGroup(selectedGroup);
            return viewGroup(selectedGroup.getName());
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public void validateGroupName(FacesContext context, UIComponent component,
            Object value) {
        if (!(value instanceof String)
                || !StringUtils.containsOnly((String) value, VALID_CHARS)) {
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(
                            context, "label.groupManager.wrongGroupName"), null);
            ((EditableValueHolder) component).setValid(false);
            context.addMessage(component.getClientId(context), message);
            // also add global message
            context.addMessage(null, message);
        }
    }

    public String createGroup() throws ClientException {
        try {
            userManager.createGroup(newGroup);

            allGroups = null; // recompute it
            recomputeGroupList();
            return viewGroup(newGroup.getName());
        } catch (GroupAlreadyExistsException e) {
            String message = resourcesAccessor.getMessages().get(
                    "error.groupManager.groupAlreadyExists");
            facesMessages.addToControl("groupName", FacesMessage.SEVERITY_INFO,
                    message);
            return null;
        }
    }

    protected boolean getCanEditGroups() throws ClientException {
        if (canEditGroups == null) {
            canEditGroups = false;
            if (!userManager.areGroupsReadOnly()
                    && currentUser instanceof NuxeoPrincipal) {
                NuxeoPrincipal pal = (NuxeoPrincipal) currentUser;
                if (pal.isAdministrator()) {
                    canEditGroups = true;
                }
            }
        }
        return canEditGroups;
    }

    public boolean getAllowCreateGroup() throws ClientException {
        return getCanEditGroups();
    }

    public boolean getAllowDeleteGroup() throws ClientException {
        return getCanEditGroups();
    }

    public boolean getAllowEditGroup() throws ClientException {
        return getCanEditGroups();
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
