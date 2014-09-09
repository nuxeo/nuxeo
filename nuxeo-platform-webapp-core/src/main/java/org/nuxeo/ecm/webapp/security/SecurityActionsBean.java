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

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.common.utils.i18n.Labeler;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.PermissionProvider;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.core.api.security.UserVisiblePermission;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.platform.query.api.PageSelection;
import org.nuxeo.ecm.platform.query.api.PageSelections;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

/**
 * Provides security related methods.
 *
 * @author Razvan Caraghin
 */
@Name("securityActions")
@Scope(CONVERSATION)
public class SecurityActionsBean extends InputController implements
        SecurityActions, Serializable {

    private static final long serialVersionUID = -7190826911734958662L;

    private static final Log log = LogFactory.getLog(SecurityActionsBean.class);

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected PermissionActionListManager permissionActionListManager;

    @In(create = true)
    protected PermissionListManager permissionListManager;

    @In(create = true)
    protected PrincipalListManager principalListManager;

    @In(create = true)
    protected transient UserManager userManager;

    @In(create = true)
    protected NuxeoPrincipal currentUser;

    protected static final String[] SEED_PERMISSIONS_TO_CHECK = {
            SecurityConstants.WRITE_SECURITY, SecurityConstants.READ_SECURITY };

    private static final Labeler labeler = new Labeler(
            "label.security.permission");

    protected String[] CACHED_PERMISSION_TO_CHECK;

    protected SecurityData securityData;

    protected boolean obsoleteSecurityData = true;

    protected PageSelections<String> entries;

    protected transient List<String> cachedValidatedUserAndGroups;

    protected transient List<String> cachedDeletedUserAndGroups;

    private Boolean blockRightInheritance;

    protected String selectedEntry;

    protected List<String> selectedEntries;

    @Observer(value = EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED, create = false)
    @BypassInterceptors
    public void resetSecurityData() {
        obsoleteSecurityData = true;
        blockRightInheritance = null;
    }

    public void rebuildSecurityData() throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        try {
            if (null != currentDocument) {
                if (null == securityData) {
                    securityData = new SecurityData();
                    securityData.setDocumentType(currentDocument.getType());
                }
                ACP acp = documentManager.getACP(currentDocument.getRef());

                if (null != acp) {
                    SecurityDataConverter.convertToSecurityData(acp,
                            securityData);
                } else {
                    securityData.clear();
                }

                reconstructTableModel();
                if (blockRightInheritance == null) {
                    blockRightInheritance = false;
                }
                obsoleteSecurityData = false;
            }
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    /**
     * Update the dataTableModel from the current {@link SecurityData} this
     * method is automatically called by rebuildSecurityData method
     */
    protected void reconstructTableModel()
            throws ClientException {
        List<String> items = securityData.getCurrentDocumentUsers();
        entries = new PageSelections<String>();
        if (items != null) {
            for (String item : items) {
                entries.add(new PageSelection<String>(item, false));
            }
        }
    }

    public PageSelections<String> getDataTableModel() throws ClientException {
        if (obsoleteSecurityData) {
            // lazy initialization at first time access
            rebuildSecurityData();
        }

        return entries;
    }

    public SecurityData getSecurityData() throws ClientException {
        if (obsoleteSecurityData) {
            // lazy initialization at first time access
            rebuildSecurityData();
        }
        return securityData;
    }

    public String updateSecurityOnDocument() throws ClientException {
        try {
            List<UserEntry> modifiableEntries = SecurityDataConverter.convertToUserEntries(securityData);
            ACP acp = currentDocument.getACP();

            if (null == acp) {
                acp = new ACPImpl();
            }

            acp.setRules(modifiableEntries.toArray(new UserEntry[0]));

            currentDocument.setACP(acp, true);
            documentManager.save();
            Events.instance().raiseEvent(EventNames.DOCUMENT_SECURITY_CHANGED);

            // Reread data from the backend to be sure the current bean
            // state is uptodate w.r.t. the real backend state
            rebuildSecurityData();

            // Type currentType =
            // typeManager.getType(currentDocument.getType());
            // return applicationController
            // .getPageOnEditedDocumentType(currentType);

            // Forward to default view, that's not what we want
            // return navigationContext.getActionResult(currentDocument,
            // UserAction.AFTER_EDIT);

            // Temporary fix, to avoid forward to default_view.
            // The same page is reloaded after submit.
            // May use UserAction, with new kind of action (AFTER_EDIT_RIGHTS)
            // ?
            return null;

        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public String addPermission(String principalName, String permissionName,
            boolean grant) throws ClientException {
        if (securityData == null) {
            try {
                securityData = getSecurityData();
            } catch (ClientException e) {
                log.error(e); // TODO raise me instead
                return null;
            }
        }

        String grantPerm = permissionName;
        String denyPerm = permissionName;
        List<UserVisiblePermission> uvps = getVisibleUserPermissions(securityData.getDocumentType());
        if (uvps != null) {
            for (UserVisiblePermission uvp : uvps) {
                if (uvp.getId().equals(permissionName)) {
                    grantPerm = uvp.getPermission();
                    denyPerm = uvp.getDenyPermission();
                    break;
                }
            }
        } else {
            log.debug("no entry for documentType in visibleUserPermissions this should never happend, using default mapping ...");
        }

        if (grant) {
            // remove the opposite rule if any
            boolean removed = securityData.removeModifiablePrivilege(
                    principalName, denyPerm, !grant);
            if (!removed) {
                removed = securityData.removeModifiablePrivilege(principalName,
                        grantPerm, !grant);
            }
            // add rule only if none was removed
            if (!removed) {
                securityData.addModifiablePrivilege(principalName, grantPerm,
                        grant);
            }
        } else {
            // remove the opposite rule if any
            boolean removed = securityData.removeModifiablePrivilege(
                    principalName, grantPerm, !grant);
            if (!removed) {
                removed = securityData.removeModifiablePrivilege(principalName,
                        denyPerm, !grant);
            }
            // add rule only if none was removed
            if (!removed) {
                securityData.addModifiablePrivilege(principalName, denyPerm,
                        grant);
            }
        }

        try {
            reconstructTableModel();
        } catch (ClientException e) {
            log.error("Error whil reconstructing security data", e);
        }
        return null;
    }

    public String addPermission() throws ClientException {
        String permissionName = permissionListManager.getSelectedPermission();
        boolean grant = permissionActionListManager.getSelectedGrant().equals(
                "Grant");
        return addPermission(selectedEntry, permissionName, grant);
    }

    public String addPermissions() throws ClientException {
        if (selectedEntries == null || selectedEntries.isEmpty()) {
            String message = ComponentUtils.translate(
                    FacesContext.getCurrentInstance(),
                    "error.rightsManager.noUsersSelected");
            FacesMessages.instance().add(message);
            return null;
        }
        String permissionName = permissionListManager.getSelectedPermission();
        boolean grant = permissionActionListManager.getSelectedGrant().equals(
                "Grant");

        for (String principalName : selectedEntries) {
            addPermission(principalName, permissionName, grant);
        }
        return null;
    }

    public String addPermissionAndUpdate() throws ClientException {
        addPermission();
        updateSecurityOnDocument();
        return null;
    }

    public String addPermissionsAndUpdate() throws ClientException {
        addPermissions();
        updateSecurityOnDocument();
        selectedEntries = null;
        facesMessages.add(StatusMessage.Severity.INFO,
                resourcesAccessor.getMessages().get("message.updated.rights"));
        return null;
    }

    public String saveSecurityUpdates() throws ClientException {
        updateSecurityOnDocument();
        selectedEntries = null;
        facesMessages.add(StatusMessage.Severity.INFO,
                resourcesAccessor.getMessages().get("message.updated.rights"));
        return null;
    }

    public String removePermission() {
        securityData.removeModifiablePrivilege(selectedEntry,
                permissionListManager.getSelectedPermission(),
                permissionActionListManager.getSelectedGrant().equals("Grant"));

        try {
            reconstructTableModel();
        } catch (ClientException e) {
            log.error("Error whil reconstructing security data", e);
        }
        return null;
    }

    public String removePermissionAndUpdate() throws ClientException {
        removePermission();

        if (!checkPermissions()) {
            facesMessages.add(
                    StatusMessage.Severity.ERROR,
                    resourcesAccessor.getMessages().get(
                            "message.updated.rights"));
            return null;
        }

        updateSecurityOnDocument();
        // do not redirect to the default folder view
        return null;
    }

    public String removePermissions() throws ClientException {
        for (PageSelection<String> user : getSelectedRows()) {
            securityData.removeModifiablePrivilege((String)user.getData());
            if (!checkPermissions()) {
                facesMessages.add(
                        StatusMessage.Severity.ERROR,
                        resourcesAccessor.getMessages().get(
                                "message.error.removeRight"));
                return null;
            }
        }
        reconstructTableModel();
        return null;
    }

    public String removePermissionsAndUpdate() throws ClientException {
        for (PageSelection<String> user : getDataTableModel().getEntries()) {
            securityData.removeModifiablePrivilege((String)user.getData());
            if (!checkPermissions()) {
                facesMessages.add(
                        StatusMessage.Severity.ERROR,
                        resourcesAccessor.getMessages().get(
                                "message.error.removeRight"));
                return null;
            }
        }
        updateSecurityOnDocument();
        facesMessages.add(StatusMessage.Severity.INFO,
                resourcesAccessor.getMessages().get("message.updated.rights"));
        // do not redirect to the default folder view
        return null;
    }

    public boolean getCanAddSecurityRules() throws ClientException {
        return documentManager.hasPermission(currentDocument.getRef(),
                "WriteSecurity");
    }

    public boolean getCanRemoveSecurityRules() throws ClientException {
        try {
            return documentManager.hasPermission(currentDocument.getRef(),
                    "WriteSecurity")
                    && !getSelectedRows().isEmpty();
        } catch (Exception e) {
            throw ClientException.wrap(e);
        }
    }

    /**
     * @return The list of selected rows in the local rights table.
     *
     * @since 5.9.6
     */
    private List<PageSelection<String>> getSelectedRows() {
        List<PageSelection<String>> selectedRows = new ArrayList<PageSelection<String>>();

        for (PageSelection<String> entry : getDataTableModel().getEntries()) {
            if (entry.isSelected()) {
                selectedRows.add(entry);
            }
        }
        return selectedRows;
    }

    public List<UserVisiblePermission> getVisibleUserPermissions(
            String documentType) throws ClientException {
        try {
            return Framework.getLocalService(PermissionProvider.class).getUserVisiblePermissionDescriptors(
                    documentType);
        } catch (ClientException e) {
            throw e;
        } catch (Throwable t) {
            throw new ClientException("Unable to get PermissionProvider", t);
        }
    }

    public List<SelectItem> getSettablePermissions() throws ClientException {
        String documentType = navigationContext.getCurrentDocument().getType();

        // BBB: use the platform service if it defines permissions (deprecated)
        UIPermissionService service = (UIPermissionService) Framework.getRuntime().getComponent(
                UIPermissionService.NAME);
        String[] settablePermissions = service.getUIPermissions(documentType);

        if (settablePermissions == null || settablePermissions.length == 0) {
            // new centralized permission provider at the core level

            List<UserVisiblePermission> visiblePerms = getVisibleUserPermissions(documentType);
            settablePermissions = new String[visiblePerms.size()];
            int idx = 0;
            for (UserVisiblePermission uvp : visiblePerms) {
                settablePermissions[idx] = uvp.getId();
                idx++;
            }
        }

        List<SelectItem> permissions = new ArrayList<SelectItem>();
        for (String perm : settablePermissions) {
            String label = labeler.makeLabel(perm);
            SelectItem it = new SelectItem(perm,
                    resourcesAccessor.getMessages().get(label));
            permissions.add(it);
        }
        return permissions;
    }

    public Map<String, String> getIconAltMap() {
        return principalListManager.iconAlt;
    }

    public Map<String, String> getIconPathMap() {
        return principalListManager.iconPath;
    }

    public Boolean getBlockRightInheritance() {
        return blockRightInheritance;
    }

    public void setBlockRightInheritance(Boolean blockRightInheritance) {
        this.blockRightInheritance = blockRightInheritance;
    }

    public String blockRightInheritance() throws ClientException {
        Boolean needBlockRightInheritance = this.blockRightInheritance;
        rebuildSecurityData();

        if (needBlockRightInheritance) {
            // Block
            securityData.addModifiablePrivilege(SecurityConstants.EVERYONE,
                    SecurityConstants.EVERYTHING, false);
            // add user to avoid lock up
            Principal currentUser = FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal();
            if (securityData.getCurrentDocumentUsers() != null
                    && !securityData.getCurrentDocumentUsers().contains(
                            currentUser.getName())) {
                securityData.addModifiablePrivilege(currentUser.getName(),
                        SecurityConstants.EVERYTHING, true);
                // add administrators to avoid LockUp
                List<String> adminGroups = userManager.getAdministratorsGroups();
                for (String adminGroup : adminGroups) {
                    securityData.addModifiablePrivilege(adminGroup,
                            SecurityConstants.EVERYTHING, true);
                }
            }
        } else {
            securityData.removeModifiablePrivilege(SecurityConstants.EVERYONE,
                    SecurityConstants.EVERYTHING, false);
        }
        updateSecurityOnDocument();
        resetSecurityData();
        selectedEntries = null;
        return null;
    }

    public Boolean displayInheritedPermissions() throws ClientException {
        return getDisplayInheritedPermissions();
    }

    public boolean getDisplayInheritedPermissions() throws ClientException {
        if (blockRightInheritance == null) {
            rebuildSecurityData();
        }
        if (blockRightInheritance) {
            return false;
        }
        return !securityData.getParentDocumentsUsers().isEmpty();
    }

    public List<String> getCurrentDocumentUsers() throws ClientException {
        List<String> currentUsers = securityData.getCurrentDocumentUsers();
        return validateUserGroupList(currentUsers);
    }

    public List<String> getParentDocumentsUsers() throws ClientException {
        List<String> parentUsers = securityData.getParentDocumentsUsers();
        return validateUserGroupList(parentUsers);
    }

    /**
     * Validates user/group against userManager in order to remove obsolete
     * entries (ie: deleted groups/users).
     */
    private List<String> validateUserGroupList(List<String> usersGroups2Validate)
            throws ClientException {
        // TODO :
        // 1 -should add a clean cache system to avoid
        // calling the directory : this can be problematic for big ldaps
        // 2 - this filtering should at some point be applied to acp and saved
        // back in a batch?

        List<String> returnList = new ArrayList<String>();
        for (String entry : usersGroups2Validate) {
            if (entry.equals(SecurityConstants.EVERYONE)) {
                returnList.add(entry);
                continue;
            }
            if (isUserGroupInCache(entry)) {
                returnList.add(entry);
                continue;
            }
            if (isUserGroupInDeletedCache(entry)) {
                continue;
            }

            if (userManager.getPrincipal(entry) != null) {
                returnList.add(entry);
                addUserGroupInCache(entry);
                continue;
            } else if (userManager.getGroup(entry) != null) {
                returnList.add(entry);
                addUserGroupInCache(entry);
                continue;
            } else {
                addUserGroupInDeletedCache(entry);
            }
        }
        return returnList;
    }

    private Boolean isUserGroupInCache(String entry) {
        if (cachedValidatedUserAndGroups == null) {
            return false;
        }
        return cachedValidatedUserAndGroups.contains(entry);
    }

    private void addUserGroupInCache(String entry) {
        if (cachedValidatedUserAndGroups == null) {
            cachedValidatedUserAndGroups = new ArrayList<String>();
        }
        cachedValidatedUserAndGroups.add(entry);
    }

    private Boolean isUserGroupInDeletedCache(String entry) {
        if (cachedDeletedUserAndGroups == null) {
            return false;
        }
        return cachedDeletedUserAndGroups.contains(entry);
    }

    private void addUserGroupInDeletedCache(String entry) {
        if (cachedDeletedUserAndGroups == null) {
            cachedDeletedUserAndGroups = new ArrayList<String>();
        }

        cachedDeletedUserAndGroups.add(entry);
    }

    /**
     * Checks if the current user can still read and write access rights. If he
     * can't, then the security data are rebuilt.
     */
    private boolean checkPermissions() throws ClientException {
        List<String> principals = new ArrayList<String>();
        principals.add(currentUser.getName());
        principals.addAll(currentUser.getAllGroups());

        ACP acp = currentDocument.getACP();
        List<UserEntry> modifiableEntries = new SecurityDataConverter().convertToUserEntries(securityData);
        if (null == acp) {
            acp = new ACPImpl();
        }
        acp.setRules(modifiableEntries.toArray(new UserEntry[0]));

        final boolean access = acp.getAccess(principals.toArray(new String[0]),
                getPermissionsToCheck()).toBoolean();
        if (!access) {
            rebuildSecurityData();
        }
        return access;
    }

    protected String[] getPermissionsToCheck() throws ClientException {
        if (CACHED_PERMISSION_TO_CHECK == null) {
            try {
                PermissionProvider pprovider = Framework.getService(PermissionProvider.class);
                List<String> aggregatedPerms = new LinkedList<String>();
                for (String seedPerm : SEED_PERMISSIONS_TO_CHECK) {
                    aggregatedPerms.add(seedPerm);
                    String[] compoundPerms = pprovider.getPermissionGroups(seedPerm);
                    if (compoundPerms != null) {
                        aggregatedPerms.addAll(Arrays.asList(compoundPerms));
                    }
                }
                CACHED_PERMISSION_TO_CHECK = aggregatedPerms.toArray(new String[aggregatedPerms.size()]);
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }
        return CACHED_PERMISSION_TO_CHECK;
    }

    public String getSelectedEntry() {
        return selectedEntry;
    }

    public void setSelectedEntry(String selectedEntry) {
        this.selectedEntry = selectedEntry;
    }

    public List<String> getSelectedEntries() {
        return selectedEntries;
    }

    public void setSelectedEntries(List<String> selectedEntries) {
        this.selectedEntries = selectedEntries;
    }
}
