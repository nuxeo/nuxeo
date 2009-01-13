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
import java.util.HashMap;
import java.util.Map;

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

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(GroupManagerActionsBean.class);

    @In(create = true)
    protected transient UserManager userManager;

    @In(create = true)
    protected Principal currentUser;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    @DataModel(value = "groupList")
    private DocumentModelList groups;

    @DataModelSelection("groupList")
    private DocumentModel selectedGroup;

    private DocumentModel newGroup;

    private String groupListingMode;

    private Boolean canEditGroups;

    private String searchString = "";

    private boolean searchOverflow;

    private String getGroupListingMode() throws ClientException {
        if (groupListingMode == null) {
            groupListingMode = userManager.getGroupListingMode();
        }
        return groupListingMode;
    }

    @Factory(value = "groupList")
    public DocumentModelList getGroups() throws ClientException {
        if (groups == null) {
            searchOverflow = false;
            try {
                String groupListingMode = getGroupListingMode();
                if (ALL.equals(groupListingMode) || "*".equals(searchString)) {
                    groups = userManager.searchGroups(
                            Collections.<String, Object> emptyMap(), null);
                } else if (!StringUtils.isEmpty(searchString)) {
                    Map<String, Object> filter = new HashMap<String, Object>();
                    // XXX: search only on id, better conf should be set in user
                    // manager interface
                    filter.put(userManager.getGroupIdField(), searchString);
                    groups = userManager.searchGroups(filter, filter.keySet());
                }
            } catch (SizeLimitExceededException e) {
                searchOverflow = true;
            }
        }
        if (groups == null) {
            groups = new DocumentModelListImpl();
        }
        return groups;
    }

    public void resetGroups() throws ClientException {
        groups = null;
    }

    // refresh to get references
    protected DocumentModel refreshGroup(String groupName)
            throws ClientException {
        return userManager.getGroupModel(groupName);
    }

    protected String viewGroup(DocumentModel group, boolean refresh)
            throws ClientException {
        if (group != null) {
            selectedGroup = group;
            if (refresh) {
                selectedGroup = refreshGroup(group.getId());
            }
            if (selectedGroup != null) {
                return "view_group";
            }
        }
        return null;
    }

    public String viewGroups() throws ClientException {
        return "view_groups";
    }

    public String viewGroup() throws ClientException {
        return viewGroup(selectedGroup, true);
    }

    public String viewGroup(String groupName) throws ClientException {
        return viewGroup(userManager.getGroupModel(groupName), false);
    }

    public String editGroup() throws ClientException {
        selectedGroup = refreshGroup(selectedGroup.getId());
        return "edit_group";
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
            // reset users and groups
            resetGroups();
            return viewGroups();
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public String updateGroup() throws ClientException {
        try {
            userManager.updateGroup(selectedGroup);
            // reset users and groups
            resetGroups();
            return viewGroup(selectedGroup.getId());
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
            selectedGroup = userManager.createGroup(newGroup);
            newGroup = null;
            // reset so that group list is computed again
            resetGroups();
            return viewGroup(selectedGroup, false);
        } catch (GroupAlreadyExistsException e) {
            String message = resourcesAccessor.getMessages().get(
                    "error.groupManager.groupAlreadyExists");
            facesMessages.addToControl("groupName",
                    FacesMessage.SEVERITY_ERROR, message);
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
    }

    public String searchGroups() throws ClientException {
        // reset so that groups are recomputed
        resetGroups();
        return viewGroups();
    }

    public String clearSearch() throws ClientException {
        searchString = null;
        return searchGroups();
    }

    public boolean isSearchOverflow() {
        return searchOverflow;
    }

}
