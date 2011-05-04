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

package org.nuxeo.ecm.webapp.security;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;
import java.security.Principal;

import javax.faces.application.FacesMessage;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.exceptions.GroupAlreadyExistsException;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * /** Handles users management related web actions.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 * @since 5.4.2
 */
@Name("groupsActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class GroupsActions implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(GroupsActions.class);

    public static final String VALID_CHARS = "0123456789_-"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    protected static final String DEFAULT_GROUP_LISTING_MODE = "search_only";

    public static final String GROUPS_LISTING_CHANGED = "groupsListingChanged";

    public static final String GROUP_DETAILS_VIEW_MODE = "view";

    @In(create = true)
    protected Principal currentUser;

    @In(create = true)
    protected transient UserManager userManager;

    @In(create = true)
    protected ContentViewActions contentViewActions;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected ResourcesAccessor resourcesAccessor;

    protected Boolean canEditGroups;

    protected DocumentModel selectedGroup;

    protected String searchString = "";

    protected DocumentModel newGroup;

    protected String groupListingMode;

    protected String groupDetailsMode;

    protected boolean showCreateGroup;

    protected boolean showGroup;

    public DocumentModel getSelectedGroup() {
        return selectedGroup;
    }

    public void setSelectedGroup(String groupName)
            throws ClientException {
        this.selectedGroup = refreshGroup(groupName);
    }

    // refresh to get references
    protected DocumentModel refreshGroup(String groupName)
            throws ClientException {
        return userManager.getGroupModel(groupName);
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public DocumentModel getNewGroup() throws ClientException {
        if (newGroup == null) {
            newGroup = userManager.getBareGroupModel();
        }
        return newGroup;
    }

    public String getGroupListingMode() throws ClientException {
        if (groupListingMode == null) {
            groupListingMode = userManager.getGroupListingMode();
            if (groupListingMode == null || groupListingMode.trim().isEmpty()) {
                groupListingMode = DEFAULT_GROUP_LISTING_MODE;
            }
        }
        return groupListingMode;
    }

    public void setUserListingMode(String groupListingMode) {
        this.groupListingMode = groupListingMode;
    }

    public String getGroupDetailsMode() {
        if (groupDetailsMode == null) {
            groupDetailsMode = GROUP_DETAILS_VIEW_MODE;
        }
        return groupDetailsMode;
    }

    public void setGroupDetailsMode(String mode) {
        groupDetailsMode = mode;
    }

    public boolean isShowCreateGroup() {
        return showCreateGroup;
    }

    public void toggleShowCreateGroup() {
        showCreateGroup = !showCreateGroup;
    }

    public boolean isShowGroup() {
        return showGroup;
    }

    public void toggleShowGroup() {
        showGroup = !showGroup;
    }

    public void clearSearch() {
        searchString = null;
        fireSeamEvent(GROUPS_LISTING_CHANGED);
    }

    public void createGroup() throws ClientException {
        try {
            selectedGroup = userManager.createGroup(newGroup);
            newGroup = null;
            facesMessages.add(
                    StatusMessage.Severity.INFO,
                    resourcesAccessor.getMessages().get(
                            "info.groupManager.groupCreated"));
            showCreateGroup = false;
            fireSeamEvent(GROUPS_LISTING_CHANGED);
        } catch (GroupAlreadyExistsException e) {
            String message = resourcesAccessor.getMessages().get(
                    "error.groupManager.groupAlreadyExists");
            facesMessages.addToControl("groupName",
                    StatusMessage.Severity.ERROR, message);
        }
    }

    public void updateGroup() throws ClientException {
        try {
            userManager.updateGroup(selectedGroup);
            groupDetailsMode = GROUP_DETAILS_VIEW_MODE;
            fireSeamEvent(GROUPS_LISTING_CHANGED);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public void deleteGroup() throws ClientException {
        try {
            userManager.deleteGroup(selectedGroup);
            selectedGroup = null;
            showGroup = false;
            fireSeamEvent(GROUPS_LISTING_CHANGED);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public boolean getAllowCreateGroup() throws ClientException {
        return getCanEditGroups();
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

    public boolean getAllowDeleteGroup() throws ClientException {
        return getCanEditGroups()
                && !BaseSession.isReadOnlyEntry(selectedGroup);
    }

    public boolean getAllowEditGroup() throws ClientException {
        return getCanEditGroups()
                && !BaseSession.isReadOnlyEntry(selectedGroup);
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

    public String viewGroup(String groupName) throws ClientException {
        setSelectedGroup(groupName);
        showGroup = true;
        // set the right tabs
        return "user_center";
    }

    protected void fireSeamEvent(String eventName) {
        Events evtManager = Events.instance();
        evtManager.raiseEvent(eventName);
    }

    @Observer(value = { GROUPS_LISTING_CHANGED })
    public void onUsersListingChanged() {
        contentViewActions.refreshOnSeamEvent(GROUPS_LISTING_CHANGED);
        contentViewActions.resetPageProviderOnSeamEvent(GROUPS_LISTING_CHANGED);
    }

}
