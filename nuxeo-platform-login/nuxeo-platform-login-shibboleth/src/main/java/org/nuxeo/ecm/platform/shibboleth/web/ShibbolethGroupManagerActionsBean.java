/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Arnaud Kervern
 */

package org.nuxeo.ecm.platform.shibboleth.web;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.security.Principal;

import javax.faces.application.FacesMessage;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.annotations.datamodel.DataModelSelection;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.model.InvalidPropertyValueException;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.platform.shibboleth.ShibbolethGroupHelper;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.exceptions.GroupAlreadyExistsException;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

@Name("shibbGroupManagerActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class ShibbolethGroupManagerActionsBean {

    protected static final String VIEW_SHIBB_GROUP = "view_shibbGroup";

    protected static final String VIEW_SHIBB_GROUPS = "view_shibbGroups";

    protected static final String EDIT_SHIBB_GROUP = "edit_shibbGroup";

    private static final long serialVersionUID = -2103588024105680788L;

    @DataModel(value = "shibbGroupList")
    protected DocumentModelList groups;

    @DataModelSelection("shibbGroupList")
    protected DocumentModel selectedGroup;

    protected DocumentModel newGroup;

    protected String searchString = null;

    protected String groupListingMode;

    protected Boolean canEditGroups;

    @In(create = true)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient UserManager userManager;

    @In(create = true)
    protected Principal currentUser;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    public String createGroup() throws ClientException {
        try {
            selectedGroup = ShibbolethGroupHelper.createGroup(newGroup);
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
        } catch (InvalidPropertyValueException e) {
            String message = resourcesAccessor.getMessages().get(
                    "error.shibboleth.groupManager.wrongEl");
            facesMessages.addToControl("expressionLanguage",
                    FacesMessage.SEVERITY_ERROR, message);
            return null;
        }
    }

    public String deleteGroup() throws ClientException {
        ShibbolethGroupHelper.deleteGroup(selectedGroup);
        resetGroups();
        return viewGroups();
    }

    public String editGroup() throws ClientException {
        selectedGroup = refreshGroup(selectedGroup.getId());
        return EDIT_SHIBB_GROUP;
    }

    @Factory(value = "shibbGroupList")
    public DocumentModelList getShibbGroups() throws ClientException {
        if (groups == null) {
            String groupListingMode = getGroupListingMode();
            if ("all".equals(groupListingMode)
                    || "*".equals(getTrimmedSearchString())) {
                groups = ShibbolethGroupHelper.getGroups();
            } else if (!StringUtils.isEmpty(getTrimmedSearchString())) {
                groups = ShibbolethGroupHelper.searchGroup(getTrimmedSearchString());
            }
        }
        if (groups == null) {
            groups = new DocumentModelListImpl();
        }
        return groups;
    }

    protected String getTrimmedSearchString() {
        if (searchString == null) {
            return null;
        }
        return searchString.trim();
    }

    protected String getGroupListingMode() throws ClientException {
        if (groupListingMode == null) {
            groupListingMode = userManager.getGroupListingMode();
        }
        return groupListingMode;
    }

    public DocumentModel getNewGroup() throws ClientException {
        if (newGroup == null) {
            newGroup = ShibbolethGroupHelper.getBareGroupModel(documentManager);
        }
        return newGroup;
    }

    public boolean isSelectedGroupReadOnly() {
        return false;
    }

    public String updateGroup() throws ClientException {
        try {
            ShibbolethGroupHelper.updateGroup(selectedGroup);
            // reset users and groups
            resetGroups();
            return viewGroup(selectedGroup.getId());
        } catch (InvalidPropertyValueException e) {
            String message = resourcesAccessor.getMessages().get(
                    "error.shibboleth.groupManager.wrongEl");
            facesMessages.addToControl("expressionLanguage",
                    FacesMessage.SEVERITY_ERROR, message);
            return null;
        }
    }

    protected void resetGroups() {
        groups = null;
    }

    public String viewGroup() throws ClientException {
        return viewGroup(selectedGroup, false);
    }

    public String viewGroup(String groupName) throws ClientException {
        return viewGroup(ShibbolethGroupHelper.getGroup(groupName), false);
    }

    // refresh to get references
    protected DocumentModel refreshGroup(String groupName)
            throws ClientException {
        return ShibbolethGroupHelper.getGroup(groupName);
    }

    protected String viewGroup(DocumentModel group, boolean refresh)
            throws ClientException {
        if (group != null) {
            selectedGroup = group;
            if (refresh) {
                selectedGroup = refreshGroup(group.getId());
            }
            if (selectedGroup != null) {
                return VIEW_SHIBB_GROUP;
            }
        }
        return null;
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
        return getCanEditGroups()
                && !BaseSession.isReadOnlyEntry(selectedGroup);
    }

    public boolean getAllowEditGroup() throws ClientException {
        return getCanEditGroups()
                && !BaseSession.isReadOnlyEntry(selectedGroup);
    }

    public String viewGroups() {
        return VIEW_SHIBB_GROUPS;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public String searchGroups() {
        resetGroups();
        return viewGroups();
    }

    public String clearSearch() {
        searchString = null;
        return searchGroups();
    }

    public DocumentModel getSelectedGroup() {
        return selectedGroup;
    }

    public void setSelectedGroup(DocumentModel group) throws ClientException {
        selectedGroup = refreshGroup(group.getId());
    }
}
