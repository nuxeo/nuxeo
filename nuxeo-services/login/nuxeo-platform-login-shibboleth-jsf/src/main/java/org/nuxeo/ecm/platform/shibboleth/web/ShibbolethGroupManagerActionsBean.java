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
 *
 * Contributors:
 *     Arnaud Kervern
 */

package org.nuxeo.ecm.platform.shibboleth.web;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.InvalidPropertyValueException;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.platform.shibboleth.ShibbolethGroupHelper;
import org.nuxeo.ecm.platform.usermanager.exceptions.GroupAlreadyExistsException;
import org.nuxeo.ecm.webapp.security.AbstractUserGroupManagement;

@Name("shibbGroupManagerActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class ShibbolethGroupManagerActionsBean extends AbstractUserGroupManagement {

    protected static final String EVENT_SHIBB_GROUP_LISTING = "shibbGroupsListingChanged";

    protected static final String VIEW_SHIBB_GROUP = "view_shibbGroup";

    protected static final String VIEW_SHIBB_GROUPS = "view_shibbGroups";

    protected static final String EDIT_SHIBB_GROUP = "edit_shibbGroup";

    private static final long serialVersionUID = -2103588024105680788L;

    protected DocumentModel selectedGroup;

    protected DocumentModel newGroup;

    protected Boolean canEditGroups;

    @In(create = true)
    protected transient CoreSession documentManager;

    public void createGroup() {
        createGroup(false);
    }

    public void createGroup(boolean createAnotherGroup) {
        try {
            selectedGroup = ShibbolethGroupHelper.createGroup(newGroup);
            newGroup = null;
            facesMessages.add(StatusMessage.Severity.INFO,
                    resourcesAccessor.getMessages().get("info.groupManager.groupCreated"));
            if (createAnotherGroup) {
                showCreateForm = true;
            } else {
                showCreateForm = false;
                showUserOrGroup = true;
            }
            fireSeamEvent(EVENT_SHIBB_GROUP_LISTING);
        } catch (GroupAlreadyExistsException e) {
            String message = resourcesAccessor.getMessages().get("error.groupManager.groupAlreadyExists");
            facesMessages.addToControl("groupName", StatusMessage.Severity.ERROR, message);
        } catch (PropertyException e) {
            String message = resourcesAccessor.getMessages().get("error.shibboleth.groupManager.wrongEl");
            facesMessages.addToControl("expressionLanguage", StatusMessage.Severity.ERROR, message);
        }
    }

    public void deleteGroup() {
        ShibbolethGroupHelper.deleteGroup(selectedGroup);
        selectedGroup = null;
        showUserOrGroup = false;
        fireSeamEvent(EVENT_SHIBB_GROUP_LISTING);
    }

    public String editGroup() {
        selectedGroup = refreshGroup(selectedGroup.getId());
        return EDIT_SHIBB_GROUP;
    }

    protected String getTrimmedSearchString() {
        if (searchString == null) {
            return null;
        }
        return searchString.trim();
    }

    public DocumentModel getNewGroup() {
        if (newGroup == null) {
            newGroup = ShibbolethGroupHelper.getBareGroupModel(documentManager);
        }
        return newGroup;
    }

    public boolean isSelectedGroupReadOnly() {
        return false;
    }

    public void updateGroup() {
        try {
            ShibbolethGroupHelper.updateGroup(selectedGroup);
            detailsMode = DETAILS_VIEW_MODE;
            fireSeamEvent(EVENT_SHIBB_GROUP_LISTING);
        } catch (PropertyException e) {
            String message = resourcesAccessor.getMessages().get("error.shibboleth.groupManager.wrongEl");
            facesMessages.addToControl("expressionLanguage", StatusMessage.Severity.ERROR, message);
        }
    }

    public String viewGroup() {
        if (selectedGroup == null) {
            return null;
        } else {
            return viewGroup(selectedGroup, false);
        }
    }

    public String viewGroup(String groupName) {
        setSelectedGroup(groupName);
        showUserOrGroup = true;
        return viewGroup(ShibbolethGroupHelper.getGroup(groupName), false);
    }

    // refresh to get references
    protected DocumentModel refreshGroup(String groupName) {
        return ShibbolethGroupHelper.getGroup(groupName);
    }

    protected String viewGroup(DocumentModel group, boolean refresh) {
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

    public void clearSearch() {
        searchString = null;
        fireSeamEvent(EVENT_SHIBB_GROUP_LISTING);
    }

    protected boolean getCanEditGroups() {
        if (canEditGroups == null) {
            canEditGroups = false;
            if (!userManager.areGroupsReadOnly() && currentUser != null) {
                if (currentUser.isAdministrator()) {
                    canEditGroups = true;
                }
            }
        }
        return canEditGroups;
    }

    public boolean getAllowCreateGroup() {
        return getCanEditGroups();
    }

    public boolean getAllowDeleteGroup() {
        return getCanEditGroups() && !BaseSession.isReadOnlyEntry(selectedGroup);
    }

    public boolean getAllowEditGroup() {
        return getCanEditGroups() && !BaseSession.isReadOnlyEntry(selectedGroup);
    }

    @Override
    protected String computeListingMode() {
        return userManager.getGroupListingMode();
    }

    public DocumentModel getSelectedGroup() {
        return selectedGroup;
    }

    public void setSelectedGroup(String group) {
        selectedGroup = refreshGroup(group);
    }

    protected void fireSeamEvent(String eventName) {
        Events.instance().raiseEvent(eventName);
    }

    @Observer(value = { EVENT_SHIBB_GROUP_LISTING })
    public void onUsersListingChanged() {
        contentViewActions.refreshOnSeamEvent(EVENT_SHIBB_GROUP_LISTING);
        contentViewActions.resetPageProviderOnSeamEvent(EVENT_SHIBB_GROUP_LISTING);
    }
}
